# SQLite 接続問題の修正

## 問題の説明

### エラー
```
[04:54:26] [Craft Scheduler Thread - 1 - MiningTracker/WARN]: [MiningTracker] 採掘データの保存エラー: データベース接続が利用できません
```

### 原因
SQLiteデータベースを使用する際、各データベース操作で`try-with-resources`を使用してConnectionを取得・使用していました。

```java
// 問題のあったコード
try (Connection conn = getConnection();
     PreparedStatement pstmt = conn.prepareStatement(sql)) {
    // 処理
} // ← ここでConnectionが自動的にclose()される
```

SQLiteは単一の永続的な接続を使用するデータベースです。try-with-resourcesでConnectionを閉じてしまうと、次の操作時に接続が利用できなくなります。

一方、MySQLはHikariCP接続プールを使用しているため、close()時に接続がプールに返却されるだけで、この問題は発生しませんでした。

## 修正内容

### 1. 接続取得メソッドの分離

**MySQLの場合:**
```java
private Connection getMySQLConnection() throws SQLException {
    // 接続プールから新しい接続を取得
    // close時にプールに返却される
    return hikariDataSource.getConnection();
}
```

**SQLiteの場合:**
```java
private Connection getSQLiteConnection() throws SQLException {
    if (connection == null || connection.isClosed()) {
        // 必要に応じて再接続
        connectSQLite();
    }
    // 永続的な接続を返す（closeしてはいけない）
    return connection;
}
```

### 2. データベース操作の更新

**MySQL用（変更なし）:**
```java
try (Connection conn = getMySQLConnection();
     PreparedStatement pstmt = conn.prepareStatement(sql)) {
    // 処理
} // Connectionはプールに返却される
```

**SQLite用（修正後）:**
```java
try {
    Connection conn = getSQLiteConnection();
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        // 処理
    } // PreparedStatementのみclose
} // Connectionはcloseしない
```

### 3. 更新されたメソッド

以下のすべてのデータベース操作メソッドが更新されました：

1. `addMiningCount()` - 採掘データの保存
2. `getPlayerStats()` - プレイヤー統計取得
3. `getTotalMined()` - 総採掘数取得
4. `getTopPlayers()` - ランキング取得
5. `getTotalPlayers()` - プレイヤー数取得
6. `resetPlayerStats()` - プレイヤー統計リセット
7. `resetAllStats()` - 全統計リセット

## 動作の違い

### 修正前
```
1. プレイヤーがブロックを破壊
2. addMiningCount() 呼び出し
3. getConnection() でSQLite接続取得
4. データ保存
5. try-with-resources で接続がclose
6. 次のブロック破壊
7. getConnection() 呼び出し
8. 接続がclosedのため SQLException発生 ❌
```

### 修正後
```
1. プレイヤーがブロックを破壊
2. addMiningCount() 呼び出し
3. getSQLiteConnection() で永続的な接続取得
4. データ保存
5. PreparedStatementのみclose、接続は維持
6. 次のブロック破壊
7. getSQLiteConnection() 呼び出し
8. 既存の接続を再利用 ✅
```

## テスト方法

### 1. 基本的な動作確認
```
1. プラグインを読み込む
2. SQLiteモードを確認（config.yml: database.type: sqlite）
3. ブロックを複数回破壊
4. /mtstats コマンドで統計を確認
5. エラーメッセージが出ないことを確認
```

### 2. 再接続のテスト
```
1. サーバーを起動
2. ブロックを破壊してデータ保存
3. サーバーを再起動
4. ブロックを再度破壊
5. データが正しく保存されることを確認
```

### 3. ランキング機能のテスト
```
1. 複数のプレイヤーでブロックを破壊
2. /mtranking コマンドを実行
3. ランキングが正しく表示されることを確認
```

## MySQL互換性

MySQLモードでは以前と同じように動作します：
- HikariCP接続プールを使用
- try-with-resourcesで接続をプールに返却
- 複数の同時接続をサポート

## まとめ

この修正により、SQLiteデータベースモードで以下が改善されました：

✅ 接続が正しく維持される
✅ "データベース接続が利用できません"エラーが発生しない
✅ すべてのデータベース操作が正常に動作
✅ MySQL互換性は維持される
✅ パフォーマンスへの影響なし

SQLiteの単一接続モデルとMySQLの接続プールモデルの違いを正しく処理できるようになりました。
