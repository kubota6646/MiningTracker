# Plan Player Analytics 連携ガイド

## 概要

MiningTracker v2.1.0から、Plan Player Analyticsプラグインとの連携機能が追加されました。
この機能により、プレイヤーの採掘統計をPlanのWebダッシュボードで視覚的に確認できます。

## 機能

### 表示されるデータ

#### 1. プレイヤー個別ページ

**Mining Statsタブ:**
- **総採掘ブロック数（全サーバー合計）**: プレイヤーが全サーバーで採掘したブロックの総数
- **総採掘ブロック数（このサーバー）**: 現在のサーバーでの採掘数
- **採掘ランキング順位**: ネットワーク全体でのランキング
- **ブロックタイプ別内訳**: 各ブロックの採掘数（サーバー別・全サーバー合計）

#### 2. サーバーページ

**Server Statsタブ:**
- **サーバー総採掘数**: そのサーバーでの全プレイヤーの採掘総数
- **アクティブマイナー数**: そのサーバーで採掘したプレイヤーの総数
- **トップマイナーランキング**: サーバー内のトップ10ランキング

#### 3. ネットワークページ

**Network Statsタブ:**
- **ネットワーク総採掘数**: 全サーバー合計の総採掘数
- **全マイナー数**: 全サーバーで採掘したプレイヤーの総数
- **ネットワークトップランキング**: 全サーバー合計のトップ10
- **サーバー比較テーブル**: 各サーバーの採掘数とプレイヤー数の比較

## セットアップ

### 必要なもの

- Minecraft サーバー (Spigot/Paper 1.21以降)
- Plan Player Analytics プラグイン (5.6以降)
- MiningTracker プラグイン (2.1.0以降)
- Java 21

### インストール手順

#### 1. Planのインストール

```bash
# Planプラグインをダウンロード
# https://www.spigotmc.org/resources/plan-player-analytics.32536/

# pluginsフォルダに配置
cp Plan-*.jar plugins/
```

#### 2. MiningTrackerのインストール

```bash
# MiningTrackerをダウンロード
# https://github.com/kubota6646/MiningTracker/releases

# pluginsフォルダに配置
cp MiningTracker-2.1.0.jar plugins/
```

#### 3. サーバー起動

```bash
# サーバーを起動
java -jar server.jar
```

プラグインが正常に起動すると、以下のログメッセージが表示されます：

```
[MiningTracker] Plan Player Analyticsとの連携を有効化しました。
```

### マルチサーバー環境の設定

複数のサーバーを運用している場合、各サーバーで異なる`server-name`を設定してください。

#### サーバーA (survival) の config.yml

```yaml
# MiningTracker 設定ファイル

# サーバー名設定（マルチサーバー環境で使用）
server-name: "survival"

# データベース設定
database:
  type: mysql  # マルチサーバーではMySQLを推奨
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: root
    password: your_password
```

#### サーバーB (creative) の config.yml

```yaml
# MiningTracker 設定ファイル

# サーバー名設定（マルチサーバー環境で使用）
server-name: "creative"

# データベース設定
database:
  type: mysql  # 同じMySQLデータベースを共有
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: root
    password: your_password
```

### データベース設定

#### シングルサーバー（SQLite）

```yaml
server-name: "default"

database:
  type: sqlite
  sqlite:
    file: mining_data.db
```

#### マルチサーバー（MySQL必須）

マルチサーバー環境では、全サーバーで同じMySQLデータベースを共有する必要があります。

```yaml
server-name: "survival"  # 各サーバーで異なる名前

database:
  type: mysql
  mysql:
    host: "mysql.example.com"  # 共通のMySQLサーバー
    port: 3306
    database: "minecraft_network"
    username: "minecraft"
    password: "secure_password"
    pool:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
```

## データの見方

### コマンドとPlanの違い

#### ゲーム内コマンド

```
/mtstats [プレイヤー名]
/mtranking [ページ]
```

これらのコマンドは**全サーバー合計**のデータを表示します。

#### Planのダッシュボード

Plan Player Analyticsのダッシュボードでは：

1. **プレイヤーページ**: 個人の詳細統計（全サーバー合計＋サーバー別）
2. **サーバーページ**: そのサーバーの統計
3. **ネットワークページ**: 全サーバーの統計と比較

