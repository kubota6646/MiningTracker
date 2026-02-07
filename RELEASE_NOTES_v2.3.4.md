# MiningTracker v2.3.4 リリースノート - サーバー間リアルタイム同期修正

**リリース日**: 2026-02-07  
**バージョン**: 2.3.4  
**対応Minecraft**: 1.21.x  
**必須Java**: 21+

## 🐛 修正した重大な問題

### サーバー間でリアルタイムに採掘量が同期されない

#### 報告された症状

```
1. メインサーバーで5ブロック採掘
2. 資源サーバーで /mtr コマンドを実行
3. 期待: 5ブロックと表示されるはず
4. 実際: 「採掘データがありません」と表示される
```

**影響範囲**: Bungeecordネットワーク環境で複数サーバーを運用している場合

#### 根本原因

1. **HikariCPの最適化設定**
   - `elideSetAutoCommits` が有効化されていた
   - この設定はautoCommit状態の変更を最適化するが、リアルタイム同期に悪影響

2. **暗黙的なautoCommit動作**
   - autoCommitが明示的に設定されていなかった
   - デフォルトで有効だが、確実性に欠けていた

3. **コミットタイミングの不確実性**
   - データ書き込み後のコミットが確実でなかった
   - 他のサーバーからデータが即座に見えない状態が発生

## ✅ 実施した修正

### 1. HikariCP設定の改善

**DatabaseManager.java (Bukkit版)**

```java
// 削除: リアルタイム同期のため最適化を無効化
// hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");

// 追加: autoCommitを明示的に有効化
hikariConfig.setAutoCommit(true);
```

### 2. データ書き込み時のautoCommit確認

**MySQL書き込み処理:**

```java
try (Connection conn = getMySQLConnection();
     PreparedStatement pstmt = conn.prepareStatement(sql)) {
    // autoCommitを明示的に有効化（リアルタイム同期のため）
    if (!conn.getAutoCommit()) {
        conn.setAutoCommit(true);
    }
    pstmt.setString(1, playerUUID.toString());
    pstmt.setString(2, playerName);
    pstmt.setString(3, material.name());
    pstmt.setString(4, serverName);
    pstmt.setString(5, playerName);
    pstmt.executeUpdate();
    // autoCommit=trueなので、ここで自動的にコミットされている
} catch (SQLException e) {
    plugin.getLogger().warning("採掘データの保存エラー: " + e.getMessage());
}
```

**SQLite書き込み処理:**

```java
Connection conn = getSQLiteConnection();
// SQLiteもautoCommitを確実に有効化
if (!conn.getAutoCommit()) {
    conn.setAutoCommit(true);
}
try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
    // ... データ書き込み
    pstmt.executeUpdate();
    // autoCommit=trueなので、ここで自動的にコミットされている
}
```

### 3. CommonDatabaseManager (Bungee版) の改善

```java
// 追加: autoCommitを明示的に有効化
hikariConfig.setAutoCommit(true);
```

## 📊 修正の効果

### Before (v2.3.3以前)

```
[サーバーA] プレイヤーがブロック採掘
     ↓
[データベース] データ書き込み... (コミット不確実)
     ↓
[サーバーB] /mtr コマンド実行
     ↓
[結果] 採掘データなし ❌
```

### After (v2.3.4)

```
[サーバーA] プレイヤーがブロック採掘
     ↓
[データベース] データ書き込み + 即座にコミット ✅
     ↓
[サーバーB] /mtr コマンド実行
     ↓
[結果] 採掘データ表示 ✅
```

## 🔧 技術詳細

### autoCommitとトランザクション

JDBCのautoCommitモード:
- **autoCommit = true**: 各SQL文が実行後、自動的にコミットされる
- **autoCommit = false**: 明示的に`commit()`を呼ぶ必要がある

### elideSetAutoCommitsの影響

HikariCPの`elideSetAutoCommits`最適化:
- 冗長な`setAutoCommit()`呼び出しをスキップ
- パフォーマンス向上に貢献
- しかし、リアルタイム同期が必要な場合は問題になる可能性

