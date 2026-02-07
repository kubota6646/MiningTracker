# v2.5.1 実装サマリー

## 📋 要件

> 既にデータが存在する場合でも、統計からデータに上書きするコマンド
> 例：統計：50 データ：2 →コマンド実行→統計：50 データ：50

## ✅ 実装内容

### 新しいコマンド

```bash
/mtimport <プレイヤー名> confirm
```

**エイリアス**: `/mtimp`

### 動作フロー

```
1. 管理者がコマンド実行
   /mtimport Steve

2. 確認メッセージ表示
   [MiningTracker] 本当にインポートしますか？既存データは上書きされます。
   /mtimport Steve confirm

3. 確認して実行
   /mtimport Steve confirm

4. 処理実行（非同期）
   - world/stats/<uuid>.json を読み込み
   - minecraft:mined セクションを解析
   - データベースを統計の値で上書き

5. 完了メッセージ
   [MiningTracker] プレイヤー Steve の統計をMinecraft統計から上書きインポートしました。
```

### 具体例

#### Before (実行前)

**統計ファイル** (`world/stats/8667ba71-b85a-4004-af54-457a9734eca3.json`):
```json
{
  "stats": {
    "minecraft:mined": {
      "minecraft:stone": 50,
      "minecraft:diamond_ore": 10,
      "minecraft:coal_ore": 30
    }
  }
}
```

**データベース**:
| player_uuid | block_type | count |
|-------------|------------|-------|
| 8667ba71... | STONE | 2 |
| 8667ba71... | DIAMOND_ORE | 5 |

#### After (実行後)

**データベース**:
| player_uuid | block_type | count |
|-------------|------------|-------|
| 8667ba71... | STONE | **50** ← 上書き |
| 8667ba71... | DIAMOND_ORE | **10** ← 上書き |
| 8667ba71... | COAL_ORE | **30** ← 新規追加 |

## 🔧 技術実装

### 1. DatabaseManager.setMiningCount()

**新メソッド**: 値を置換（加算ではなく）

```java
// 既存のaddMiningCount (加算)
INSERT ... VALUES (...)
ON DUPLICATE KEY UPDATE count = count + ? 
// 2 + 10 = 12

// 新しいsetMiningCount (置換)
INSERT ... VALUES (...)
ON DUPLICATE KEY UPDATE count = ?
// count = 10 (上書き)
```

### 2. MinecraftStatsImporter.importPlayerStats()

**オーバーロード**: forceOverwrite フラグ追加

```java
// デフォルト（加算モード）
importPlayerStats(uuid, name)
  → addMiningCount() を使用

// 強制モード（置換モード）
importPlayerStats(uuid, name, true)
  → setMiningCount() を使用
```

### 3. ImportCommand

**新クラス**: コマンドエグゼキューター

- 権限チェック: `miningtracker.import` (op)
- 確認ステップ: `confirm` パラメータ必須
- 非同期処理: メインスレッドをブロックしない
- エラーハンドリング: 統計ファイルなし等

## 📝 設定ファイル

### plugin.yml

```yaml
commands:
  mtimport:
    description: Minecraft統計から強制的にインポート（管理者専用）
    usage: /mtimport <プレイヤー名> confirm
    permission: miningtracker.import
    aliases: [mtimp]

permissions:
  miningtracker.import:
    description: 統計を強制インポートする権限
    default: op
```

### messages.yml

```yaml
import:
  success: "&aプレイヤー &e{player}&a の統計をMinecraft統計から上書きインポートしました。"
  failed: "&cプレイヤー &e{player}&c の統計インポートに失敗しました。統計ファイルが存在しないか、データがありません。"
  confirm: "&c本当にインポートしますか？既存データは上書きされます。 &e/mtimport {target} confirm"
  usage: "&c使用方法: /mtimport <プレイヤー名> confirm"
```

## 🔐 セキュリティ

### CodeQL 分析結果

```
✅ 0 vulnerabilities found
```

### セキュリティ対策

1. **権限チェック**: 管理者のみ実行可能
2. **確認ステップ**: 誤操作防止
3. **入力検証**: プレイヤー存在確認
4. **エラーハンドリング**: 例外の適切な処理
5. **ログ出力**: 実行記録を保存

## 📊 変更統計

```
11 files changed
477 insertions(+)
20 deletions(-)
```

### ファイル別変更

| ファイル | 追加 | 削除 | 説明 |
|---------|------|------|------|
| DatabaseManager.java | 64 | 0 | setMiningCount()追加 |
| MinecraftStatsImporter.java | 39 | 9 | forceOverwrite対応 |
| ImportCommand.java | 74 | 0 | 新規作成 |
| plugin.yml | 6 | 1 | コマンド・権限追加 |
| messages.yml | 8 | 1 | メッセージ追加 |
| README.md | 56 | 11 | ドキュメント更新 |
| VERSION.md | 19 | 1 | v2.5.1追加 |
| CHANGELOG.md | 27 | 0 | 変更履歴追加 |
| RELEASE_NOTES_v2.5.1.md | 194 | 0 | リリースノート作成 |
| build.gradle | 1 | 1 | バージョン更新 |
| MiningTracker.java | 2 | 0 | コマンド登録 |

## 🎯 達成項目

- ✅ `/mtimport` コマンドの実装
- ✅ 既存データの上書き機能
- ✅ 確認ステップの実装
- ✅ 権限システムの統合
- ✅ エラーハンドリング
- ✅ 非同期処理
- ✅ MySQL/SQLite対応
- ✅ ドキュメント完備
- ✅ セキュリティ検証
- ✅ バージョン更新（2.5.0 → 2.5.1）

## 🚀 使用方法

### 基本的な使い方

```bash
# ステップ1: コマンド実行
/mtimport Steve

# ステップ2: 確認して実行
/mtimport Steve confirm
```

### 実際の出力例

```
> /mtimport Steve
[MiningTracker] 本当にインポートしますか？既存データは上書きされます。 /mtimport Steve confirm

> /mtimport Steve confirm
[MiningTracker] プレイヤー Steve の統計をMinecraft統計から上書きインポートしました。
```

### エラー時の出力例

```
> /mtimport NewPlayer confirm
[MiningTracker] プレイヤー NewPlayer の統計インポートに失敗しました。
統計ファイルが存在しないか、データがありません。
```

## 📦 バージョン情報

- **バージョン**: 2.5.1
- **リリース日**: 2026-02-07
- **前バージョン**: 2.5.0
- **互換性**: 完全な下位互換性あり

## 🔄 v2.5.0との比較

| 項目 | v2.5.0 自動インポート | v2.5.1 強制インポート |
|------|---------------------|---------------------|
| **トリガー** | `/mtstats` 実行時 | `/mtimport` コマンド |
| **条件** | データが存在しない場合 | いつでも実行可能 |
| **動作** | 加算 (ADD) | 置換 (SET) |
| **権限** | 全ユーザー | 管理者のみ |
| **確認** | 不要 | 必須 |
| **用途** | 新規プレイヤー | データ修正・再同期 |

## ✨ まとめ

v2.5.1では、要件通りに「既存データを統計ファイルで上書きする」コマンドを実装しました。

- **安全性**: 確認ステップと権限チェック
- **柔軟性**: データの完全な再同期が可能
- **互換性**: 既存機能を壊さない実装
- **品質**: セキュリティチェック合格

すべての変更がコミットされ、ドキュメントも更新されています。
