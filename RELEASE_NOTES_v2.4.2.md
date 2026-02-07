# MiningTracker v2.4.2 リリースノート

**リリース日**: 2026-02-07

## 📋 概要

v2.4.2は、v2.4.1で導入されたビルドエラーを修正するパッチリリースです。Plan APIの存在しないメソッドを使用していた問題を解決し、Planの自動更新メカニズムに依存する方式に変更しました。

## 🐛 修正した問題

### ビルドエラー - Plan API無効化メソッドが存在しない

**問題の詳細:**
```
> Task :miningtracker-bukkit:compileJava FAILED
エラー: シンボルを見つけられません
    extensionService.invalidate(this, playerUUID)
                    ^
  シンボル:   メソッド invalidate(MiningTrackerExtension,UUID)
  場所: タイプExtensionServiceの変数 extensionService
```

**根本原因:**
- v2.4.1でPlan統計のリアルタイム更新機能を実装
- `ExtensionService.invalidate()`メソッドを使用
- しかし、Plan API 5.6にはこのメソッドが存在しない
- ビルドが失敗する状態になっていた

**解決方法:**
以下のコードを削除して、Planの自動更新メカニズムに依存する方式に変更:
1. `MiningTrackerExtension.invalidatePlayerCache(UUID)` メソッド
2. `MiningTrackerExtension.invalidateServerCache()` メソッド
3. `MiningTracker.planExtension` フィールド
4. `MiningTracker.getPlanExtension()` ゲッター
5. `DataManager` での Plan無効化呼び出し

## 📊 変更内容

### 削除されたコード

#### MiningTrackerExtension.java
```java
// 削除
public void invalidatePlayerCache(UUID playerUUID) {
    try {
        ExtensionService extensionService = ExtensionService.getInstance();
        extensionService.invalidate(this, playerUUID);
    } catch (Exception e) {
        // Planが無効な場合は無視
    }
}

public void invalidateServerCache() {
    try {
        ExtensionService extensionService = ExtensionService.getInstance();
        extensionService.invalidate(this);
    } catch (Exception e) {
        // Planが無効な場合は無視
    }
}
```

#### MiningTracker.java
```java
// 削除
private MiningTrackerExtension planExtension;

public MiningTrackerExtension getPlanExtension() {
    return planExtension;
}

// 変更前
planExtension = new MiningTrackerExtension(this);
planExtension.register();

// 変更後（ローカル変数）
MiningTrackerExtension extension = new MiningTrackerExtension(this);
extension.register();
```

#### DataManager.java
```java
// 削除
if (plugin.getPlanExtension() != null) {
    plugin.getPlanExtension().invalidatePlayerCache(playerUUID);
    plugin.getPlanExtension().invalidateServerCache();
}
```

## 🎯 Plan統計の更新について

### Planの自動更新メカニズム

Plan DataExtensionは以下のタイミングで自動的に更新されます:

1. **プレイヤーログイン時**: プレイヤーがサーバーに参加した時
2. **プレイヤーログアウト時**: プレイヤーがサーバーから退出した時
3. **定期スケジュール**: Planの設定で指定された間隔（デフォルト: 数分ごと）
4. **@InvalidateMethodで指定されたメソッド実行時**

### 更新頻度

- **リアルタイム**: プレイヤーログイン/ログアウト時
- **定期更新**: Planの設定による（通常は5-10分間隔）
- **手動更新**: `/plan analyze` コマンドで即座に更新可能

### 表示の遅延について

データベースに保存された採掘統計は、Planの次回更新時に反映されます。通常、数分以内に更新されますが、即座には反映されない場合があります。

即座に更新を確認したい場合は:
```
/plan analyze
```
コマンドを実行してください。

## 💡 技術的詳細

### Plan API 5.6の制約

Plan API 5.6では、以下の理由によりキャッシュの手動無効化はサポートされていません:

1. `ExtensionService.invalidate()` メソッドが存在しない
2. DataExtensionは自動的にキャッシュ管理される
3. 開発者は`@InvalidateMethod`アノテーションで更新タイミングを宣言するのみ

### @InvalidateMethodアノテーション

MiningTrackerExtensionでは以下の`@InvalidateMethod`を宣言しています:

```java
@InvalidateMethod("playerLeave")
@InvalidateMethod("serverShutdown")
@InvalidateMethod("extensionRefresh")
@InvalidateMethod("serverLoad")
```

これらのメソッドが実行されたとき、Planは自動的にキャッシュを無効化してデータを再取得します。

## 📦 ビルド成果物

### ダウンロード

- `MiningTracker-Bukkit-2.4.2.jar` - Bukkit/Paper/Spigotサーバー用
- `MiningTracker-Bungee-2.4.2.jar` - Bungeecordプロキシ用

### ビルド要件

- Java 21以上
- Gradle 8.9以上

### ビルド方法

```bash
./gradlew clean build
```

## 🔄 アップグレード手順

### v2.4.1からのアップグレード

1. サーバーを停止
2. 古いJARファイルを削除:
   - `plugins/MiningTracker-2.4.1.jar`
3. 新しいJARファイルをインストール:
   - `plugins/MiningTracker-2.4.2.jar`
4. サーバーを起動

**設定ファイルの変更は不要です。**

### データベース

データベーススキーマに変更はありません。既存のデータはそのまま使用できます。

## ⚠️ 既知の問題

### Planでの表示遅延

採掘データがPlanに反映されるまで数分かかる場合があります。これはPlanの自動更新スケジュールによるものです。

**回避方法:**
- `/plan analyze` コマンドで手動更新
- Planの設定ファイルで更新間隔を短縮（推奨: 5分）

## 🔗 関連情報

### 前バージョン

- [v2.4.1リリースノート](RELEASE_NOTES_v2.4.1.md) - SQL構文エラー修正、Planリアルタイム更新実装（ビルドエラーあり）
- [v2.4.0リリースノート](RELEASE_NOTES_v2.4.0.md) - SLF4J完全修正（slf4j-jdk14）

### ドキュメント

- [README.md](README.md) - プラグイン概要とインストール手順
- [VERSION.md](VERSION.md) - バージョン履歴の詳細
- [CHANGELOG.md](CHANGELOG.md) - 変更履歴
- [PLAN_INTEGRATION.md](PLAN_INTEGRATION.md) - Plan連携ガイド

## 👥 貢献者

このリリースは以下の方々の協力により実現しました:

- [@kubota6646](https://github.com/kubota6646) - プロジェクトメンテナー

## 📝 フィードバック

問題を見つけた場合や改善提案がある場合は、[GitHubのIssues](https://github.com/kubota6646/MiningTracker/issues)でお知らせください。

---

**ダウンロード**: [GitHub Releases](https://github.com/kubota6646/MiningTracker/releases/tag/v2.4.2)

**前バージョン**: [v2.4.1](https://github.com/kubota6646/MiningTracker/releases/tag/v2.4.1)

**次の予定**: v2.4.3 - さらなる改善と最適化
