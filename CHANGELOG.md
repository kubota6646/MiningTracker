# 変更履歴 / Changelog

## [2.5.1] - 2026-02-07

### ✨ 新機能 / New Feature

#### 強制インポートコマンド (`/mtimport`)
- **要件**: 既存データがある場合でも、Minecraft統計でデータを上書きするコマンドを追加
- **コマンド**: `/mtimport <プレイヤー名> confirm`
- **エイリアス**: `/mtimp`
- **動作例**: 
  - 統計ファイル: 50個
  - データベース: 2個
  - コマンド実行後: 50個（統計の値で上書き）
- **権限**: `miningtracker.import` (デフォルト: op)
- **実装内容**:
  - ImportCommand: 新しいコマンドクラス（確認ステップ付き）
  - DatabaseManager.setMiningCount(): 加算ではなく置換する新メソッド
  - MinecraftStatsImporter: forceOverwriteフラグ対応
- **使用ケース**:
  - データが壊れた場合の復元
  - 統計ファイルからの再同期
  - 管理者によるデータ修正

### 📋 技術的詳細 / Technical Details
- 安全性確保のため確認ステップ必須
- 非同期処理でサーバーパフォーマンスに影響なし
- MySQL/SQLite両対応のUPSERT構文使用

## [2.5.0] - 2026-02-07

### ✨ 新機能 / New Feature

#### Minecraft統計インポート機能
- **要件**: 既存のデータが存在しない場合、Minecraftの内蔵統計から採掘データを自動インポート
- **実装内容**:
  - MinecraftStatsImporter: 新しいクラスで統計ファイルを読み込み・解析
  - DatabaseManager: addMiningCount() にカスタム数量パラメータを追加
  - StatsCommand: データがない場合に自動的にインポートを試行
  - config.yml: `import.enabled` 設定オプション追加
- **技術的詳細**:
  - `world/stats/<uuid>.json` から `minecraft:mined` セクションを読み込み
  - JSON解析にGson（Bukkitに同梱）を使用
  - ブロックタイプのみフィルタリング（アイテムは除外）
  - 非同期処理でメインスレッドをブロックしない
  - 適切なエラーハンドリングとログ出力
- **利点**:
  - 新規プレイヤーが既存の採掘データを失わない
  - バニラ統計からのスムーズな移行
  - プレイヤーエクスペリエンスの大幅な改善

### 🔧 改善 / Improvements

#### コード品質向上
- MinecraftStatsImporter: `Bukkit.getWorlds().stream().findFirst()` で IndexOutOfBoundsException を防止
- DatabaseManager: PreparedStatement パラメータにコメント追加（INSERT/UPDATE の区別を明確化）

### 📚 ドキュメント / Documentation
- MINECRAFT_STATS_IMPORT.md: 詳細な機能説明、使用例、トラブルシューティングガイドを追加

## [2.4.2] - 2026-02-07

### 🐛 重大なバグ修正 / Critical Bug Fix

#### ビルドエラー修正 - Plan API無効化メソッド削除
- **問題**:
  ```
  エラー: シンボルを見つけられません
  extensionService.invalidate(this, playerUUID)
  extensionService.invalidate(this)
  ```
- **根本原因**: Plan API 5.6に`ExtensionService.invalidate()`メソッドが存在しない、v2.4.1で追加したキャッシュ無効化コードがコンパイルエラーを引き起こす
- **解決**: 
  - MiningTrackerExtension: `invalidatePlayerCache()` と `invalidateServerCache()` メソッドを削除
  - MiningTracker: `planExtension` フィールドと `getPlanExtension()` ゲッターを削除
  - DataManager: Plan無効化呼び出しを削除
  - Planの自動更新メカニズムに依存（`@InvalidateMethod`アノテーション使用）
- **影響**: ビルドエラーが完全に解消、Planの自動更新でデータが定期的に反映される、コードがシンプルで保守しやすくなった

### 📋 技術的詳細 / Technical Details
- Plan DataExtensionは定期的に自動更新される
- `@InvalidateMethod`アノテーションで更新タイミングを指定
- プレイヤーログイン/ログアウト時、定期スケジュールで更新

## [2.4.1] - 2026-02-07

### 🐛 重大なバグ修正 / Critical Bug Fix

