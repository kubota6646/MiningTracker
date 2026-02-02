# HikariCP接続リーク修正

## 問題の概要

### エラーメッセージ
```
[01:04:57 WARN]: [com.zaxxer.hikari.pool.ProxyLeakTask] Connection leak detection triggered 
for com.mysql.cj.jdbc.ConnectionImpl@73681f55 on thread Server thread, stack trace follows
java.lang.Exception: Apparent connection leak detected
at com.zaxxer.hikari.HikariDataSource.getConnection(HikariDataSource.java:99)
at com.kubota6646.miningtracker.database.DatabaseManager.connectMySQL(DatabaseManager.java:101)
```

### 問題の症状
1. プラグイン起動時から60秒後に警告が表示
2. 長時間稼働すると接続プール内の利用可能な接続が減少
3. パフォーマンスが低下する可能性

## 根本原因

### 問題のあったコード（v2.0.2）

```java
private boolean connectMySQL() throws SQLException {
    // ... 設定 ...
    
    try {
        hikariDataSource = new HikariDataSource(hikariConfig);
        connection = hikariDataSource.getConnection(); // ❌ 問題！
        createTables();
        
        plugin.getLogger().info("MySQLデータベースに接続しました（HikariCP使用）。");
        return true;
    } catch (SQLException e) {
        // ...
    }
}
```

### 何が問題だったか

1. **接続の不適切な保存**
   - `connection = hikariDataSource.getConnection()` で接続プールから接続を取得
   - この接続をインスタンス変数`connection`に保存
   - プラグインが有効な間、この接続が保持される

2. **接続が返却されない**
   - HikariCPの接続プールでは、接続を取得したら使用後すぐに返却する必要がある
   - インスタンス変数に保存すると、プールに返却されない
   - プール内の利用可能な接続数が減少

3. **リーク検出のトリガー**
   - HikariCPは`leakDetectionThreshold`（デフォルト60秒）を設定
   - 接続が60秒以上返却されないと警告を発する
   - これは正常な動作（問題を検出している）

## 修正内容

### 修正後のコード（v2.0.3）

```java
private boolean connectMySQL() throws SQLException {
    // ... 設定 ...
    
    try {
        hikariDataSource = new HikariDataSource(hikariConfig);
        
        // テーブルを作成（接続を一時的に取得し、すぐに返却）
        try (Connection conn = hikariDataSource.getConnection()) {
            createTablesForMySQL(conn);
        } // ← ここで自動的に接続がプールに返却される
        
        plugin.getLogger().info("MySQLデータベースに接続しました（HikariCP使用）。");
        return true;
    } catch (SQLException e) {
        // ...
    }
}

/**
 * MySQL用のテーブル作成（指定された接続を使用）
 */
private void createTablesForMySQL(Connection conn) throws SQLException {
    String createTableSQL = "CREATE TABLE IF NOT EXISTS mining_data (" +
            // ... SQL ...
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    
    try (Statement stmt = conn.createStatement()) {
        stmt.execute(createTableSQL);
    }
}
```

### 変更点の説明

1. **接続の一時的な取得**
   - `try (Connection conn = hikariDataSource.getConnection())`
   - try-with-resourcesを使用して自動的に接続を返却

2. **専用メソッドの追加**
   - `createTablesForMySQL(Connection conn)` を追加
   - 接続を引数として受け取る
   - インスタンス変数`connection`に依存しない

3. **明確な責任分離**
   - `connection`フィールド: SQLiteのみで使用
   - `hikariDataSource`: MySQLで接続プールとして使用

## HikariCP接続プールの正しい使用方法

### 推奨パターン

```java
// ✅ 正しい方法: try-with-resourcesで自動返却
try (Connection conn = hikariDataSource.getConnection()) {
    // データベース操作
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        // SQL実行
    }
} // 接続は自動的にプールに返却される
```

### 避けるべきパターン

```java
// ❌ 間違い1: インスタンス変数に保存
private Connection connection;

public void someMethod() {
    connection = hikariDataSource.getConnection(); // 返却されない
    // ...
}

// ❌ 間違い2: 手動でcloseを忘れる
public void someMethod() {
    Connection conn = hikariDataSource.getConnection();
    // ... 処理 ...
    // conn.close() を忘れる = リーク
}

// ❌ 間違い3: 長時間保持
private Connection connection;

public void init() {
    connection = hikariDataSource.getConnection();
    // この接続を長時間使い続ける = プールの無駄遣い
}
```

## 接続管理の設計パターン

### SQLite（永続的な単一接続）

