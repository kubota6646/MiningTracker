# 変更履歴 / Changelog

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