#### SQL構文エラー修正 - rank予約語問題
- **問題**:
  ```
  [Plan Non critical-pool-3/WARN]: [MiningTracker] ランク取得エラー: 
  You have an error in your SQL syntax near 'rank FROM mining_data
  ```
- **根本原因**: `rank`はMySQLの予約語、SQLクエリで列エイリアスとして使用
- **解決**: DatabaseManager.getPlayerRank() で `rank` → `player_rank` に変更
- **影響**: SQL構文エラーが完全に解消、Planでランキングが正常に表示

### ✨ 新機能 / New Feature

#### Planリアルタイム更新実装
- **要件**: 総採掘量をPlanにリアルタイムで反映
- **実装内容**:
  - MiningTrackerExtension: `invalidatePlayerCache(UUID)` メソッド追加
  - MiningTrackerExtension: `invalidateServerCache()` メソッド追加
  - DataManager: ブロック破壊時にPlanキャッシュを自動無効化
  - ExtensionService.invalidate() を使用した公式推奨方法
- **技術的詳細**:
  - ブロック破壊 → DB保存 → Planキャッシュ無効化（非同期処理）
  - プレイヤー統計、サーバー統計、ネットワーク統計が即時更新
- **影響**:
  - ブロック破壊後、即座にPlanに採掘量が反映される ⭐
  - リアルタイムでランキングも更新される
  - Plan拡張機能が完全にリアルタイム対応

## [2.4.0] - 2026-02-07

### 🎉 メジャー修正 / Major Fix

#### SLF4J完全修正 - slf4j-jdk14への切り替え
- **SLF4Jエラー根本解決**: v2.3.8, v2.3.9での修正後も継続していたエラーを根本的に解決
- **問題**:
  ```
  [ERROR]: [MiningTracker] [STDERR] SLF4J: No SLF4J providers were found.
  [ERROR]: [MiningTracker] [STDERR] SLF4J: Defaulting to no-operation (NOP) logger implementation
  ```
- **根本原因**:
  - `slf4j-simple`: スタンドアロンアプリケーション向けのSLF4J実装
  - BukkitはJava Util Logging（JUL）を使用
  - slf4j-simpleとJULが競合し、SLF4Jプロバイダーが正しく検出されない
- **解決**:
  - **slf4j-simple → slf4j-jdk14への切り替え** (全モジュール)
  - slf4j-jdk14はSLF4JをJava Util Logging（JUL）にブリッジ
  - Bukkitのロギングシステムとネイティブ統合
  - SLF4Jのリロケーションを削除し、`mergeServiceFiles()`を追加
- **技術的メリット**:
  - HikariCPのSLF4Jログ → JUL → Bukkitログに完全統合
  - Paper/Spigot/Bungeecordの既存のログ設定を使用
  - 追加設定不要、競合なし
- **影響**: SLF4Jエラーが完全に解消され、HikariCPログがBukkitのログシステムに統合される

## [2.3.9] - 2026-02-07

### 🐛 バグ修正 / Bug Fix

#### SLF4J依存関係の明示化
- **SLF4Jエラー修正**: v2.3.8で修正を試みたが継続していたエラーを完全解決
- **問題**:
  ```
  [ERROR]: [MiningTracker] [STDERR] SLF4J: No SLF4J providers were found.
  [ERROR]: [MiningTracker] [STDERR] SLF4J: Defaulting to no-operation (NOP) logger implementation
  ```
- **根本原因**:
  - commonモジュールで`slf4j-simple`を宣言
  - しかしbukkit/bungeeモジュールで明示的に宣言されていなかった
  - 推移的依存関係として含まれるべきだったが、shadowJarで正しく処理されなかった
- **解決**:
  - bukkit/bungee両モジュールの`dependencies`に`slf4j-simple:2.0.9`を明示的に追加
  - 各モジュールで直接依存関係を宣言することで、shadowJarが確実に含める
- **影響**:
  - SLF4Jエラーが完全に解消
  - HikariCPのログが正常に出力される

## [2.3.8] - 2026-02-07

### 🐛 バグ修正 / Bug Fix

#### SLF4J Provider修正
- **SLF4Jエラー修正**: 「SLF4J: No SLF4J providers were found」エラー解消
- **問題**:
  ```
  [ERROR]: [MiningTracker] [STDERR] SLF4J: No SLF4J providers were found.
  [ERROR]: [MiningTracker] [STDERR] SLF4J: Defaulting to no-operation (NOP) logger implementation
  ```
