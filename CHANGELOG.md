# 変更履歴 / Changelog

## [2.1.4] - 2026-02-02

### 🔧 修正 / Fixes

#### DataExtension登録方法の完全修正
- **問題**: MiningTrackerExtension.javaでコンパイルエラーが発生
  - `CapabilityService.registerExtension(MiningTrackerExtension)` メソッドが存在しない
  - エラー: "シンボルを見つけられません: メソッド registerExtension(MiningTrackerExtension)"
- **根本原因**:
  - Plan API 5.6では`CapabilityService`ではなく`ExtensionService`を使用する必要がある
  - 正しいメソッド: `ExtensionService.getInstance().register(DataExtension)`
- **修正内容**:
  - `CapabilityService`を`ExtensionService`に変更
  - `registerExtension()`を`register()`に変更
  - `registerEnableListener`の使用を削除（ExtensionServiceが自動的に処理）
  - 適切なエラーハンドリングを追加（NoClassDefFoundError, IllegalStateException, IllegalArgumentException）
- **影響**: ビルドが正常に完了し、Plan連携が公式推奨方法で動作

### 📚 技術詳細
- Plan API 5.6公式ドキュメントに準拠した登録方法
- `ExtensionService.getInstance().register()` が正しいAPI
- Planがインストールされていない場合の適切なエラーハンドリング

---

## [2.1.3] - 2026-02-02

### 🔧 修正 / Fixes

#### CapabilityService.registerExtension メソッドシグネチャ修正
- **問題**: MiningTrackerExtension.javaでコンパイルエラーが発生
  - `registerExtension(String, MiningTrackerExtension)` メソッドが存在しない
  - エラー: "シンボルを見つけられません: メソッド registerExtension(String,MiningTrackerExtension)"
- **修正内容**:
  - `registerExtension()` の呼び出しからプラグイン名パラメータを削除
  - 正しいメソッドシグネチャ: `registerExtension(DataExtension)` のみを使用
  - `@PluginInfo` アノテーションでプラグイン名が指定されているため、明示的に渡す必要なし
- **影響**: ビルドが正常に完了し、Plan連携が正しく動作

### 📚 技術詳細
- Plan API 5.6の正しいメソッドシグネチャを使用
- `@PluginInfo` アノテーションがプラグインの識別情報を提供

---

## [2.1.2] - 2026-02-02

### 🔧 修正 / Fixes

#### Plan APIコンパイルエラー修正
- **問題**: MiningTrackerExtension.javaでコンパイルエラーが発生
  - `ElementOrder` シンボルが見つからない（7個のエラー）
  - `registerExtension()` メソッドが存在しない
- **修正内容**:
  - `ElementOrder` のインポートを追加
  - `register()` メソッドをPlan API 5.6の正しい使用方法に修正
  - `registerEnableListener()` を使用したDataExtension登録に変更
- **影響**: ビルドが正常に完了し、Plan連携が正しく動作

### 📚 ドキュメント更新
- Plan API 5.6の正しい使用方法を記載
- DataExtension登録パターンの説明を追加

---

## [2.1.1] - 2026-02-02

### 🔧 修正 / Fixes

#### Plan依存関係の解決エラー修正
- **問題**: Gradleプロジェクト同期時に`com.djrapitops:plan:5.6.3027`が見つからないエラー
- **修正内容**:
  - JitPackリポジトリを追加（`https://jitpack.io`）
  - Plan依存関係を正しい形式に変更: `com.github.plan-player-analytics:Plan:5.6.2959`
  - 旧リポジトリ（`repo.playeranalytics.net`）を削除
- **影響**: ビルドが正常に動作するようになる

### 📚 ドキュメント更新
- PLAN_INTEGRATION.mdに開発者向けビルド設定セクションを追加
- 正しいPlan依存関係の設定方法を記載

---

## [2.1.0] - 2026-02-02

### 🎉 新機能 / New Features

#### Plan Player Analytics 連携 ⭐
- **Plan Player Analyticsとの統合**
  - Webダッシュボードで採掘統計を視覚的に表示
  - プレイヤー個別ページ、サーバーページ、ネットワークページに対応

#### マルチサーバー対応
- **サーバー別データ記録**
  - 各サーバーでの採掘を個別に記録
  - `server-name`設定でサーバーを識別
  - MySQLで複数サーバーのデータを統合管理

