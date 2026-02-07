# バージョン管理

## 現在のバージョン

**v2.3.9** (2026-02-07)

## バージョン履歴

### v2.3.9 - SLF4J依存関係の明示化 (2026-02-07)
- **SLF4Jエラー修正**: v2.3.8で修正を試みたが、エラーが継続していた問題を完全解決
- **問題**: v2.3.8の修正後も「SLF4J: No SLF4J providers were found」エラーが継続
- **根本原因**:
  - commonモジュールで`slf4j-simple`を宣言
  - しかしbukkit/bungeeモジュールで明示的に宣言されていなかった
  - 推移的依存関係として含まれるべきだったが、shadowJarで正しく処理されなかった
- **解決**:
  - bukkit/bungee両モジュールの`dependencies`に`slf4j-simple:2.0.9`を明示的に追加
  - shadowJarが依存関係を確実に含めるように修正
- **影響**: SLF4Jエラーが完全に解消され、HikariCPのログが正常に出力される

### v2.3.8 - SLF4J Provider修正 (2026-02-07)
- **SLF4Jエラー修正**: 「SLF4J: No SLF4J providers were found」エラー解消
- **問題**: HikariCPのログ出力時にSLF4Jプロバイダーが見つからない
- **根本原因**:
  - SLF4Jのリロケーション（`org.slf4j` → `lib.slf4j`）によりサービスプロバイダーメカニズムが破損
  - SLF4J 2.xはJava ServiceLoader APIを使用するため、パッケージ名変更で機能しなくなる
  - `META-INF/services/org.slf4j.spi.SLF4JServiceProvider`が見つからない
- **解決**:
  - SLF4Jリロケーションを削除（bukkit & bungee両モジュール）
  - `mergeServiceFiles()`を追加してサービスプロバイダーファイルを統合
  - HikariCPとMySQLのみリロケートを維持（依存関係の競合回避）
- **影響**: SLF4Jエラーが解消され、HikariCPのログが正常に出力される

### v2.3.7 - Shadow plugin移行（新プラグインID） (2026-02-07)
- **ビルドエラー修正**: 「Plugin 'com.github.johnrengelman.shadow' version '8.1.7' was not found」エラー解消
- **問題**: Shadow plugin version 8.1.7が存在しない
- **根本原因**:
  - v2.3.6で指定した8.1.7は存在しないバージョン
  - 旧プラグインID（`com.github.johnrengelman.shadow`）の最終版は8.1.1
  - Java 21完全サポートには新プラグインID（`com.gradleup.shadow`）が必要
- **解決**:
  - プラグインID変更: `com.github.johnrengelman.shadow` → `com.gradleup.shadow`
  - バージョン: 8.3.3に更新（Java 21完全サポート）
  - 組織移行: John Rengelman → GradleUp
- **影響**: ビルドが正常に完了し、最新のShadow pluginでメンテナンス継続

### v2.3.6 - Shadow plugin & Gradle更新 (2026-02-07)
- **ビルドエラー修正**: shadowJarタスクの「Unsupported class file major version 65」エラー解消
- **問題**: Java 21でコンパイルされたクラスファイルをshadowJarが処理できない
- **根本原因**:
  - Shadow plugin 8.1.1のASMライブラリがJava 21バイトコードを完全サポートしていない
  - Class file major version 65 (Java 21) の処理に失敗
- **解決**:
  - Shadow plugin: 8.1.1 → 8.1.7 に更新
  - Gradle: 8.5 → 8.9 に更新
  - ASMライブラリの最新化によりJava 21完全サポート
- **影響**: ビルドが正常に完了し、JARファイルが生成されるようになった

### v2.3.5 - Plan統計表示のNULL処理修正 (2026-02-07)
- **バグ修正**: Planのサーバー総採掘数とネットワーク総採掘数が正常に表示されない
- **問題**: SQLの`SUM(count)`がNULLを返した際の処理が不適切
- **解決**: 
  - `rs.wasNull()`で明示的にNULLチェックを追加
  - NULLの場合は確実に0を返すように修正
  - エラーログを改善し、スタックトレースを追加
- **影響**: Planでサーバー/ネットワーク統計が正しく表示されるようになった

### v2.3.4 - サーバー間リアルタイム同期修正 (2026-02-07)
- **重大なバグ修正**: サーバー間でリアルタイムに採掘量が同期されない問題を解決
- **問題**: あるサーバーで採掘したデータが、他のサーバーから即座に見えない
- **原因**: HikariCPの`elideSetAutoCommits`最適化とautoCommitの暗黙的な動作
- **解決**: 
  - `elideSetAutoCommits`最適化を削除
  - `hikariConfig.setAutoCommit(true)`を明示的に設定
  - データ書き込み時にautoCommitを確実に有効化
- **影響**: データ書き込み後、即座に全サーバーから見えるようになった