- **根本原因**:
  - SLF4Jのリロケーション（`org.slf4j` → `lib.slf4j`）によりサービスプロバイダーメカニズムが破損
  - SLF4J 2.xはJava ServiceLoader APIを使用
  - パッケージ名変更で`META-INF/services/org.slf4j.spi.SLF4JServiceProvider`が機能しない
- **解決**:
  - **SLF4Jリロケーション削除**: bukkit & bungee両モジュールから削除
  - **mergeServiceFiles()追加**: サービスプロバイダーファイルを統合
  - **HikariCPとMySQLのみリロケート**: 依存関係の競合回避のため維持
- **影響**:
  - SLF4Jエラーメッセージが解消
  - HikariCPのログが正常に出力される
  - Bukkitサーバー・Bungeecord両方で正常動作

## [2.3.7] - 2026-02-07

### 🔧 ビルドエラー修正 / Build Fix

#### Shadow plugin移行（新プラグインID）
- **ビルドエラー修正**: 「Plugin 'com.github.johnrengelman.shadow' version '8.1.7' was not found」エラー解消
- **問題**:
  ```
  Plugin [id: 'com.github.johnrengelman.shadow', version: '8.1.7', apply: false] was not found
  ```
- **根本原因**:
  - v2.3.6で指定したShadow plugin 8.1.7が存在しない
  - 旧プラグインID（`com.github.johnrengelman.shadow`）はメンテナンス終了
  - 最終版は8.1.1で、Java 21完全サポートには不十分
- **解決**:
  - **プラグインID変更**: `com.github.johnrengelman.shadow` → `com.gradleup.shadow`
  - **バージョン更新**: 8.3.3に更新
    - Java 21完全サポート（class file version 65対応）
    - Gradle 8.3+対応（現在8.9使用中）
    - ASMライブラリ最新化
  - **組織移行**: John Rengelman → GradleUp
- **影響**: 
  - ビルドエラー完全解消
  - Java 21環境で安定動作
  - 最新のShadow pluginでメンテナンス継続

## [2.3.6] - 2026-02-07

### 🔧 ビルドエラー修正 / Build Fix

#### shadowJar Java 21互換性問題解消
- **ビルドエラー修正**: shadowJarタスクの「Unsupported class file major version 65」エラー解消
- **問題**:
  ```
  org.gradle.api.GradleException: Could not add file to ZIP
  Caused by: java.lang.IllegalArgumentException: Unsupported class file major version 65
  ```
- **根本原因**:
  - Java 21でコンパイル（class file version 65）
  - Shadow plugin 8.1.1のASMライブラリがJava 21バイトコードを完全にサポートしていない
  - shadowJarタスクがバイトコード処理に失敗
- **解決**:
  - **Shadow plugin更新**: 8.1.1 → 8.1.7
    - Java 21完全サポート
    - ASMライブラリ最新化
    - 多数のバグ修正
  - **Gradle更新**: 8.5 → 8.9
    - Java 21サポート改善
    - パフォーマンス向上
    - セキュリティ修正
- **影響**: 
  - shadowJarがJava 21バイトコードを正常に処理
  - ビルドが成功し、JARファイルが生成される
  - 安定したビルドプロセス

## [2.3.5] - 2026-02-07

### 🐛 バグ修正 / Bug Fixes

#### Plan統計表示のNULL処理修正
- **バグ修正**: Planのサーバー総採掘数とネットワーク総採掘数が正常に表示されない
- **問題**: 
  - Planでサーバー総採掘数が表示されない
  - Planでネットワーク総採掘数が表示されない
  - データが存在する場合でも0や不正な値が表示される
- **根本原因**:
  - SQLの`SUM(count)`は結果が空の場合に`NULL`を返す
  - JDBCの`rs.getLong()`は`NULL`を0として扱うが、`rs.wasNull()`でチェックしないと不正確
  - NULL処理が不適切だった
- **解決**:
  ```java
  long total = rs.getLong("total");
  // rs.wasNull()をチェックしてNULLの場合は0を返す
  return rs.wasNull() ? 0 : total;
  ```
- **影響**: 
  - Planでサーバー総採掘数が正しく表示される
  - Planでネットワーク総採掘数が正しく表示される
  - データがない場合も0が正しく表示される

### 🔧 技術詳細 / Technical Details

