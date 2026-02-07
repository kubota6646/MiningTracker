# MiningTracker v2.5.0 リリースノート

**リリース日**: 2026-02-07

## 🎉 新機能

### Minecraft統計インポート機能

プラグインに新しい**統計インポート機能**が追加されました！

#### 何ができるようになったか？

既存のデータが存在しない場合、Minecraftの内蔵統計ファイルから採掘データを**自動的にインポート**できるようになりました。

#### 利用シーン

- **新規プレイヤー**: MiningTracker導入前に既に採掘していたブロックのデータを引き継げます
- **既存サーバー**: プラグイン追加時に、プレイヤーの過去の採掘実績が失われません
- **データ移行**: バニラ統計からスムーズに移行できます

#### 動作の仕組み

1. プレイヤーが `/mtstats` を実行
2. データベースにデータがない場合、自動的に `world/stats/<UUID>.json` を検索
3. Minecraftの `minecraft:mined` セクションからブロック採掘データを読み込み
4. データベースに一括インポート
5. インポート後の統計を表示

#### 技術的な詳細

- **対象データ**: `minecraft:mined` セクションのブロック採掘統計のみ
- **フィルタリング**: ブロックタイプのみをインポート（アイテムは除外）
- **非同期処理**: メインスレッドをブロックしません
- **エラーハンドリング**: 統計ファイルがない場合でも安全に動作

#### 設定方法

`config.yml` で機能の有効/無効を切り替えられます：

```yaml
# Minecraft統計インポート設定
import:
  enabled: true  # true: 自動インポート有効、false: 無効
```

デフォルトは `true`（有効）です。

#### 使用例

```
# データがないプレイヤーが統計を確認
/mtstats

# 自動的にMinecraft統計からインポート
[MiningTracker] プレイヤー Steve の統計をインポートしました: 15種類のブロック

# インポート後の統計が表示される
========== Steve の採掘統計 ==========
総採掘ブロック数: 5,234
...
```

詳細なドキュメントは [MINECRAFT_STATS_IMPORT.md](MINECRAFT_STATS_IMPORT.md) を参照してください。

## 🔧 改善

### コード品質向上

- **MinecraftStatsImporter**: ワールドリストが空の場合の `IndexOutOfBoundsException` を防止
  - `Bukkit.getWorlds().get(0)` → `Bukkit.getWorlds().stream().findFirst()` に変更
- **DatabaseManager**: PreparedStatement パラメータにコメント追加
  - INSERT/UPDATE で使用される count 値の意味を明確化

## 📚 ドキュメント

### 新規ドキュメント追加

- **MINECRAFT_STATS_IMPORT.md**: 統計インポート機能の詳細ガイド
  - 機能の概要
  - 動作の仕組み
  - 設定方法
  - 使用例とトラブルシューティング

### 既存ドキュメント更新

- **README.md**: v2.5.0 の新機能を追加
- **CHANGELOG.md**: 詳細な変更履歴を記載
- **VERSION.md**: バージョン情報を更新

## 🔄 マイグレーション

### v2.4.2 からのアップグレード

このバージョンは **完全な下位互換性** があります。

#### 必要なアクション

1. 新しい JAR ファイルに置き換え
2. サーバーを再起動

#### 自動マイグレーション

- `config.yml` に新しい `import` セクションが自動的に追加されます
- 既存のデータベースには影響しません
- 既存の設定はすべて保持されます

#### 新しい設定項目

```yaml
# config.yml に自動追加される
import:
  enabled: true
```

この設定は任意で変更できます。

## 📦 ダウンロード

### ファイル

- **Bukkit/Paper用**: `MiningTracker-Bukkit-2.5.0.jar`
- **Bungeecord用**: `MiningTracker-Bungee-2.5.0.jar`

### インストール

Bukkit/Paper サーバー：
```bash
# plugins フォルダに配置
cp MiningTracker-Bukkit-2.5.0.jar /path/to/server/plugins/
```

Bungeecord プロキシ（オプション）：
```bash
# plugins フォルダに配置
cp MiningTracker-Bungee-2.5.0.jar /path/to/bungeecord/plugins/
```

## 💡 使用方法

### 基本的な使い方

```bash
# 自分の統計を確認（データがなければ自動インポート）
/mtstats

# 他のプレイヤーの統計を確認
/mtstats <プレイヤー名>

# ランキングを表示
/mtranking
```

### 統計インポートを無効にする場合

`config.yml`:
```yaml
import:
  enabled: false
```

## 🐛 既知の問題

現在、重大な既知の問題はありません。

## 🔜 次のバージョン予定

### v2.5.1 (検討中)

- 手動インポートコマンドの追加 (`/mtimport`)
- 定期的な自動インポート機能
- マルチワールド対応の改善

## 📋 システム要件

- **Minecraft**: 1.21.x
- **Java**: 21以降（必須）
- **サーバー**: Spigot/Paper 1.21.x
- **データベース**: SQLite（デフォルト）または MySQL 5.7以降

## 🙏 謝辞

このリリースは以下の要望に基づいています：
- 「既存のデータが存在しない場合は、Minecraftの統計から採掘したブロック数などをインポートするように変更してください」

フィードバックをありがとうございました！

## 📞 サポート

問題が発生した場合は、GitHubのIssuesページで報告してください。

---

**Full Changelog**: https://github.com/kubota6646/MiningTracker/compare/v2.4.2...v2.5.0
