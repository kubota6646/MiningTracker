# Bungeecord ネットワーク構成ガイド

## 概要

MiningTracker v2.2.0以降、Bungeecordネットワーク環境で動作し、Plan Player Analyticsのネットワークページに採掘統計を表示できます。

## Bungeecordネットワークでの動作

### アーキテクチャ

```
┌─────────────────────────────────────────────────────────┐
│           Bungeecord Proxy                               │
│  - Plan Player Analytics (Bungee版)                     │
│  - ネットワークページを提供                               │
└──────────┬──────────────────────────────────────────────┘
           │
    ┌──────┴───────┬──────────────┬──────────────┐
    │              │              │              │
┌───▼────┐   ┌────▼───┐    ┌────▼───┐    ┌────▼───┐
│ Lobby  │   │Survival│    │Creative│    │MiniGame│
│        │   │        │    │        │    │        │
│ Plan   │   │ Plan   │    │ Plan   │    │ Plan   │
│ MT     │   │ MT     │    │ MT     │    │ MT     │
└────┬───┘   └────┬───┘    └────┬───┘    └────┬───┘
     │            │             │             │
     └────────────┴─────────────┴─────────────┘
                  │
           ┌──────▼───────┐
           │ MySQL Server │
           │ (共有DB)      │
           └──────────────┘

MT = MiningTracker
```

### データフロー

1. **各バックエンドサーバー**でプレイヤーがブロックを採掘
2. **MiningTracker**が採掘データを共有MySQLデータベースに記録
3. **Plan DataExtension**がMySQLから統計を取得
4. **Planネットワークページ**が全サーバーのデータを集約して表示

### 重要なポイント

- **MiningTrackerはBukkitプラグイン**：Bungeecordプロキシには不要
- **各バックエンドサーバーにインストール**：全てのSpigot/Paperサーバーに必要
- **共有MySQL必須**：全サーバーが同じMySQLデータベースを使用
- **Plan連携自動**：ネットワーク統計は自動的にネットワークページに表示

## セットアップ手順

### 前提条件

- Bungeecord/Waterfall/Velocityプロキシ
- 複数のSpigot/Paper 1.21.xバックエンドサーバー
- MySQL 5.7+ または 8.0+
- Plan Player Analytics 5.6+（Bungeecord版とBukkit版両方）
- Java 21+

### 1. MySQLデータベースの準備

まず、ネットワーク全体で共有するMySQLデータベースを作成します。

```sql
-- MySQLにログイン
mysql -u root -p

-- データベース作成
CREATE DATABASE minecraft_network CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ユーザー作成と権限付与
CREATE USER 'minecraft'@'%' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON minecraft_network.* TO 'minecraft'@'%';
FLUSH PRIVILEGES;

EXIT;
```

### 2. Plan Player Analyticsのインストール

#### Bungeecordプロキシ

```bash
# Bungeecord Planプラグインをダウンロード
# https://www.spigotmc.org/resources/plan-player-analytics.32536/

# Bungeecordのpluginsフォルダに配置
cp Plan-5.6-build-XXXX.jar /path/to/bungeecord/plugins/
```

#### 各バックエンドサーバー

```bash
# Bukkit/Spigot Planプラグインをダウンロード
# 同じJARファイルがBukkit/Bungeecord両方で動作

# 各サーバーのpluginsフォルダに配置
cp Plan-5.6-build-XXXX.jar /path/to/server1/plugins/
cp Plan-5.6-build-XXXX.jar /path/to/server2/plugins/
cp Plan-5.6-build-XXXX.jar /path/to/server3/plugins/
```

#### Planの設定

Planを初回起動後、各サーバーの`Plan/config.yml`を編集：

**Bungeecordプロキシ** (`plugins/Plan/config.yml`):
```yaml
Network:
  # ネットワーク名
  Name: "My Minecraft Network"

Database:
  Type: MySQL
  MySQL:
    Host: mysql.example.com
    Port: 3306
    Database: minecraft_network
    Username: minecraft
    Password: secure_password

Webserver:
  # メインWebサーバー（プロキシで提供）
  Port: 8804
  Internal_IP: 0.0.0.0
```

