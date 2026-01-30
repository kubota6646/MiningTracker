# MySQL セットアップガイド

このドキュメントでは、MiningTrackerプラグインでMySQLデータベースを使用する方法を説明します。

## 前提条件

- MySQL 5.7以降またはMySQL 8.0以降がインストールされていること
- MySQLサーバーが稼働していること
- MySQLの管理者権限があること

## ステップ1: データベースの作成

MySQLにログインして、データベースを作成します：

```sql
-- MySQLにログイン
mysql -u root -p

-- データベースを作成
CREATE DATABASE minecraft CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 専用ユーザーを作成（推奨）
CREATE USER 'minecraft_user'@'localhost' IDENTIFIED BY 'secure_password';

-- ユーザーに権限を付与
GRANT ALL PRIVILEGES ON minecraft.* TO 'minecraft_user'@'localhost';

-- 権限を反映
FLUSH PRIVILEGES;

-- 確認
SHOW DATABASES;
```

## ステップ2: プラグイン設定

`plugins/MiningTracker/config.yml`を編集します：

```yaml
# データベース設定
database:
  # データベースタイプをmysqlに変更
  type: mysql
  
  # MySQL設定
  mysql:
    # MySQLサーバーのホスト（ローカルの場合はlocalhost）
    host: localhost
    # MySQLサーバーのポート（デフォルトは3306）
    port: 3306
    # 作成したデータベース名
    database: minecraft
    # MySQLユーザー名
    username: minecraft_user
    # MySQLパスワード
    password: secure_password
    # 接続プール設定
    pool:
      # 最大接続数（サーバー規模に応じて調整）
      maximum-pool-size: 10
      # 最小アイドル接続数
      minimum-idle: 2
      # 接続タイムアウト（ミリ秒）
      connection-timeout: 30000
```

## ステップ3: サーバーの再起動

設定を変更したら、Minecraftサーバーを再起動します。

```bash
# サーバーを停止
/stop

# サーバーを起動
java -jar server.jar
```

## ステップ4: 動作確認

サーバーログで以下のようなメッセージを確認します：

```
[MiningTracker] MySQLデータベースに接続しました（HikariCP使用）。
[MiningTracker] 接続プール設定: 最大=10, 最小=2
[MiningTracker] MiningTracker が有効化されました。
```

## リモートMySQLサーバーを使用する場合

### MySQLサーバー側の設定

1. MySQLの設定ファイル（`/etc/mysql/mysql.conf.d/mysqld.cnf`）を編集：

```ini
# デフォルトの bind-address をコメントアウトまたは変更
# bind-address = 127.0.0.1
bind-address = 0.0.0.0
```

2. リモートアクセス用のユーザーを作成：

```sql
-- リモートホストからのアクセスを許可
CREATE USER 'minecraft_user'@'%' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON minecraft.* TO 'minecraft_user'@'%';
FLUSH PRIVILEGES;
```

3. ファイアウォールでポート3306を開放：

```bash
# UFWの場合
sudo ufw allow 3306/tcp

# firewalldの場合
sudo firewall-cmd --permanent --add-port=3306/tcp
sudo firewall-cmd --reload
```

4. MySQLサーバーを再起動：

```bash
sudo systemctl restart mysql
```

### プラグイン側の設定

```yaml
database:
  type: mysql
  mysql:
    # リモートサーバーのIPアドレスまたはホスト名
    host: 192.168.1.100  # または your-mysql-server.com
    port: 3306
    database: minecraft
    username: minecraft_user
    password: secure_password
    pool:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
```

## 接続プールサイズの推奨値

サーバーの規模に応じて、接続プールのサイズを調整してください：

| サーバー規模 | プレイヤー数 | maximum-pool-size | minimum-idle |
|------------|------------|------------------|--------------|
| 小規模      | 〜50人      | 5-10             | 2            |
| 中規模      | 50-200人    | 10-20            | 3-5          |
| 大規模      | 200人以上   | 20-30            | 5-10         |

## パフォーマンス最適化

### MySQL サーバー側の設定

`/etc/mysql/mysql.conf.d/mysqld.cnf`に以下を追加：

```ini
[mysqld]
# 接続数の上限を増やす
max_connections = 200

# クエリキャッシュの設定
query_cache_type = 1
query_cache_size = 128M

# InnoDB設定
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT
```

### プラグイン側での最適化

HikariCPは自動的に以下の最適化を適用します：

- **PreparedStatementキャッシュ**: クエリの再利用で性能向上
- **バッチ書き込み**: 複数のデータ更新を効率的に処理
- **コネクション再利用**: 接続の確立コストを削減
- **自動再接続**: 接続が切れた場合の自動復旧

## トラブルシューティング

### 接続エラー

```
Could not connect to MySQL server
```

**解決方法:**
1. MySQLサーバーが起動しているか確認
2. ホスト名とポートが正しいか確認
3. ファイアウォールの設定を確認
4. MySQLのエラーログを確認（`/var/log/mysql/error.log`）

### 認証エラー

```
Access denied for user 'minecraft_user'@'localhost'
```

**解決方法:**
1. ユーザー名とパスワードが正しいか確認
2. ユーザーに適切な権限があるか確認：
   ```sql
   SHOW GRANTS FOR 'minecraft_user'@'localhost';
   ```

### 接続タイムアウト

```
Connection timeout
```

**解決方法:**
1. `connection-timeout`の値を増やす（60000など）
2. ネットワーク接続を確認
3. MySQLサーバーの負荷を確認

## データの移行

### SQLiteからMySQLへの移行

1. 既存のSQLiteデータベースをバックアップ：
   ```bash
   cp plugins/MiningTracker/mining_data.db plugins/MiningTracker/mining_data.db.backup
   ```

2. データをエクスポート：
   ```bash
   sqlite3 plugins/MiningTracker/mining_data.db .dump > mining_data.sql
   ```

3. SQLをMySQL互換に変換（手動編集が必要な場合があります）

4. MySQLにインポート：
   ```bash
   mysql -u minecraft_user -p minecraft < mining_data.sql
   ```

5. config.ymlでMySQLに切り替え

6. サーバーを再起動

## セキュリティのベストプラクティス

1. **強力なパスワードを使用**: 推測されにくいパスワードを設定
2. **専用ユーザーを作成**: rootユーザーは使用しない
3. **最小権限の原則**: 必要な権限のみを付与
4. **リモート接続の制限**: 可能な限りlocalhostから接続
5. **SSL接続の使用**: 本番環境ではSSL接続を検討（config.ymlで`useSSL: true`に変更）
6. **定期的なバックアップ**: データベースの定期バックアップを実施

## サポート

問題が発生した場合は、以下の情報と共にGitHubのIssuesで報告してください：

- Minecraftバージョン
- プラグインバージョン
- MySQLバージョン
- サーバーログ（エラーメッセージ）
- config.ymlの設定（パスワードは除く）
