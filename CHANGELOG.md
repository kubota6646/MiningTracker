# 変更履歴 / Changelog

## [2.0.1] - 2026-02-02

### 🐛 バグ修正 / Bug Fixes

#### 修正 / Fixed
- **HikariCP接続エラー**: MySQL接続時の`Unsupported character encoding 'utf8mb4'`エラーを修正
- **SQLite接続管理**: SQLiteデータベース接続のライフサイクル管理を改善
- 接続パラメータ（characterEncoding、useSSL、serverTimezoneなど）をJDBC URLパラメータとして正しく設定

#### 技術的詳細 / Technical Details
- `characterEncoding`をDataSourceプロパティからJDBC URLパラメータに移動
- HikariCPの設定を正しく分離:
  - JDBC URLパラメータ: 接続レベルの設定（SSL、タイムゾーン、文字エンコーディング）
  - DataSourceプロパティ: パフォーマンス最適化設定（キャッシュ設定など）
- SQLite: 永続的な接続を維持するように改善
- MySQL: 接続プール管理を最適化

#### ドキュメント / Documentation
- HIKARICP_FIX.md: HikariCP設定の詳細ガイドを追加
- MYSQL_SETUP.md: トラブルシューティングセクションを追加
- SQLITE_FIX.md: SQLite接続問題の解決方法を追加

#### 影響 / Impact
- ✅ MySQL接続が正常に動作
- ✅ UTF-8MB4文字エンコーディングが正しく適用
- ✅ 日本語データの保存・取得が正常に動作
- ✅ SQLiteデータベース操作が安定
- ⚠️ 設定ファイルの変更は不要

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