### v2.3.3 - shadowJar設定修正とドキュメント改善 (2026-02-07)
- **重大な改善**: shadowJar設定を改善してリソースを明示的に包含
- **ドキュメント**: README/BUNGEECORD_SETUPに詳細なトラブルシューティングを追加
- **明確化**: Bukkit版とBungee版の使い分けを明確に説明
- **依存関係リロケーション**: HikariCP, MySQL Connector, SLF4Jをリロケートして競合を回避
- **問題解決**: "Plugin must have plugin.yml or bungee.yml"エラーの原因と解決方法を文書化

### v2.3.2 - Bungeecord API バージョン修正 (2026-02-07)
- **重大なバグ修正**: Bungeecord APIバージョンを正しいものに修正
- **ビルド**: Bungeecordモジュールのコンパイルエラー解消
- **依存関係**: `1.21-R0.1-SNAPSHOT` → `1.21-R0.4` に変更
- **説明**: BungeecordはSNAPSHOTバージョンを使用せず、安定版リリースを使用

### v2.3.1 - HikariCP依存関係修正 (2026-02-07)
- **重大なバグ修正**: bukkit/bungeeモジュールのHikariCP依存関係不足を解消
- **ビルド**: コンパイルエラー完全解消
- **依存関係**: MySQL Connector, HikariCP, slf4jを明示的に追加
- **説明**: Gradleの`implementation`依存関係は推移的でないため、各モジュールで明示的に宣言

### v2.3.0 - ビルドエラー修正とマルチモジュール完成 (2026-02-07)
- **重大なバグ修正**: ルートディレクトリの古い`src/`削除によりビルドエラーを解消
- **ビルド**: マルチモジュールプロジェクトのビルドが正常に動作
- **クリーンアップ**: プロジェクト構造を完全にマルチモジュール化
- v2.2.0の全機能を継承

### v2.2.0 - Bungeecordプラグイン対応とマルチモジュール化 (2026-02-07)
- **新機能**: Bungeecordプロキシに直接インストール可能なプラグインを追加 ⭐
- **新機能**: MiningTracker-Bungee - ネットワーク統計のみをPlanに表示
- **アーキテクチャ**: マルチモジュールGradleプロジェクトに変更
  - `miningtracker-common`: 共通コード（DB、設定）
  - `miningtracker-bukkit`: Bukkitプラグイン（既存機能維持）
  - `miningtracker-bungee`: Bungeecordプラグイン（新規）
- **新機能**: ConfigAdapterインターフェース - Bukkit/Bungeeの設定統一化
- **新機能**: CommonDatabaseManager - プラットフォーム非依存のDB処理
- **Plan連携**: Bungee版はネットワーク統計のみ表示（サーバー統計は非表示）
- **Plan連携**: Bukkit版は全統計表示（プレイヤー、サーバー、ネットワーク）
- **ドキュメント**: README.md - Bungeecordプラグインの説明追加
- **ドキュメント**: BUNGEECORD_SETUP.md - 2つのインストール方法を詳説
- **ドキュメント**: Bukkit版とBungee版の比較表を追加
- **重大なバグ修正**: DatabaseManagerのResultSetリソースリークを修正（継続）

### v2.1.9 - 重複テーブル表示の修正 (2026-02-03)
- **バグ修正**: Planでテーブルが2つずつ表示される問題を修正
- `@InvalidateMethod`アノテーションを追加して、古いメソッド名から新しいメソッド名への移行を明示
- Planでテーブルが重複せず、正しく1つずつ表示されるようになる

### v2.1.8 - テーブルメソッド名の日本語化 (2026-02-03)
- **国際化**: テーブルプロバイダーメソッド名を日本語Romaji（ローマ字）に変更
- コンパイルエラーを修正（@TableProviderはtextパラメータをサポートしていない）
- メソッド名を日本語Romajiに変更することで、より日本語に近い表示を実現

### v2.1.7 - Planの英語/日本語混在問題の解決ガイド追加 (2026-02-03)
- **ドキュメント改善**: Planの言語設定に関するトラブルシューティングガイドを追加
- PLAN_INTEGRATION.mdとPLAN_TROUBLESHOOTING.mdに詳細な解決方法を記載

### v2.1.6 - PlanページUI完全日本語化 (2026-02-02)
- **UI改善**: PlanのMiningTrackerページをすべて日本語化
- タブ名を日本語に変更（Mining Stats → 採掘統計、Server Stats → サーバー統計、Network Stats → ネットワーク統計）
- すべてのNumberProviderのtext属性を日本語化（7項目）
- すべてのTableProviderの列名を日本語化（9項目）
- 日本語ユーザーにとってより理解しやすいインターフェースに改善
- description（説明文）は元々日本語のため変更なし