**各バックエンドサーバー** (`plugins/Plan/config.yml`):
```yaml
Server:
  # サーバー固有の名前
  ServerName: "Survival"  # または "Creative", "Lobby" など

Network:
  Name: "My Minecraft Network"

Database:
  Type: MySQL
  MySQL:
    Host: mysql.example.com
    Port: 3306
    Database: minecraft_network
    Username: minecraft
    Password: secure_password

Webserver:
  # バックエンドサーバーはWebサーバーを無効化（プロキシにリダイレクト）
  DisableWebServer: true
  Alternative_IP:
    Enabled: true
    Address: "https://plan.example.com:8804"  # プロキシのPlan URL
```

### 3. MiningTrackerのインストール

#### 各バックエンドサーバーのみ

```bash
# MiningTrackerをダウンロード
# https://github.com/kubota6646/MiningTracker/releases

# 各バックエンドサーバーのpluginsフォルダに配置
cp MiningTracker-2.2.0.jar /path/to/server1/plugins/
cp MiningTracker-2.2.0.jar /path/to/server2/plugins/
cp MiningTracker-2.2.0.jar /path/to/server3/plugins/
```

**重要**: MiningTrackerはBungeecordプロキシには不要です。

#### MiningTrackerの設定

各バックエンドサーバーの`plugins/MiningTracker/config.yml`を編集：

**サーバー1 (Survival)** の `config.yml`:
```yaml
# サーバー固有の名前（各サーバーで異なる名前を使用）
server-name: "survival"

database:
  type: mysql
  mysql:
    # 全サーバーで同じMySQLデータベースを使用
    host: "mysql.example.com"
    port: 3306
    database: "minecraft_network"
    username: "minecraft"
    password: "secure_password"
    pool:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000

tracking:
  enabled: true
  count-creative: false
  count-silk-touch: true
```

**サーバー2 (Creative)** の `config.yml`:
```yaml
# 異なるサーバー名
server-name: "creative"

database:
  type: mysql
  mysql:
    # 同じMySQLデータベース
    host: "mysql.example.com"
    port: 3306
    database: "minecraft_network"
    username: "minecraft"
    password: "secure_password"
    pool:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000

tracking:
  enabled: true
  count-creative: true  # クリエイティブでもカウント
  count-silk-touch: true
```

**サーバー3 (Lobby)** の `config.yml`:
```yaml
# ロビーサーバー
server-name: "lobby"

database:
  type: mysql
  mysql:
    # 同じMySQLデータベース
    host: "mysql.example.com"
    port: 3306
    database: "minecraft_network"
    username: "minecraft"
    password: "secure_password"
    pool:
      maximum-pool-size: 5  # ロビーは少なめでOK
      minimum-idle: 1
      connection-timeout: 30000

tracking:
  enabled: false  # ロビーでは採掘トラッキングを無効化
```

### 4. サーバーの起動

1. **MySQLサーバー**を起動
2. **Bungeecordプロキシ**を起動
3. **各バックエンドサーバー**を起動

起動ログで以下を確認：

**Planのログ（各サーバー）**:
```
[Plan] Registering network with network name: My Minecraft Network
[Plan] Database connection established
```

**MiningTrackerのログ（各バックエンドサーバー）**:
```
[MiningTracker] MiningTracker が有効化されました。
[MiningTracker] Plan拡張機能を正常に登録しました
```

### 5. 動作確認

1. **Planダッシュボードにアクセス**:
   ```
   http://your-proxy-ip:8804
   ```

2. **ネットワークページを開く**:
   - トップメニューから「Network」をクリック
   - または `http://your-proxy-ip:8804/network`

3. **MiningTracker統計を確認**:
   - サイドバーの「MiningTracker」セクション
   - 「ネットワーク統計」タブ
   - 以下が表示されるはず：
     - ネットワーク総採掘数
     - 総アクティブマイナー数
     - ネットワークトップマイナーランキング
     - サーバー比較テーブル

4. **サーバー別ページも確認**:
   - 各サーバーページ（例: `/server/Survival`）
   - 「サーバー統計」タブ
   - サーバー固有の統計が表示される

5. **プレイヤー別ページも確認**:
   - プレイヤーページ（例: `/player/PlayerName`）
   - 「採掘統計」タブ
   - 全サーバー合計とサーバー別の統計が表示される

## トラブルシューティング

### ネットワークページに統計が表示されない

**原因1: MySQLデータベースが共有されていない**

確認方法：
```sql
-- 各サーバーのMySQLに接続して確認
mysql -h mysql.example.com -u minecraft -p minecraft_network

-- MiningTrackerのテーブルを確認
SHOW TABLES LIKE 'mt_%';

-- サーバー名ごとのデータを確認
SELECT server_name, COUNT(*) FROM mt_mining_data GROUP BY server_name;
```