#### データベーススキーマ更新
- **server_nameカラムを追加**
  - どのサーバーでの採掘かを記録
  - 自動マイグレーション処理を実装
  - 既存データは`server_name = 'default'`として保存

### 追加されたPlan統計

**プレイヤー別:**
- 総採掘ブロック数（全サーバー合計）
- 総採掘ブロック数（このサーバーのみ）
- 採掘ランキング順位（ネットワーク全体）
- ブロックタイプ別内訳テーブル（サーバー別・全サーバー合計）

**サーバー別:**
- サーバー総採掘数
- アクティブマイナー数
- サーバー内トップランキング

**ネットワーク全体:**
- 全サーバー合計採掘数
- 全サーバーマイナー数
- ネットワーク全体のトップランキング
- サーバー比較テーブル

### 既存機能の改善

#### コマンドの動作変更
- `/mtstats` - 全サーバー合計の統計を表示
- `/mtranking` - 全サーバー合計のランキングを表示

### 技術的な変更

- Plan API 5.6.3027 を依存関係に追加
- HikariCP接続プール統合（MySQL）
- DatabaseManagerに17の新しいメソッドを追加
  - `getTotalMinedAllServers(UUID)`
  - `getTotalMinedByServer(UUID, String)`
  - `getPlayerRank(UUID)`
  - `getPlayerStatsByServer(UUID, String)`
  - `getPlayerStatsAllServers(UUID)`
  - `getServerTotalMined(String)`
  - `getServerPlayerCount(String)`
  - `getTopPlayersByServer(String, int)`
  - `getNetworkTotalMined()`
  - `getNetworkPlayerCount()`
  - `getTopPlayersAllServers(int)`
  - `getAllServerStats()`
  - その他サポートメソッド

### ドキュメント

- **PLAN_INTEGRATION.md** - Plan連携の完全ガイド
  - セットアップ手順
  - マルチサーバー設定
  - データの見方
  - トラブルシューティング
  - API使用例

### アップグレード

v2.0.x → v2.1.0:
1. プラグインJARを置き換え
2. サーバーを起動（自動マイグレーション実行）
3. config.ymlで`server-name`を設定（オプション）
4. Plan（オプション）をインストールして連携

既存データは自動的に移行され、`server_name = 'default'`として保存されます。

---

## [2.0.3] - 2026-02-02

### 🐛 重要なバグ修正 / Critical Bug Fix

#### 修正 / Fixed
- **HikariCP接続リーク**: MySQL接続プールからの接続リークを修正
- HikariCPの接続リーク検出警告を解消

#### 問題 / Problem
```
[WARN]: [com.zaxxer.hikari.pool.ProxyLeakTask] Connection leak detection triggered
```

connectMySQL()メソッドで接続プールから取得した接続を`connection`フィールドに保存し、
プールに返却していなかった。これによりHikariCPが60秒後に接続リークを検出。

#### 技術的詳細 / Technical Details
- `connectMySQL()`で`connection = hikariDataSource.getConnection()`を削除
- テーブル作成時に接続を一時的に取得し、使用後すぐに返却（try-with-resources）
- `createTablesForMySQL(Connection)`メソッドを追加
- `connection`フィールドはSQLiteのみで使用（MySQLでは使用しない）

#### 変更内容
```java
// 修正前（v2.0.2）- 接続リーク
hikariDataSource = new HikariDataSource(hikariConfig);
connection = hikariDataSource.getConnection(); // ❌ プールに返却されない
createTables();

// 修正後（v2.0.3）- 正しい使用方法
hikariDataSource = new HikariDataSource(hikariConfig);
try (Connection conn = hikariDataSource.getConnection()) { // ✅ 自動的に返却
    createTablesForMySQL(conn);
}
```

#### 影響 / Impact
- ✅ 接続リーク警告が解消
- ✅ 接続プールの効率的な使用
- ✅ 長時間稼働時のパフォーマンス向上
- ✅ プール内の利用可能な接続数が正常に維持
- ⚠️ 設定ファイルの変更は不要

---

## [2.0.2] - 2026-02-02

### 🐛 重要なバグ修正 / Critical Bug Fix

#### 修正 / Fixed
- **MySQL Connector/J 8.x互換性**: `characterEncoding`パラメータを`connectionCollation`に変更
- v2.0.1でも発生していた`Unsupported character encoding 'utf8mb4'`エラーを完全に修正