### データの集計方法

#### プレイヤーの総採掘数

```
プレイヤーAの総採掘数 = サーバーA + サーバーB + サーバーC + ...
```

例：
- サーバーA (survival): 1000ブロック
- サーバーB (creative): 500ブロック
- **合計**: 1500ブロック

#### サーバーの総採掘数

```
サーバーAの総採掘数 = プレイヤー1 + プレイヤー2 + プレイヤー3 + ...
（サーバーAでの採掘のみ）
```

#### ネットワーク全体の総採掘数

```
ネットワーク総採掘数 = サーバーA + サーバーB + サーバーC + ...
                    = 全プレイヤー × 全サーバー の合計
```

## トラブルシューティング

### Planに統計が表示されない

**原因1: Planが正しくインストールされていない**

確認方法：
```
/plan
```

解決方法：
1. Planプラグインが`plugins`フォルダにあることを確認
2. サーバーを再起動

**原因2: MiningTrackerが正しく登録されていない**

確認方法：サーバーログで以下を確認
```
[MiningTracker] Plan Player Analyticsとの連携を有効化しました。
```

このメッセージが表示されない場合：
1. MiningTrackerのバージョンが2.1.0以降であることを確認
2. `plugin.yml`に`softdepend: [Plan]`があることを確認
3. サーバーを再起動

**原因3: データベースに接続できていない**

確認方法：
```
/mtstats
```

このコマンドが動作しない場合は、データベース接続に問題があります。

解決方法：
1. `config.yml`のデータベース設定を確認
2. MySQLの場合、接続情報が正しいか確認
3. サーバーログでエラーメッセージを確認

### Planの表示が一部英語になる（「Average 総採掘ブロック数」など）

**原因: Planの言語設定が英語になっている**

Planは統計をプレイヤー一覧テーブルに表示する際、自動的に「Average」（平均）などのラベルを追加します。
このラベルがPlanの言語設定に基づいて表示されるため、英語設定の場合は「Average 総採掘ブロック数」のように英語と日本語が混在します。

**注意**: MiningTracker v2.1.8以降、テーブル名は日本語Romaji（ローマ字）になっています。
- 例: `burokku_shubetsu_naiwake`（ブロック種類別内訳）、`toppu_maina`（トップマイナー）
- これは`@TableProvider`が`text`パラメータをサポートしていないための対応です

それでも英語が表示される場合は、以下の解決方法を試してください。

**解決方法：Planを日本語に設定する**

#### 方法1: config.ymlで言語を変更（推奨）

1. サーバーを停止
2. `plugins/Plan/config.yml`を編集：
```yaml
Plugin:
  # 言語設定を日本語に変更
  Locale: ja
  # 日本語ロケールファイルの自動生成を有効化
  Logging:
    Create_new_locale_file_on_next_enable: true
```

3. サーバーを起動（または`/plan reload`を実行）
4. Planが`plugins/Plan/locale.yml`を生成

これにより、「Average」は「平均」に、その他の英語ラベルも日本語に変換されます。

#### 方法2: Webインターフェースから変更

1. Plan管理画面を開く（例: `http://サーバーIP:8804`）
2. 「Manage」→「Server Settings」
3. 「General Settings」→「Locale」
4. 言語を「日本語 (ja)」に変更
5. 設定を保存

#### 方法3: showInPlayerTableを無効にする（非推奨）

完全に英語ラベルを回避したい場合、MiningTrackerの設定を変更することも可能です：
- `MiningTrackerExtension.java`の`showInPlayerTable = true`を`false`に変更
- ただし、この方法では統計がプレイヤー一覧テーブルに表示されなくなります

**推奨**: 方法1または方法2でPlanを日本語に設定してください。

#### 変更後の表示例

**変更前（英語設定）:**
- Average 総採掘ブロック数
- Total 総採掘ブロック数
- Max 総採掘ブロック数

**変更後（日本語設定）:**
- 平均 総採掘ブロック数
- 合計 総採掘ブロック数
- 最大 総採掘ブロック数

### マルチサーバーでデータが統合されない

**原因: 異なるデータベースを使用している**

