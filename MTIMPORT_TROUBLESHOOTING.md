# /mtimport コマンド トラブルシューティングガイド

## 問題

`/mtimport <プレイヤー名> confirm` コマンドが正常に動作しない

## 実装された修正

v2.5.1で追加された `/mtimport` コマンドに包括的なデバッグログを追加しました。

### 追加されたログ機能

#### ImportCommand.java
- コマンド実行時のログ（実行者、引数）
- 権限チェック結果のログ
- プレイヤー検索結果の詳細ログ（UUID、名前、プレイ履歴、オンライン状態）
- 非同期タスクの開始・完了ログ
- 例外キャッチとログ出力
- ユーザーへのエラーフィードバック

#### MinecraftStatsImporter.java
- インポートパラメータのログ（UUID、名前、強制上書きフラグ）
- 統計ファイルパスのログ（絶対パス）
- 統計ファイル存在チェックのログ
- パースされたブロックタイプ数のログ
- ワールドフォルダと統計フォルダのパスログ
- 詳細なエラー情報とファイルパス

## 診断方法

サーバーログを確認することで、以下の問題を特定できます：

### 1. 権限の問題
```
[INFO] ImportCommand executed by PlayerName with args: Steve, confirm
[INFO] Permission denied for PlayerName
```
**解決方法**: プレイヤーに `miningtracker.import` 権限を付与

### 2. 引数の不足
```
[INFO] ImportCommand executed by Admin with args: Steve
[INFO] Target player: Steve, Confirmed: false
```
**解決方法**: `confirm` パラメータを追加 (`/mtimport Steve confirm`)

### 3. プレイヤーが見つからない
```
[INFO] Player lookup result - UUID: xxx, Name: Steve, HasPlayedBefore: false, IsOnline: false
[INFO] Player not found or never played: Steve
```
**解決方法**: プレイヤー名が正確か確認、またはプレイヤーが一度もサーバーに参加していない

### 4. 統計ファイルが見つからない
```
[INFO] Stats file path: /path/to/world/stats/xxx-xxx-xxx.json
[WARNING] 統計ファイルが見つかりません: xxx-xxx-xxx (Path: /path/to/world/stats/xxx-xxx-xxx.json)
```
**原因**: 
- プレイヤーがまだブロックを採掘していない
- 統計ファイルが存在しない
- ワールドフォルダのパスが間違っている

**解決方法**: 
- プレイヤーが実際にブロックを採掘したことを確認
- 統計ファイルが存在することを確認
- ワールドフォルダのパスを確認

### 5. 統計ファイルにデータがない
```
[INFO] Stats file found, attempting to parse: /path/to/world/stats/xxx.json
[INFO] Parsed 0 block types from stats file
[WARNING] 統計ファイルに採掘データがありません: Steve
```
**原因**: 統計ファイルは存在するが、`minecraft:mined` セクションにデータがない

**解決方法**: プレイヤーが実際にブロックを採掘したことを確認

### 6. 解析エラー
```
[WARNING] 統計ファイルの読み込みエラー: Steve (File: /path/to/world/stats/xxx.json)
java.io.IOException: ...
```
**原因**: JSONファイルの形式が不正

**解決方法**: 統計ファイルの内容を確認、必要に応じてバックアップから復元

## 成功時のログ例

```
[INFO] ImportCommand executed by Admin with args: Steve, confirm
[INFO] Target player: Steve, Confirmed: true
[INFO] Player lookup result - UUID: 12345678-1234-1234-1234-123456789012, Name: Steve, HasPlayedBefore: true, IsOnline: false
[INFO] Initiating import for player: Steve
[INFO] Starting async import task for player: Steve (UUID: 12345678-1234-1234-1234-123456789012)
[INFO] Async task started - calling importPlayerStats
[INFO] importPlayerStats called - UUID: 12345678-1234-1234-1234-123456789012, Name: Steve, forceOverwrite: true
[INFO] Main world found: world
[INFO] World folder: /path/to/world
[INFO] Stats folder: /path/to/world/stats
[INFO] Stats folder exists: true
[INFO] Looking for stats file: /path/to/world/stats/12345678-1234-1234-1234-123456789012.json
[INFO] Stats file exists: true
[INFO] Stats file path: /path/to/world/stats/12345678-1234-1234-1234-123456789012.json
[INFO] Stats file found, attempting to parse: /path/to/world/stats/12345678-1234-1234-1234-123456789012.json
[INFO] Parsed 15 block types from stats file
[INFO] プレイヤー Steve の統計を上書きインポートしました: 15種類のブロック
[INFO] Import result for Steve: SUCCESS
```

## よくある問題と解決方法

### 問題: コマンドが認識されない
- プラグインが正しくロードされているか確認
- プラグインのバージョンが v2.5.1 以降であることを確認
- サーバーを再起動

### 問題: 権限エラー
- `miningtracker.import` 権限を付与（デフォルトはOP）
- 権限プラグインの設定を確認

### 問題: プレイヤー名が認識されない
- プレイヤー名の大文字小文字を正確に入力
- プレイヤーが少なくとも一度サーバーに参加したことを確認
- スペースや特殊文字を避ける

### 問題: 統計ファイルが見つからない
- プレイヤーのUUIDを確認
- ワールドフォルダの `stats` ディレクトリを確認
- プレイヤーが実際にバニラのMinecraftでブロックを採掘したことを確認

## コマンドの正しい使用方法

```bash
# ステップ1: 確認メッセージを表示
/mtimport <プレイヤー名>

# ステップ2: 確認して実行
/mtimport <プレイヤー名> confirm

# 例:
/mtimport Steve
# → メッセージが表示される: "本当にインポートしますか？既存データは上書きされます。 /mtimport Steve confirm"

/mtimport Steve confirm
# → インポートが実行される
```

## サポート

問題が解決しない場合は、以下の情報を含めてサポートに連絡してください：
1. サーバーログの該当部分（上記のログ出力）
2. Minecraftバージョン
3. プラグインバージョン
4. 使用したコマンド
5. エラーメッセージ