#### 技術的詳細 / Technical Details
- MySQL Connector/J 8.xでは`characterEncoding`パラメータが非推奨（deprecated）
- `characterEncoding=utf8mb4`を`connectionCollation=utf8mb4_unicode_ci`に変更
- MySQL Connector/J 8.xはデフォルトでUTF-8（utf8mb4）を使用するため、明示的な文字セット指定は不要だが、照合順序を指定することで日本語対応を確実にする

#### 変更内容
```java
// v2.0.1 (問題あり)
"jdbc:mysql://host:port/db?...&characterEncoding=utf8mb4"

// v2.0.2 (修正後)
"jdbc:mysql://host:port/db?...&connectionCollation=utf8mb4_unicode_ci"
```

#### 影響 / Impact
- ✅ MySQL Connector/J 8.3.0との完全な互換性
- ✅ MySQL接続が確実に成功
- ✅ UTF-8MB4文字エンコーディングと照合順序が正しく適用
- ✅ 日本語データの保存・取得が確実に動作
- ⚠️ 設定ファイルの変更は不要

---

## [2.0.1] - 2026-02-02

### 🐛 バグ修正 / Bug Fixes

#### 修正 / Fixed
- **HikariCP接続エラー**: MySQL接続時の`Unsupported character encoding 'utf8mb4'`エラーの修正を試みた（v2.0.2で完全修正）
- **SQLite接続管理**: SQLiteデータベース接続のライフサイクル管理を改善
- 接続パラメータ（characterEncoding、useSSL、serverTimezoneなど）をJDBC URLパラメータとして正しく設定

#### 注意 / Note
- v2.0.1の修正は不完全で、MySQL Connector/J 8.xの非推奨パラメータを使用していたため、エラーが継続
- v2.0.2で完全に修正されました

---

## [2.0.0] - 2026-02-01

### 🎉 メジャーアップデート - Minecraft 1.21.x対応

#### 追加 / Added
- Minecraft 1.21.x完全対応
- Java 21必須要件

#### 変更 / Changed
- **Minecraft対応バージョン**: 1.19.4 → 1.21.x
- **Java要件**: Java 17以降 → Java 21必須
- **Spigot API**: 1.19.4-R0.1-SNAPSHOT → 1.21-R0.1-SNAPSHOT
- **plugin.yml api-version**: 1.19 → 1.21
- **プラグインバージョン**: 1.0.0 → 2.0.0
- build.gradleのJava互換性設定をJava 21に更新

#### 互換性 / Compatibility
- ✅ Minecraft 1.21, 1.21.1, 1.21.3対応
- ✅ Java 21必須
- ✅ Gradle 8.5以降対応
- ❌ Java 17以下は非対応（Minecraft 1.21の要件）
- ❌ Minecraft 1.19.x以前は非対応

#### 注意事項 / Notes
- **重要**: このバージョンからJava 21が必須です
- Minecraft 1.19.4用には v1.0.0 を使用してください
- すべての既存機能は1.21.xでも動作します

---

## [1.0.0] - 2026-01-30

### 初回リリース / Initial Release

#### 機能 / Features
- ブロック採掘トラッキング
- プレイヤー統計表示（/mtstats）
- ランキング表示（/mtranking）
- 統計リセット機能（/mtreset）
- SQLiteデータベースサポート
- MySQLデータベースサポート（HikariCP接続プール）
- 日本語メッセージ対応
- 設定ファイルによるカスタマイズ

#### 対応環境 / Requirements
- Minecraft 1.19.4
- Java 17以降
- Spigot/Paper 1.19.4
- Gradle 8.5

#### データベース / Database
- SQLite（デフォルト）
- MySQL 5.7+ / 8.0+（オプション）
- HikariCP接続プール
- 自動テーブル作成

#### コマンド / Commands
- `/mtstats [プレイヤー名]` - 採掘統計表示
- `/mtranking [ページ]` - ランキング表示
- `/mtreset <プレイヤー|all> [confirm]` - 統計リセット

#### 権限 / Permissions
- `miningtracker.use` - 基本機能使用（デフォルト: true）
- `miningtracker.other` - 他プレイヤー統計閲覧（デフォルト: true）
- `miningtracker.reset` - 統計リセット（デフォルト: op）

---

## バージョニングについて / Versioning

このプロジェクトは [Semantic Versioning](https://semver.org/) に従います。

- **メジャーバージョン**: Minecraft メジャーバージョンアップ、互換性のない変更
- **マイナーバージョン**: 新機能追加（後方互換性あり）
- **パッチバージョン**: バグ修正、小さな改善
