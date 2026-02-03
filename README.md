# MiningTracker

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-green.svg)](https://www.minecraft.net/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Minecraft 1.21.x対応のプレイヤー採掘トラッキングプラグイン

## バージョン情報

- **最新バージョン**: 2.1.0
- **対応Minecraft**: 1.21.x
- **必須Java**: 21以降

## 概要

MiningTrackerは、各プレイヤーがどのブロックをどれだけ採掘したかを記録し、ランキング形式で表示するMinecraftプラグインです。

## 主な機能

- **採掘トラッキング**: プレイヤーが破壊したブロックを自動的に記録
- **統計表示**: 自分や他のプレイヤーの採掘統計をコマンドで確認
- **ランキング表示**: 全プレイヤーの採掘量をランキング形式で表示
- **マルチサーバー対応**: 複数サーバー間でデータを統合管理
- **Plan連携**: Plan Player Analyticsと統合してWebダッシュボードに統計を表示 ⭐ NEW
- **データ永続化**: SQLiteまたはMySQLデータベースを使用
- **柔軟な設定**: config.ymlで動作をカスタマイズ可能

## 動作環境

- Minecraft バージョン: 1.21.x (1.21, 1.21.1, 1.21.3など)
- Java バージョン: 21以降（必須）
- Gradle バージョン: 8.5以降
- サーバー: Spigot/Paper 1.21.x
- MySQL サーバー（オプション）: 5.7以降または8.0以降
- **Plan Player Analytics**（オプション）: 5.6以降

**セキュリティ**: MySQL使用時はMySQL Connector/J 8.3.0を使用しています（脆弱性修正済み）。

## ビルド方法

### 前提条件
- Java 21以降がインストールされていること（必須）
- Gradle 8.5以降がインストールされていること（またはGradle Wrapperを使用）

**重要**: Minecraft 1.21.xではJava 21が必須要件です。Java 17では動作しません。

### ビルドコマンド

```bash
# Gradle Wrapperを使用する場合（推奨）
./gradlew build

# システムのGradleを使用する場合
gradle build
```

ビルドが成功すると、`build/libs/MiningTracker-1.0.0.jar`が生成されます。

## インストール

1. ビルドされたJARファイルをサーバーの`plugins`フォルダにコピー
2. サーバーを再起動
3. `plugins/MiningTracker`フォルダに設定ファイルが自動生成されます

## コマンド

### `/mtstats [プレイヤー名]`
- **エイリアス**: `/mts`, `/minestats`
- **説明**: 採掘統計を表示します
- **引数なし**: 自分の統計を表示
- **プレイヤー名指定**: 指定したプレイヤーの統計を表示
- **権限**: `miningtracker.use` (デフォルト: true)

### `/mtranking [ページ番号]`
- **エイリアス**: `/mtr`, `/mineranking`
- **説明**: 採掘ランキングを表示します
- **引数なし**: 1ページ目を表示
- **ページ番号指定**: 指定したページを表示
- **権限**: `miningtracker.use` (デフォルト: true)

### `/mtreset <プレイヤー名|all> [confirm]`
- **エイリアス**: `/mtres`
- **説明**: 統計をリセットします（管理者専用）
- **プレイヤー名**: 指定したプレイヤーの統計をリセット
- **all**: 全プレイヤーの統計をリセット
- **confirm**: リセットを確定（安全のため2段階実行）
- **権限**: `miningtracker.reset` (デフォルト: op)

## 権限

| 権限 | 説明 | デフォルト |
|------|------|-----------|
| `miningtracker.use` | プラグインの基本機能を使用 | true |
| `miningtracker.other` | 他のプレイヤーの統計を閲覧 | true |
| `miningtracker.reset` | 統計をリセット | op |

## Plan Player Analytics 連携

MiningTracker v2.1.0以降では、[Plan Player Analytics](https://github.com/plan-player-analytics/Plan)プラグインと連携して、
Webダッシュボードで採掘統計を視覚的に表示できます。

### 表示される統計

- **プレイヤーページ**: 個人の採掘統計（全サーバー合計・サーバー別）
- **サーバーページ**: サーバーごとの採掘統計とランキング
- **ネットワークページ**: 全サーバー合計統計とサーバー比較

### マルチサーバー対応

複数のサーバーを運用している場合、各サーバーで異なる`server-name`を設定することで、
サーバー別の統計を取得しつつ、全サーバーのデータを統合できます。

```yaml
# サーバーAのconfig.yml
server-name: "survival"

# サーバーBのconfig.yml
server-name: "creative"
```

ゲーム内コマンド（`/mtstats`, `/mtranking`）は**全サーバー合計**のデータを表示します。

**詳細**: [PLAN_INTEGRATION.md](PLAN_INTEGRATION.md)を参照してください。

### 注意: Planの日本語化

**MiningTracker v2.1.9以降**: テーブルの重複表示が修正されています。
- v2.1.8からv2.1.9へのアップデートで、`@InvalidateMethod`による古いメソッド名からの移行が追加されました
- テーブルが2つずつ表示される問題が解決されています

**MiningTracker v2.1.8以降**: テーブル名は日本語Romaji（ローマ字）になっています。
- 例: `burokku_shubetsu_naiwake`（ブロック種類別内訳）、`toppu_maina`（トップマイナー）
- これは`@TableProvider`が`text`パラメータをサポートしていないための対応です

それでもPlanで「Average 総採掘ブロック数」のように英語と日本語が混在する場合は、Plan自体の言語設定を変更する必要があります。

**解決方法**: `plugins/Plan/config.yml`で言語を日本語に設定してください：
```yaml
Plugin:
  Locale: ja  # 日本語に設定
```

詳細は [PLAN_INTEGRATION.md#planの表示が一部英語になる](PLAN_INTEGRATION.md#planの表示が一部英語になるaverage-総採掘ブロック数など) を参照してください。

## 設定

### config.yml

```yaml
# サーバー名設定（マルチサーバー環境で使用）
server-name: "default"

# データベース設定
database:
  type: sqlite              # データベースタイプ (sqlite または mysql)
  
  # SQLite設定
  sqlite:
    file: mining_data.db    # SQLiteファイル名
  
  # MySQL設定
  mysql:
    host: localhost         # MySQLサーバーのホスト
    port: 3306             # MySQLサーバーのポート
    database: minecraft    # データベース名
    username: root         # ユーザー名
    password: password     # パスワード
    pool:
      maximum-pool-size: 10      # 最大接続数
      minimum-idle: 2            # 最小アイドル接続数
      connection-timeout: 30000  # 接続タイムアウト（ミリ秒）

# 採掘カウント設定
tracking:
  enabled: true             # 採掘トラッキングの有効/無効
  count-creative: false     # クリエイティブモードでの採掘をカウント
  count-silk-touch: true    # シルクタッチでの採掘をカウント

# ランキング設定
ranking:
  per-page: 10             # 1ページあたりの表示件数
  show-zero: false         # 採掘数0のプレイヤーを表示

# メッセージ設定
messages:
  use-message-file: true   # messages.ymlを使用
  language: ja             # 言語設定
```

### messages.yml

プラグインのすべてのメッセージをカスタマイズできます。
カラーコードは`&`を使用します（例: `&a` = 緑、`&e` = 黄色）。

## 使用例

### 自分の統計を確認
```
/mtstats
```

### 他のプレイヤーの統計を確認
```
/mtstats Steve
```

### ランキングを表示
```
/mtranking
/mtranking 2  # 2ページ目
```

### 統計をリセット
```
/mtreset Steve confirm        # Steveの統計をリセット
/mtreset all confirm          # 全プレイヤーの統計をリセット
```

## データベース

プラグインはSQLiteまたはMySQLを使用してデータを保存します。

### SQLite（デフォルト）
- 設定不要で簡単に使用可能
- データベースファイルは`plugins/MiningTracker/mining_data.db`に保存されます
- 小規模サーバーに最適

### MySQL
- 大規模サーバーや複数サーバーでのデータ共有に最適
- config.ymlで接続情報を設定
- パフォーマンスと拡張性に優れる
- **HikariCP接続プール**を使用した高速データベースアクセス
- 自動接続管理と最適化されたクエリ実行
- 複数の同時接続を効率的に処理

### MySQL使用例

config.ymlを以下のように設定：
```yaml
database:
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: your_username
    password: your_password
    pool:
      maximum-pool-size: 10      # 最大接続数（デフォルト: 10）
      minimum-idle: 2            # 最小アイドル接続数（デフォルト: 2）
      connection-timeout: 30000  # 接続タイムアウト（ミリ秒、デフォルト: 30000）
```

事前にMySQLサーバー上でデータベースを作成：
```sql
CREATE DATABASE minecraft CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 接続プール設定について
- **maximum-pool-size**: 同時に維持する最大接続数。プレイヤー数やサーバー負荷に応じて調整してください
  - 小規模サーバー（〜50人）: 5-10
  - 中規模サーバー（50-200人）: 10-20
  - 大規模サーバー（200人以上）: 20-30
- **minimum-idle**: アイドル状態で維持する最小接続数。レスポンス時間を改善します
- **connection-timeout**: 接続取得のタイムアウト時間（ミリ秒）

#### MySQL性能最適化
HikariCPは以下の最適化を自動的に適用します：
- PreparedStatementのキャッシュ
- バッチ書き込みの最適化
- サーバー設定のキャッシュ
- 接続の自動再利用とプール管理

### テーブル構造

**mining_data**
- `id`: プライマリキー
- `player_uuid`: プレイヤーのUUID
- `player_name`: プレイヤー名
- `block_type`: ブロックタイプ
- `count`: 採掘数

## トラブルシューティング

### プラグインが起動しない
- Java 17以降がインストールされているか確認
- サーバーログでエラーメッセージを確認
- config.ymlの構文が正しいか確認

### データが保存されない
- `plugins/MiningTracker`フォルダの書き込み権限を確認
- サーバーログでデータベースエラーを確認
- MySQL使用時はMySQLサーバーが稼働していることを確認
- MySQL接続情報（ホスト、ポート、ユーザー名、パスワード）が正しいか確認

### MySQL接続エラー
- MySQLサーバーが起動しているか確認
- ファイアウォールでポート3306が開いているか確認
- MySQLユーザーに適切な権限があるか確認：
  ```sql
  GRANT ALL PRIVILEGES ON minecraft.* TO 'your_username'@'localhost';
  FLUSH PRIVILEGES;
  ```
- 接続プール設定が適切か確認（maximum-pool-sizeが大きすぎないか等）

### コマンドが動作しない
- 権限が正しく設定されているか確認
- `/plugins`コマンドでプラグインが有効になっているか確認

## ライセンス

このプラグインはMITライセンスの下で公開されています。

## 開発者向け情報

### プロジェクト構造
```
MiningTracker/
├── src/main/
│   ├── java/com/kubota6646/miningtracker/
│   │   ├── MiningTracker.java          # メインクラス
│   │   ├── commands/                   # コマンド実装
│   │   ├── listeners/                  # イベントリスナー
│   │   ├── managers/                   # マネージャークラス
│   │   └── database/                   # データベース管理
│   └── resources/
│       ├── plugin.yml                  # プラグイン定義
│       ├── config.yml                  # デフォルト設定
│       └── messages.yml                # メッセージ定義
├── build.gradle                        # Gradleビルド設定
└── settings.gradle                     # Gradleプロジェクト設定
```

### 開発環境のセットアップ

```bash
# リポジトリをクローン
git clone https://github.com/kubota6646/MiningTracker.git
cd MiningTracker

# ビルド
./gradlew build

# クリーンビルド
./gradlew clean build
```

### 使用している主要なライブラリ

- **Spigot API 1.19.4**: Minecraftプラグイン開発API
- **MySQL Connector/J 8.3.0**: MySQL JDBC ドライバー（脆弱性対策済み）
- **HikariCP 5.1.0**: 高性能JDBC接続プール
- **SLF4J 2.0.9**: ログフレームワーク

### データベース実装の詳細

#### SQLiteモード
- 直接JDBC接続を使用
- プラグインフォルダ内にデータベースファイルを作成
- シングルサーバー環境に最適

#### MySQLモード
- HikariCP接続プールを使用
- 設定可能な接続プール設定
- 自動再接続とコネクション管理
- PreparedStatementキャッシュによる性能向上
- マルチサーバー環境でのデータ共有に対応
./gradlew build

# クリーンビルド
./gradlew clean build
```

## サポート

問題が発生した場合は、GitHubのIssuesページで報告してください。