**本プラグインの選択**: リアルタイム同期を優先し、この最適化を無効化

### MySQL接続プール

HikariCPの設定:
```java
hikariConfig.setAutoCommit(true);  // プールのデフォルト値
```

取得した接続でも確認:
```java
if (!conn.getAutoCommit()) {
    conn.setAutoCommit(true);  // 念のため確認して設定
}
```

## 📦 ビルド成果物

```
miningtracker-bukkit/build/libs/
└── MiningTracker-Bukkit-2.3.4.jar   # Bukkit/Paper用 ⭐ 更新

miningtracker-bungee/build/libs/
└── MiningTracker-Bungee-2.3.4.jar   # Bungeecord用 ⭐ 更新
```

## 🔄 v2.3.3からの移行

**必要な作業**: JARファイルの置き換えのみ

```bash
# 古いバージョンを削除
rm MiningTracker-Bukkit-2.3.3.jar
rm MiningTracker-Bungee-2.3.3.jar

# 新バージョンをインストール
cp MiningTracker-Bukkit-2.3.4.jar server1/plugins/
cp MiningTracker-Bukkit-2.3.4.jar server2/plugins/
cp MiningTracker-Bukkit-2.3.4.jar server3/plugins/

# Bungeecord版を使用している場合
cp MiningTracker-Bungee-2.3.4.jar bungeecord/plugins/
```

**設定変更**: 不要

## ✅ 動作確認方法

### テスト手順

1. **サーバーAで採掘**
   ```
   [サーバーA] プレイヤーがブロックを5個採掘
   ```

2. **サーバーBで確認**
   ```
   [サーバーB] /mtr コマンドを実行
   ```

3. **期待される結果**
   ```
   ✅ 5ブロックの採掘データが即座に表示される
   ```

### トラブルシューティング

**それでもデータが表示されない場合:**

1. **MySQLを使用しているか確認**
   - Bungeecordネットワークでは**MySQL必須**
   - SQLiteはサーバー間で共有できない

2. **全サーバーで同じデータベースを使用しているか確認**
   ```yaml
   # 各サーバーのconfig.ymlで同じ設定
   database:
     type: mysql
     mysql:
       host: "mysql.example.com"
       database: "minecraft_network"  # 全サーバーで同じ
   ```

3. **ログでエラーを確認**
   ```bash
   tail -f logs/latest.log | grep MiningTracker
   ```

## 🎯 v2.3.3の全機能を継承

- ✅ Bungeecordプラグイン対応
- ✅ マルチモジュール構造
- ✅ ネットワーク統計表示
- ✅ Plan Player Analytics連携
- ✅ HikariCP依存関係修正済み
- ✅ Bungeecord API 1.21-R0.4使用
- ✅ shadowJar依存関係リロケーション
- ✅ 詳細なトラブルシューティングガイド

## 🙏 謝辞

リアルタイム同期の問題を報告いただき、ありがとうございました。この修正により、Bungeecordネットワーク環境でより快適にプラグインを使用できるようになりました。

## 🔗 参考リンク

- **v2.3.3リリースノート**: [RELEASE_NOTES_v2.3.3.md](RELEASE_NOTES_v2.3.3.md)
- **Bungeecordセットアップ**: [BUNGEECORD_SETUP.md](BUNGEECORD_SETUP.md)
- **トラブルシューティング**: [README.md#トラブルシューティング](README.md#トラブルシューティング)
- **変更履歴**: [CHANGELOG.md](CHANGELOG.md)
- **バージョン情報**: [VERSION.md](VERSION.md)

## 💬 サポート

問題が発生した場合は、以下を確認してください：

1. **MySQL使用**: SQLiteではサーバー間共有不可
2. **同じデータベース**: 全サーバーで同じMySQL接続情報
3. **v2.3.4使用**: 全サーバーで最新版を使用

それでも解決しない場合は、GitHubのIssuesページで報告してください。

---

**MiningTracker v2.3.4** - Real-time Sync Fix Release  
© 2026 kubota6646

リアルタイム同期で快適なマイニングライフを！