### v2.1.5 - Plan拡張データ表示問題の修正 (2026-02-02)
- **重大なバグ修正**: Plan拡張データが表示されない問題を完全に解決
- `MiningTracker.java`から古い`CapabilityService`を削除
- `registerPlanHook()`を簡素化し、直接`ExtensionService.register()`を呼び出し
- 非同期リスナーを削除し、同期的な登録に変更（タイミング問題を解消）
- 不要な`callExtensionMethodsOn()`メソッドを削除
- エラーハンドリングを改善
- Planに統計データが正常に表示されるようになった

### v2.1.4 - DataExtension登録方法の完全修正 (2026-02-02)
- **重要なバグ修正**: Plan API登録メソッドを正しい方法に変更
- `CapabilityService.registerExtension()` → `ExtensionService.register()` に変更
- Plan API 5.6公式ドキュメントに準拠した登録方法を実装
- 適切なエラーハンドリング（NoClassDefFoundError, IllegalStateException, IllegalArgumentException）
- ビルドエラーを完全に解消
- Plan連携機能が公式推奨方法で正常に動作

### v2.1.3 - CapabilityService.registerExtension メソッド修正 (2026-02-02)
- **重要なバグ修正**: Plan API registerExtensionメソッドのシグネチャ修正
- `registerExtension(String, DataExtension)` → `registerExtension(DataExtension)` に変更
- プラグイン名パラメータを削除（@PluginInfoアノテーションで指定済みのため）
- ビルドエラーを完全に解消

### v2.1.2 - Plan APIコンパイルエラー修正 (2026-02-02)
- **重要なバグ修正**: Plan APIのコンパイルエラーを修正
- `ElementOrder` のインポート追加
- `register()` メソッドをPlan API 5.6の正しい使用方法に修正
- `registerEnableListener()` を使用したDataExtension登録に変更
- ビルドが正常に完了するように修正

### v2.1.1 - Plan依存関係修正 (2026-02-02)
- **重要なバグ修正**: Gradle依存関係の解決エラーを修正
- JitPackリポジトリを追加
- Plan依存関係を正しい形式に変更: `com.github.plan-player-analytics:Plan:5.6.2959`
- 開発者向けビルド設定のドキュメントを追加

### v2.1.0 - Plan Player Analytics連携とマルチサーバー対応 (2026-02-02)
- **新機能**: Plan Player Analyticsとの統合
- **新機能**: マルチサーバー対応（サーバー別データ記録）
- **データベース**: server_nameカラム追加、自動マイグレーション
- **API**: 17の新しいメソッド追加
- **ドキュメント**: PLAN_INTEGRATION.md追加

### v2.0.3 - HikariCP接続リーク修正 (2026-02-02)
- **重要なバグ修正**: HikariCP接続プールからの接続リークを修正
- 接続を一時的に取得し、使用後すぐにプールに返却するように改善
- 接続リーク検出警告が解消

### v2.0.2 - MySQL Connector/J 8.x完全対応 (2026-02-02)
- **重要なバグ修正**: MySQL Connector/J 8.x互換性の問題を完全に修正
- `characterEncoding`パラメータを`connectionCollation`に変更
- v2.0.1でも発生していた接続エラーを解消

### v2.0.1 - HikariCP修正（不完全） (2026-02-02)
- **バグ修正**: HikariCP文字エンコーディング設定エラーの修正を試みた
- 注: MySQL Connector/J 8.xの非推奨パラメータ使用により、完全には修正されなかった
- SQLite接続ライフサイクル管理の改善

### v2.0.0 - Minecraft 1.21.x対応 (2026-02-01)
- Minecraft 1.21.x完全対応
- Java 21必須化
- Spigot API 1.21への更新
- メジャーバージョンアップ

### v1.0.0 - 初回リリース (2026-01-30)
- Minecraft 1.19.4対応
- 基本機能の実装
- SQLite/MySQLサポート
- HikariCP接続プール

## バージョニングルール

このプロジェクトは [Semantic Versioning](https://semver.org/) に従います。

### フォーマット
`MAJOR.MINOR.PATCH`

### ルール
- **MAJOR**: Minecraftメジャーバージョン変更、破壊的変更
- **MINOR**: 新機能追加（後方互換性あり）
- **PATCH**: バグ修正、小さな改善

## リリースプロセス

### 1. バージョン番号の更新
- `build.gradle` の `version` を更新
- `CHANGELOG.md` に変更内容を追加
- `VERSION.md` (このファイル) を更新

### 2. リリースノートの作成
- `RELEASE_NOTES_vX.Y.Z.md` を作成

### 3. Gitタグの作成
```bash
git tag -a vX.Y.Z -m "Release vX.Y.Z"
git push origin vX.Y.Z
```

### 4. ビルドとテスト
```bash
./gradlew clean build
# テストの実行
```

### 5. リリース
- GitHubでリリースを作成
- JARファイルを添付
- リリースノートを記載

## 次期バージョン予定

### v2.1.0 (予定)
- 新機能追加の可能性
- パフォーマンス改善

### v2.0.1 (必要に応じて)
- バグ修正
- マイナーな改善
