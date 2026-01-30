# MySQL対応実装の概要

## 実装内容

MiningTrackerプラグインは、既にMySQLデータベースへの完全な対応が実装されています。
さらに、HikariCP接続プールを統合することで、高性能なデータベースアクセスを実現しました。

## 主な機能

### 1. データベースタイプの選択
- **SQLite**: デフォルト、小規模サーバー向け
- **MySQL**: 大規模サーバー、マルチサーバー環境向け

### 2. HikariCP接続プール（MySQL専用）
- 高性能なJDBC接続プール
- 接続の自動管理と再利用
- 設定可能なプールサイズとタイムアウト
- 自動再接続機能

### 3. MySQL最適化
以下の最適化が自動的に適用されます：
- PreparedStatementキャッシュ（250エントリ）
- バッチ書き込みの最適化
- サーバー設定のキャッシュ
- メタデータのキャッシュ
- 不要な通信の削減

## コード変更箇所

### 1. build.gradle
新しい依存関係を追加：
```gradle
dependencies {
    compileOnly 'org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT'
    implementation 'com.mysql:mysql-connector-j:8.3.0'      // MySQLドライバー
    implementation 'com.zaxxer:HikariCP:5.1.0'             // 接続プール
    implementation 'org.slf4j:slf4j-simple:2.0.9'          // ログ
}
```

### 2. DatabaseManager.java
主な変更点：
- HikariDataSourceの追加
- connectMySQL()メソッドの書き換え（HikariCP使用）
- getConnection()メソッドの追加（接続プールから取得）
- disconnect()メソッドの更新（プールのクローズ処理）
- すべてのデータベース操作でgetConnection()を使用

### 3. config.yml
MySQL接続プール設定を含む完全な設定：
```yaml
database:
  type: mysql  # または sqlite
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: root
    password: password
    pool:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
```

## ドキュメント

### README.md
以下のセクションを追加・更新：
- MySQL機能の説明（HikariCP使用を明記）
- 接続プール設定の詳細
- サーバー規模別の推奨設定
- MySQL性能最適化の説明
- トラブルシューティング（MySQL接続エラー対応）
- 使用ライブラリの一覧
- データベース実装の詳細

### MYSQL_SETUP.md（新規作成）
完全なMySQLセットアップガイド：
- データベース作成手順
- ユーザー作成と権限設定
- プラグイン設定手順
- リモートMySQL接続の設定
- 接続プールサイズの推奨値
- パフォーマンス最適化（MySQLサーバー側）
- トラブルシューティング
- SQLiteからMySQLへのデータ移行
- セキュリティのベストプラクティス

## 技術的な詳細

### HikariCPの利点
1. **高性能**: 業界標準の接続プール、最速のJDBC接続プール
2. **信頼性**: 自動接続テストと再接続
3. **軽量**: 小さいフットプリント、少ないメモリ使用量
4. **設定可能**: 柔軟なプール設定

### 接続管理
- **SQLite**: 単一の永続的な接続を使用
- **MySQL**: HikariCPプールから接続を取得・返却
- すべてのデータベース操作でtry-with-resources構文を使用（自動クローズ）

### SQL互換性
- MySQLとSQLiteの両方に対応したSQL文
- UPSERT構文の使い分け：
  - MySQL: `ON DUPLICATE KEY UPDATE`
  - SQLite: `ON CONFLICT ... DO UPDATE`

## 動作確認

プラグインが正常に起動すると、以下のようなログが出力されます：

### SQLite使用時
```
[MiningTracker] SQLiteデータベースに接続しました。
[MiningTracker] MiningTracker が有効化されました。
```

### MySQL使用時
```
[MiningTracker] MySQLデータベースに接続しました（HikariCP使用）。
[MiningTracker] 接続プール設定: 最大=10, 最小=2
[MiningTracker] MiningTracker が有効化されました。
```

## パフォーマンス

### 小規模サーバー（〜50人）
- 接続プールサイズ: 5-10
- 十分なパフォーマンスを提供

### 中規模サーバー（50-200人）
- 接続プールサイズ: 10-20
- 高負荷時も安定した動作

### 大規模サーバー（200人以上）
- 接続プールサイズ: 20-30
- 大量の同時アクセスに対応

## セキュリティ

### MySQL Connector/J 8.3.0使用
- 最新の安全なMySQLドライバー
- 既知の脆弱性に対する保護

### 接続セキュリティ
- SSL接続のサポート（設定可能）
- パスワード暗号化
- 安全な認証方式

## 互換性

- Minecraft 1.19.4
- Java 17以降
- MySQL 5.7以降、8.0以降
- MariaDB 10.3以降（互換性あり）
- SQLite 3.x

## まとめ

MiningTrackerプラグインは、MySQLデータベースへの完全な対応と、HikariCP接続プールによる高性能なデータベースアクセスを提供しています。小規模から大規模まで、あらゆる規模のサーバーに対応できる柔軟な設計となっています。

詳細なセットアップ方法については、`MYSQL_SETUP.md`を参照してください。