```java
private Connection connection; // SQLite用

private boolean connectSQLite() throws SQLException {
    connection = DriverManager.getConnection(url);
    createTables();
    return true;
}

private Connection getSQLiteConnection() throws SQLException {
    if (connection == null || connection.isClosed()) {
        connectSQLite();
    }
    return connection; // 同じ接続を返す
}
```

### MySQL（接続プール）

```java
private HikariDataSource hikariDataSource; // MySQL用

private boolean connectMySQL() throws SQLException {
    hikariDataSource = new HikariDataSource(hikariConfig);
    
    // 初期化時のみ接続を取得・使用・返却
    try (Connection conn = hikariDataSource.getConnection()) {
        createTablesForMySQL(conn);
    }
    return true;
}

private Connection getMySQLConnection() throws SQLException {
    // 毎回プールから新しい接続を取得
    return hikariDataSource.getConnection();
}
```

## HikariCPの設定

### リーク検出の設定

```java
HikariConfig config = new HikariConfig();

// リーク検出のしきい値（ミリ秒）
config.setLeakDetectionThreshold(60000); // 60秒

// 0に設定すると検出を無効化（非推奨）
// config.setLeakDetectionThreshold(0);
```

### 推奨設定

```java
// 接続プールサイズ
config.setMaximumPoolSize(10);      // 最大接続数
config.setMinimumIdle(2);           // 最小アイドル接続数

// タイムアウト
config.setConnectionTimeout(30000); // 接続取得タイムアウト（30秒）

// リーク検出（本番環境では有効にする）
config.setLeakDetectionThreshold(60000); // 60秒
```

## トラブルシューティング

### 警告が出る場合

**症状:**
```
Connection leak detection triggered for ...
```

**原因:**
1. 接続を取得して返却していない
2. try-with-resourcesを使用していない
3. 例外発生時にcloseが呼ばれない

**解決方法:**
- try-with-resourcesを使用
- すべてのコードパスで接続が返却されることを確認
- finally句でcloseを確実に実行

### パフォーマンスが低下する場合

**症状:**
- データベース操作が遅い
- 接続タイムアウトエラー

**原因:**
- 接続リークによりプール内の接続が枯渇
- 新しい接続を待つ時間が発生

**解決方法:**
1. 接続リークを修正
2. プールサイズを調整
3. 接続の使用パターンを見直す

### 接続数の確認

```sql
-- MySQL側で確認
SHOW PROCESSLIST;
SHOW STATUS LIKE 'Threads_connected';

-- HikariCP側で確認（ログレベルをDEBUGに設定）
[HikariPool-1] - Pool stats (total=10, active=2, idle=8, waiting=0)
```

## ベストプラクティス

### 1. 常にtry-with-resourcesを使用

```java
try (Connection conn = hikariDataSource.getConnection();
     PreparedStatement pstmt = conn.prepareStatement(sql)) {
    // 処理
}
```

### 2. 接続を長時間保持しない

```java
// ❌ 悪い例
Connection conn = getConnection();
// 長時間の処理...
conn.close();

// ✅ 良い例
try (Connection conn = getConnection()) {
    // 必要な処理のみ
}
```

### 3. トランザクションは短く保つ

```java
try (Connection conn = getConnection()) {
    conn.setAutoCommit(false);
    try {
        // トランザクション処理（短時間）
        conn.commit();
    } catch (SQLException e) {
        conn.rollback();
        throw e;
    }
}
```

### 4. 接続プールのメトリクスを監視

```java
HikariPoolMXBean poolProxy = hikariDataSource.getHikariPoolMXBean();
int activeConnections = poolProxy.getActiveConnections();
int idleConnections = poolProxy.getIdleConnections();
int totalConnections = poolProxy.getTotalConnections();
int threadsAwaitingConnection = poolProxy.getThreadsAwaitingConnection();
```

## まとめ

### 修正前（v2.0.2）
- ❌ 接続をインスタンス変数に保存
- ❌ プールに返却されない
- ❌ リーク警告が発生

### 修正後（v2.0.3）
- ✅ try-with-resourcesで自動返却
- ✅ 接続プールの効率的な使用
- ✅ リーク警告が解消

### 重要な教訓

1. **HikariCPでは接続を保持しない**
   - 必要な時に取得、使用後すぐに返却

2. **try-with-resourcesを活用**
   - 自動的に接続が返却される
   - 例外時も安全

3. **適切な設計パターン**
   - SQLite: 永続的な単一接続
   - MySQL: 接続プール（短期的な使用）

4. **リーク検出は有効に**
   - 問題の早期発見に役立つ
   - 本番環境では必須

---

このドキュメントは、HikariCP接続プールの正しい使用方法と、
接続リークを防ぐためのベストプラクティスをまとめたものです。