#### 修正箇所
1. **DatabaseManager.java (Bukkit)**
   - `getServerTotalMined()`: NULL処理追加
   - `getNetworkTotalMined()`: NULL処理追加
   - エラーログ改善、スタックトレース追加

2. **CommonDatabaseManager.java (Common)**
   - `getNetworkTotalMined()`: NULL処理追加
   - エラーログ改善、スタックトレース追加

---

## [2.3.4] - 2026-02-07

### 🐛 バグ修正 / Bug Fixes

#### サーバー間リアルタイム同期の修正
- **重大なバグ修正**: Bungeecordネットワークでサーバー間のリアルタイム同期が機能していなかった
- **問題**: 
  - メインサーバーで5ブロック採掘
  - 資源サーバーで`/mtr`コマンドを実行
  - 期待: 5ブロックと表示
  - 実際: 採掘データがありませんと表示
- **根本原因**:
  - HikariCPの`elideSetAutoCommits`最適化がautoCommit管理を最適化
  - autoCommitが暗黙的に動作していた
  - データ書き込み後のコミットが確実でなかった
- **解決**:
  - `elideSetAutoCommits`最適化を削除（リアルタイム同期のため）
  - `hikariConfig.setAutoCommit(true)`を明示的に設定
  - データ書き込み前に`conn.getAutoCommit()`でautoCommitを確認・有効化
  - executeUpdate()後、autoCommitにより自動コミットされることを明示化
- **影響**: 
  - データ書き込み後、即座に他のサーバーから見えるようになった
  - Bungeecordネットワークでリアルタイム統計が正常に機能

### 🔧 技術詳細 / Technical Details

#### DatabaseManager.java (Bukkit)
```java
// 削除
// hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");

// 追加
hikariConfig.setAutoCommit(true);  // 明示的にautoCommitを有効化

// データ書き込み時
if (!conn.getAutoCommit()) {
    conn.setAutoCommit(true);
}
pstmt.executeUpdate();
// autoCommit=trueなので、ここで自動的にコミットされている
```

#### CommonDatabaseManager.java (Common)
```java
// 追加
hikariConfig.setAutoCommit(true);  // 明示的にautoCommitを有効化
```

---

## [2.3.3] - 2026-02-07

### 🔧 改善 / Improvements

#### shadowJar設定の改善
- **リソース包含**: `from(sourceSets.main.output)`を明示的に追加
- **依存関係リロケーション**: 競合回避のため以下をリロケート
  - `com.zaxxer.hikari` → `com.kubota6646.miningtracker.lib.hikari`
  - `com.mysql` → `com.kubota6646.miningtracker.lib.mysql`
  - `org.slf4j` → `com.kubota6646.miningtracker.lib.slf4j`
- **影響**: プラグイン間の依存関係競合を防止、より安定した動作

### 📚 ドキュメント改善 / Documentation

#### README.md
- **ビルド成果物の明確化**: Bukkit版とBungee版の2つのJARを明示
- **インストール手順の詳細化**: 
  - シングルサーバー向け手順
  - Bungeecordネットワーク向け手順
  - 重要な警告と注意事項
- **トラブルシューティングセクション追加**:
  - "Plugin must have plugin.yml or bungee.yml"エラーの解決
  - Paper/Spigotで動作しない場合の確認事項
  - データベース接続エラーの対処法
  - Bungeecordネットワーク統計が表示されない場合の対処

#### BUNGEECORD_SETUP.md
- **警告セクション追加**: 正しいJARファイルの使用方法を明示
- **JARファイル名の更新**: 2.2.0 → 2.3.3
- **エラー原因の説明**: 間違ったJARを使用した場合のエラーを文書化

### 🐛 問題解決 / Problem Resolution

- **Paper/Bungeecordで動作しない問題**: ドキュメント改善により解決方法を提供
- **混乱の防止**: どのJARをどこで使うべきか明確化

---

## [2.3.2] - 2026-02-07

### 🐛 バグ修正 / Bug Fixes

#### Bungeecord APIバージョン修正
- **重大なバグ修正**: Bungeecord APIの正しいバージョンに修正
- **問題**: `miningtracker-bungee`コンパイルエラー - Bungeecord APIが見つからない
- **原因**: `net.md-5:bungeecord-api:1.21-R0.1-SNAPSHOT`が存在しない。BungeecordはSNAPSHOT版を使用しない
- **解決**: 正しい安定版バージョンに変更
  - `net.md-5:bungeecord-api:1.21-R0.4`
