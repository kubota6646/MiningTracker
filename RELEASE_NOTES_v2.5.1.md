# MiningTracker v2.5.1 リリースノート

**リリース日**: 2026-02-07

## 🎉 新機能

### 強制インポートコマンド (`/mtimport`)

既存データがある場合でも、Minecraftの統計ファイルでデータを**上書き**できる新しいコマンドを追加しました。

#### コマンド

```bash
/mtimport <プレイヤー名> confirm
```

**エイリアス**: `/mtimp`

#### 動作の仕組み

既存のデータベースの値を、Minecraft統計ファイルの値で**置き換え**ます。

**例**:
- 統計ファイル: 石 50個、ダイヤ 10個
- データベース: 石 2個、ダイヤ 5個
- **実行後**: 石 50個、ダイヤ 10個

#### v2.5.0 の自動インポートとの違い

| 機能 | v2.5.0 自動インポート | v2.5.1 強制インポート |
|------|---------------------|---------------------|
| トリガー | `/mtstats` でデータなしの場合 | `/mtimport` コマンド実行 |
| 既存データ | 存在しない場合のみ | 存在しても上書き |
| 動作 | 加算 | 置換 |
| 権限 | 自動（全ユーザー） | `miningtracker.import` (op) |

#### 使用ケース

1. **データ修正**: プレイヤーのデータが壊れた場合の復元
2. **再同期**: 統計ファイルからの完全な再同期
3. **管理者操作**: データベースとバニラ統計の不一致を修正

#### 安全性

- **確認ステップ必須**: 実行には `confirm` パラメータが必要
- **警告メッセージ**: 既存データが上書きされることを警告
- **管理者専用**: デフォルトでop権限が必要

## 🔧 実装の詳細

### 新しいメソッド

#### DatabaseManager.setMiningCount()

既存の `addMiningCount()` は値を加算しますが、新しい `setMiningCount()` は値を置換します。

```java
// 加算（既存）
addMiningCount(uuid, name, material, 10);  // 2 + 10 = 12

// 置換（新規）
setMiningCount(uuid, name, material, 10);  // 10（上書き）
```

#### MinecraftStatsImporter.importPlayerStats() オーバーロード

```java
// デフォルト（加算モード）
importPlayerStats(uuid, name);

// 強制モード（置換モード）
importPlayerStats(uuid, name, true);
```

### SQL実装

MySQL:
```sql
INSERT INTO mining_data (...) VALUES (...)
ON DUPLICATE KEY UPDATE count = ? -- 加算ではなく置換
```

SQLite:
```sql
INSERT INTO mining_data (...) VALUES (...)
ON CONFLICT(...) DO UPDATE SET count = ? -- 加算ではなく置換
```

## 📦 インストール

### v2.5.0 からのアップグレード

完全な下位互換性があります。JARファイルを置き換えるだけです。

```bash
# Bukkit/Paper サーバー
cp MiningTracker-Bukkit-2.5.1.jar /path/to/server/plugins/

# Bungeecord プロキシ
cp MiningTracker-Bungee-2.5.1.jar /path/to/bungeecord/plugins/
```

設定ファイルやデータベースの変更は不要です。

## 💡 使用例

### 基本的な使い方

```bash
# ステップ1: コマンド実行（確認要求）
/mtimport Steve

# 出力:
# [MiningTracker] 本当にインポートしますか？既存データは上書きされます。
# /mtimport Steve confirm

# ステップ2: 確認して実行
/mtimport Steve confirm

# 出力:
# [MiningTracker] プレイヤー Steve の統計をMinecraft統計から上書きインポートしました。
```

### エラーハンドリング

```bash
# 統計ファイルがない場合
/mtimport NewPlayer confirm
# [MiningTracker] プレイヤー NewPlayer の統計インポートに失敗しました。
# 統計ファイルが存在しないか、データがありません。

# 権限がない場合
/mtimport Steve confirm
# [MiningTracker] 権限がありません。
```

## 🔄 マイグレーション

### 必要なアクション

なし。完全な下位互換性があります。

### 自動設定

新しいメッセージが自動的に `messages.yml` に追加されます：
- `import.usage`
- `import.confirm`
- `import.success`
- `import.failed`

既存のメッセージファイルがある場合、手動で追加することもできます。

## 📋 コマンド一覧（更新版）

| コマンド | 説明 | 権限 |
|---------|------|------|
| `/mtstats [player]` | 統計表示 | `miningtracker.use` |
| `/mtranking [page]` | ランキング表示 | `miningtracker.use` |
| `/mtreset <player\|all> confirm` | 統計リセット | `miningtracker.reset` (op) |
| `/mtimport <player> confirm` ⭐ | 統計強制インポート | `miningtracker.import` (op) |

## 🐛 既知の問題

現在、重大な既知の問題はありません。

## 🔜 次のバージョン予定

### v2.5.2 (検討中)

- インポート履歴の記録
- 一括インポート機能
- ドライランモード（実際にインポートせずに確認）

## 📋 システム要件

- **Minecraft**: 1.21.x
- **Java**: 21以降（必須）
- **サーバー**: Spigot/Paper 1.21.x
- **データベース**: SQLite（デフォルト）または MySQL 5.7以降

## 🙏 謝辞

このリリースは以下の要望に基づいています：
- 「既にデータが存在する場合でも、統計からデータに上書きするコマンド」

フィードバックをありがとうございました！

## 📞 サポート

問題が発生した場合は、GitHubのIssuesページで報告してください。

---

**Full Changelog**: https://github.com/kubota6646/MiningTracker/compare/v2.5.0...v2.5.1