解決方法：
- 全サーバーの`config.yml`で同じMySQL接続情報を使用
- `server-name`は各サーバーで異なる名前を設定

**原因2: Planが正しくネットワークモードで動作していない**

確認方法：
```
# Bungeecordコンソール
plan info

# バックエンドサーバーコンソール
plan info
```

解決方法：
- Planの`config.yml`で`Network.Name`を全サーバーで統一
- Bungeecordと全バックエンドサーバーが同じMySQLデータベースを使用

**原因3: MiningTrackerがBungeecordにインストールされている**

解決方法：
- MiningTrackerはBungeecordプロキシからアンインストール
- 各バックエンドサーバーのみにインストール

### データが重複して表示される

**原因: 同じ`server-name`を複数のサーバーで使用**

解決方法：
- 各サーバーの`config.yml`で異なる`server-name`を設定
- 例: "survival", "creative", "lobby", "minigame"など

### ネットワーク統計が0になる

**原因: データベースに接続できていない**

確認方法：
```bash
# サーバーログを確認
tail -f /path/to/server/logs/latest.log | grep -i "mining\|mysql\|database"
```

解決方法：
1. MySQL接続情報が正しいか確認
2. MySQLサーバーが起動しているか確認
3. ファイアウォールでポート3306が開いているか確認
4. MySQL用ユーザーに適切な権限があるか確認

## パフォーマンス最適化

### MySQL接続プール設定

バックエンドサーバー数に応じて接続プール設定を調整：

**小規模ネットワーク（2-3サーバー）**:
```yaml
database:
  mysql:
    pool:
      maximum-pool-size: 5
      minimum-idle: 2
```

**中規模ネットワーク（4-10サーバー）**:
```yaml
database:
  mysql:
    pool:
      maximum-pool-size: 10
      minimum-idle: 3
```

**大規模ネットワーク（10+サーバー）**:
```yaml
database:
  mysql:
    pool:
      maximum-pool-size: 15
      minimum-idle: 5
```

### MySQLサーバー最適化

```sql
-- MySQLの設定を最適化（my.cnf）
[mysqld]
max_connections = 200
innodb_buffer_pool_size = 2G
innodb_flush_log_at_trx_commit = 2
query_cache_size = 64M
```

## よくある質問

### Q: Bungeecordにもプラグインをインストールする必要がありますか？

A: **いいえ**。MiningTrackerはBukkitプラグインで、バックエンドサーバーのみにインストールします。Bungeecordプロキシには不要です。

### Q: Velocityでも動作しますか？

A: はい。PlanはVelocityにも対応しているため、同じ設定でVelocityネットワークでも動作します。

### Q: 一部のサーバーでのみ採掘統計を有効にできますか？

A: はい。各サーバーの`config.yml`で`tracking.enabled: false`に設定すれば、そのサーバーでは採掘トラッキングが無効になります。

### Q: サーバー名は後から変更できますか？

A: 技術的には可能ですが、推奨されません。サーバー名を変更すると、新しい名前で記録が開始され、履歴が分断されます。変更する場合は、データベースで手動マイグレーションが必要です。

### Q: プレイヤーがサーバー間を移動した場合、統計はどうなりますか？

A: プレイヤーはUUIDで識別されるため、どのサーバーで採掘しても同じプレイヤーとして統計が集計されます。Planのプレイヤーページでは「全サーバー合計」と「サーバー別」の両方が表示されます。

## 参考リンク

- **Plan Bungeecord Setup**: https://github.com/plan-player-analytics/Plan/wiki/Bungee-Set-Up
- **Plan Bungeecord Configuration**: https://github.com/plan-player-analytics/Plan/wiki/Bungee-Configuration
- **MiningTracker Plan連携ガイド**: [PLAN_INTEGRATION.md](PLAN_INTEGRATION.md)

## サポート

問題が発生した場合は、以下の情報を含めてGitHub Issuesで報告してください：

1. Bungeecordのバージョン
2. 各バックエンドサーバーのバージョン
3. Planのバージョン
4. MiningTrackerのバージョン
5. MySQLのバージョン
6. 各サーバーの`server-name`設定
7. サーバーログ（エラーメッセージを含む）
8. `config.yml`の内容（パスワードは除く）

---

**MiningTracker v2.2.0** - Bungeecord Network Support  
© 2026 kubota6646