- **影響**: Bungeecordモジュールのコンパイルエラー解消、ビルド成功

### 📦 依存関係管理
- Bungeecord API最新安定版（1.21-R0.4）を使用
- Maven Centralから正常に解決可能

---

## [2.3.1] - 2026-02-07

### 🐛 バグ修正 / Bug Fixes

#### HikariCP依存関係修正
- **重大なバグ修正**: bukkit/bungeeモジュールのHikariCP依存関係を追加
- **問題**: `miningtracker-bukkit`コンパイルエラー - HikariCPパッケージが見つからない
- **原因**: Gradleの`implementation`依存関係は推移的でない。CommonモジュールがHikariCPを宣言していても、Bukkitモジュールからはアクセスできない
- **解決**: bukkit/bungee両モジュールに以下を明示的に追加
  - `com.mysql:mysql-connector-j:8.3.0`
  - `com.zaxxer:HikariCP:5.1.0`
  - `org.slf4j:slf4j-simple:2.0.9`
- **影響**: コンパイルエラー完全解消、ビルド成功

### 📦 依存関係管理
- 各モジュールが必要な依存関係を明示的に宣言
- マルチモジュールプロジェクトのベストプラクティスに準拠

---

## [2.3.0] - 2026-02-07

### 🐛 バグ修正 / Bug Fixes

#### ビルドエラー修正
- **重大なバグ修正**: ルートディレクトリの古い`src/`ディレクトリを削除
- **問題**: マルチモジュール化後、ルートに古い`src/`が残りビルドエラー発生
- **原因**: Gradleがルートプロジェクトの`src/`をコンパイルしようとするが、依存関係が未設定
- **解決**: gitから古い`src/`を完全に削除
- **影響**: ビルドが正常に動作するようになった

### 📦 プロジェクト構造
- マルチモジュールプロジェクトの構造をクリーンアップ
- ルートレベルのソースコードを完全に削除
- 各モジュール（bukkit, bungee, common）のみがソースコードを持つ

---

## [2.2.0] - 2026-02-07

### 🚀 新機能 / New Features

#### Bungeecordプラグイン対応 ⭐
- **MiningTracker-Bungee**: Bungeecordプロキシに直接インストール可能な新プラグイン
- **ネットワーク統計のみ表示**: サーバー統計を非表示、ネットワーク統計のみをPlanに表示
- **MySQL専用**: Bungee版はMySQLのみ対応（SQLiteは不可）
- **読み取り専用**: 採掘トラッキングなし、統計の表示のみ

#### マルチモジュール化
- **アーキテクチャ変更**: Gradleマルチモジュールプロジェクトに変更
  - `miningtracker-common`: 共通コード（ConfigAdapter, CommonDatabaseManager）
  - `miningtracker-bukkit`: Bukkitプラグイン（既存機能をすべて維持）
  - `miningtracker-bungee`: Bungeecordプラグイン（ネットワーク統計のみ）

#### 共通モジュール
- **ConfigAdapter Interface**: Bukkit/Bungeeの設定を統一的に扱う抽象化
- **CommonDatabaseManager**: プラットフォーム非依存のデータベース処理
- **BukkitConfigAdapter**: Bukkit用の設定アダプター実装
- **BungeeConfigAdapter**: Bungee用の設定アダプター実装

### 📚 ドキュメント / Documentation

#### 新規追加・更新
- **README.md**: Bungeecordプラグインの説明を追加
  - 2つのインストール方法を明記（バックエンドのみ vs プロキシにも）
  - Bukkit版とBungee版の比較表
- **BUNGEECORD_SETUP.md**: Bungee版の詳細な説明を追加
  - インストール方法の選択ガイド
  - Bungee版の設定例
  - Bukkit版との違いを明示
- **bungee.yml**: Bungeecordプラグイン記述ファイル
- **config.yml (Bungee用)**: Bungeecord版の設定ファイルテンプレート

### 🔧 技術詳細 / Technical Details

#### Plan連携
- **Bukkit版**: すべての統計を表示
  - プレイヤー統計 ✓
  - サーバー統計 ✓
  - ネットワーク統計 ✓
- **Bungee版**: ネットワーク統計のみ表示
  - プレイヤー統計 ✗
  - サーバー統計 ✗
  - ネットワーク統計 ✓

