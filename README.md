# MiningTracker

Minecraft 1.19.4対応のプレイヤー採掘トラッキングプラグイン

## 概要

MiningTrackerは、各プレイヤーがどのブロックをどれだけ採掘したかを記録し、ランキング形式で表示するMinecraftプラグインです。

## 機能

- **採掘トラッキング**: プレイヤーが破壊したブロックを自動的に記録
- **統計表示**: 自分や他のプレイヤーの採掘統計をコマンドで確認
- **ランキング表示**: 全プレイヤーの採掘量をランキング形式で表示
- **データ永続化**: SQLiteデータベースを使用してデータを保存
- **柔軟な設定**: config.ymlで動作をカスタマイズ可能

## 動作環境

- Minecraft バージョン: 1.19.4
- Java バージョン: 17以降
- サーバー: Spigot/Paper 1.19.4

## ビルド方法

### 前提条件
- Java 17以降がインストールされていること
- Gradleがインストールされていること（またはGradle Wrapperを使用）

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

## 設定

### config.yml

```yaml
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
```

事前にMySQLサーバー上でデータベースを作成：
```sql
CREATE DATABASE minecraft CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

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

## サポート

問題が発生した場合は、GitHubのIssuesページで報告してください。