解決方法：
1. 全サーバーで同じMySQLデータベースを使用
2. `config.yml`の`database.mysql`設定を全サーバーで統一
3. 各サーバーの`server-name`は異なる名前を設定

### パフォーマンスの問題

大量のデータがある場合、Planのページ読み込みが遅くなる可能性があります。

対策：
1. MySQLのインデックスが正しく作成されているか確認
2. HikariCP接続プールの設定を調整
3. Planの設定でデータ更新頻度を調整

## APIの使用

プラグイン開発者向け：MiningTrackerのAPIを使用して統計データにアクセスできます。

```java
import com.kubota6646.miningtracker.MiningTracker;
import com.kubota6646.miningtracker.database.DatabaseManager;

// MiningTrackerインスタンスを取得
MiningTracker tracker = (MiningTracker) Bukkit.getPluginManager().getPlugin("MiningTracker");
DatabaseManager db = tracker.getDatabaseManager();

// プレイヤーの全サーバー合計採掘数を取得
UUID playerUUID = player.getUniqueId();
long totalMined = db.getTotalMinedAllServers(playerUUID);

// 特定サーバーでの採掘数を取得
String serverName = "survival";
long serverMined = db.getTotalMinedByServer(playerUUID, serverName);

// ネットワーク全体の統計を取得
long networkTotal = db.getNetworkTotalMined();
```

## 開発者向け：ビルド設定

MiningTrackerをソースからビルドする場合、以下の依存関係が必要です。

### build.gradle 設定

```gradle
repositories {
    mavenCentral()
    maven {
        name = 'papermc'
        url = 'https://repo.papermc.io/repository/maven-public/'
    }
    maven {
        name = 'jitpack'
        url = 'https://jitpack.io'
    }
}

dependencies {
    compileOnly 'org.spigotmc:spigot-api:1.21-R0.1-SNAPSHOT'
    compileOnly 'com.github.plan-player-analytics:Plan:5.6.2959'
    implementation 'com.mysql:mysql-connector-j:8.3.0'
    implementation 'com.zaxxer:HikariCP:5.1.0'
    implementation 'org.slf4j:slf4j-simple:2.0.9'
}
```

### 重要なポイント

1. **JitPackリポジトリ**: Planの依存関係を解決するために必要
2. **Plan依存関係**: `com.github.plan-player-analytics:Plan:{version}` 形式を使用
3. **最新バージョン**: [Plan GitHub Tags](https://github.com/plan-player-analytics/Plan/tags)で確認
4. **compileOnly**: Planはランタイムで提供されるため、JARに含めない

### ビルドコマンド

```bash
./gradlew clean build
```

生成されるJARファイル: `build/libs/MiningTracker-2.1.0.jar`

## 参考リンク

- **Plan Player Analytics**: https://github.com/plan-player-analytics/Plan
- **Plan Documentation**: https://github.com/plan-player-analytics/Plan/wiki
- **Plan API**: https://github.com/plan-player-analytics/Plan/wiki/APIv5
- **MiningTracker GitHub**: https://github.com/kubota6646/MiningTracker

## FAQ

### Q: Planなしでも動作しますか？

A: はい。Planがインストールされていない場合でも、MiningTrackerは通常通り動作します。

### Q: 既存のデータは移行されますか？

A: はい。v2.1.0へのアップグレード時に、既存のデータは自動的に`server_name = 'default'`として保存されます。

### Q: サーバー名は後から変更できますか？

A: はい。ただし、変更後は新しいサーバー名でデータが記録されるため、履歴が分断されます。変更する場合は慎重に検討してください。

### Q: 一つのサーバーで複数のワールドがある場合は？

A: `server-name`はサーバー単位での設定です。同じサーバー内の全ワールドは同じサーバー名で記録されます。

### Q: プレイヤー名が変更された場合は？

A: プレイヤーはUUIDで識別されるため、名前変更後も統計は正しく引き継がれます。

## サポート

問題が発生した場合は、以下の情報を含めてGitHub Issuesで報告してください：

1. MiningTrackerのバージョン
2. Planのバージョン
3. Minecraftサーバーのバージョン
4. サーバーログ（エラーメッセージを含む）
5. `config.yml`の内容（パスワードは除く）

---

**MiningTracker v2.1.0** - Plan Player Analytics Integration  
© 2026 kubota6646