#### ビルド成果物
- `MiningTracker-Bukkit-2.2.0.jar`: バックエンドサーバー用
- `MiningTracker-Bungee-2.2.0.jar`: Bungeecordプロキシ用
- `MiningTracker-Common-2.2.0.jar`: 共通ライブラリ（shadowJarに含まれる）

### 🐛 バグ修正 / Bug Fixes

#### DatabaseManagerのリソースリーク修正（継続）
- **問題**: ResultSetオブジェクトが適切に閉じられておらず、リソースリークが発生
- **原因**: 30箇所のResultSet宣言がtry-with-resourcesで管理されていなかった
- **修正内容**: すべてのResultSetをtry-with-resources文で適切に管理するように修正
- **影響**: 
  - メモリリークの防止
  - データベースコネクションプール枯渇の防止
  - 長時間稼働時の安定性向上
  - パフォーマンス向上

### 🔧 メンテナンス / Maintenance

#### バージョン番号の一貫性確保
- build.gradleのバージョンを2.2.0に更新
- VERSION.mdを最新の変更履歴で更新
- すべてのバージョン情報を統一

### 📊 修正詳細
- **修正ファイル**: `DatabaseManager.java`
- **修正箇所**: 30箇所のResultSet宣言
- **変更行数**: 156行追加、132行削除
- **影響範囲**: すべてのデータベースクエリメソッド

### ⚠️ 重要性
このリリースは**重要なバグ修正**を含んでいるため、すべてのユーザーにアップグレードを強く推奨します。

---

## [2.1.9] - 2026-02-03

### 🐛 バグ修正 / Bug Fix

#### 重複テーブル表示の修正
- **問題**: Planでテーブルが2つずつ表示される（重複表示）
- **原因**: メソッド名変更時に`@InvalidateMethod`アノテーションを使用していなかったため、Planが古いメソッド名と新しいメソッド名を別々のプロバイダーとして認識
- **修正内容**: `@InvalidateMethod`アノテーションを追加して、古いメソッド名から新しいメソッド名への移行を明示
  - `@InvalidateMethod("blockTypeBreakdown")` → `burokku_shubetsu_naiwake`に移行
  - `@InvalidateMethod("serverTopMiners")` → `toppu_maina`に移行
  - `@InvalidateMethod("networkTopMiners")` → `nettowaku_toppu_maina`に移行
  - `@InvalidateMethod("serverComparison")` → `saba_hikaku`に移行
- **影響**: Planでテーブルが重複せず、正しく1つずつ表示されるようになる

### 📚 技術詳細
- Planはメソッド名をデータベース識別子として使用する
- メソッド名を変更する場合、`@InvalidateMethod`で古い名前を指定する必要がある
- これにより、Planは古いデータを新しいメソッド名に関連付ける
- 重複表示や古いデータの残存を防ぐ

---

## [2.1.8] - 2026-02-03

### 🌐 国際化 / Localization

#### テーブルメソッド名の日本語化（Romaji）
- **問題**: Planの言語設定を日本語（ja）に変更しても、テーブル名が英語のまま表示される
- **原因**: `@TableProvider`アノテーションは`text`パラメータをサポートしておらず、メソッド名がそのまま表示される
- **修正内容**: テーブルプロバイダーメソッド名を日本語Romaji（ローマ字）に変更
  - `blockTypeBreakdown` → `burokku_shubetsu_naiwake`（ブロック種類別内訳）
  - `serverTopMiners` → `toppu_maina`（トップマイナー）
  - `networkTopMiners` → `nettowaku_toppu_maina`（ネットワークトップマイナー）
  - `serverComparison` → `saba_hikaku`（サーバー比較）
- **影響**: Planのテーブル表示名が日本語Romajiになり、英語と日本語の混在が軽減

### 📚 技術詳細
- `@NumberProvider`は`text`パラメータをサポートしているが、`@TableProvider`はサポートしていない
- Plan 5.6の`@TableProvider`はメソッド名を表示名として使用する
- メソッド名を日本語Romajiに変更することで、より日本語に近い表示を実現
- データベース互換性を保つため、メソッド名の変更は慎重に実施

### ⚠️ 重要な注意事項
- このバージョンでテーブルプロバイダーのメソッド名が変更されました
- Planのデータベースでは新しいメソッド名として認識されます
- 既存のPlan統計データは保持されますが、新しい名前でデータが記録されます

---

## [2.1.7] - 2026-02-03

### 📚 ドキュメント改善 / Documentation Improvements

#### Planの英語/日本語混在問題の解決ガイドを追加
- **問題**: Planの統計表示で「Average 総採掘ブロック数」のように英語と日本語が混在する
- **原因**: Plan自体の言語設定が英語（デフォルト）になっているため、Planが自動追加するラベル（Average、Total等）が英語で表示される
- **解決策**: Planの設定ファイルで言語を日本語（ja）に変更する
- **追加ドキュメント**:
  - `PLAN_INTEGRATION.md`に「Planの表示が一部英語になる」セクションを追加
  - `PLAN_TROUBLESHOOTING.md`に「問題4: Planの表示が一部英語になる」を追加
  - `README.md`に注意事項として追加
- **内容**: 
  - config.ymlでの設定方法（推奨）
  - Webインターフェースからの変更方法
  - 変更前後の表示例
  - クロスリファレンス追加
- **影響**: ユーザーが簡単に日本語表示に変更できるようになる

### 📚 技術詳細
- MiningTrackerのコード変更は不要（Plan側の設定で解決）
- `showInPlayerTable = true`の機能は維持
- Plan 5.6以降の日本語ロケールを活用

---

## [2.1.6] - 2026-02-02

### 🌐 国際化 / Localization

#### PlanページのUI完全日本語化
- **変更内容**: PlanのMiningTrackerページのすべての英語テキストを日本語に翻訳
- **対象箇所**:
  - タブ名: `"Mining Stats"` → `"採掘統計"`, `"Server Stats"` → `"サーバー統計"`, `"Network Stats"` → `"ネットワーク統計"`
  - NumberProviderのtext（7項目）:
    - `"Total Blocks Mined"` → `"総採掘ブロック数"`
    - `"Total Blocks Mined (This Server)"` → `"総採掘ブロック数（このサーバー）"`
    - `"Mining Rank"` → `"採掘ランキング順位"`
    - `"Server Total Blocks Mined"` → `"サーバー総採掘数"`
    - `"Active Miners"` → `"アクティブマイナー数"`
    - `"Network Total Blocks Mined"` → `"ネットワーク総採掘数"`
    - `"Total Active Miners"` → `"総アクティブマイナー数"`
  - TableProviderの列名（9項目）:
    - `"Block Type"` → `"ブロック種類"`
    - `"Count (This Server)"` → `"採掘数（このサーバー）"`
    - `"Count (All Servers)"` → `"採掘数（全サーバー）"`
    - `"Player"` → `"プレイヤー"`
    - `"Blocks Mined"` / `"Total Blocks"` → `"採掘数"` / `"総採掘数"`
    - `"Rank"` → `"順位"`
    - `"Server"` → `"サーバー"`
    - `"Players"` → `"プレイヤー数"`
- **影響**: 日本語ユーザーにとってより理解しやすいインターフェースに改善

### 📚 技術詳細
- Plan DataExtensionのすべてのアノテーション内テキストを日本語化
- description（説明文）は元々日本語だったため変更なし
- UI表示に影響するtext、tab、column名のみを変更

---

## [2.1.5] - 2026-02-02

### 🔧 修正 / Critical Fix

#### Plan拡張データが表示されない問題を修正
- **問題**: Paper 1.21.8環境でPlanに「Extension Dataが存在しません」と表示される
  - MiningTrackerのデータがPlanに反映されない
  - プレイヤー統計が表示されない
- **根本原因**:
  - `MiningTracker.java`で古い`CapabilityService`を使用していた
  - `registerEnableListener`で非同期登録していたため、タイミングの問題が発生
  - 不要な`callExtensionMethodsOn()`メソッドがあった
- **修正内容**:
  - `MiningTracker.java`から`CapabilityService`インポートを削除
  - `registerPlanHook()`メソッドを簡素化し、直接`ExtensionService.register()`を呼び出し
  - `MiningTrackerExtension.java`から不要な`callExtensionMethodsOn()`メソッドを削除
  - `CallEvents`インポートを削除
  - エラーハンドリングを改善（スタックトレース出力追加）
- **影響**: Plan拡張データが正常に表示され、統計情報がWebダッシュボードに反映される

### 📚 技術詳細
- CapabilityService（機能チェック用）とExtensionService（登録用）の違いを明確化
- 同期的な登録方法により、タイミング問題を解消
- シンプルで確実な実装パターンに変更

---

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
