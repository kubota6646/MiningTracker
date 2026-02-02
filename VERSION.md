# バージョン管理

## 現在のバージョン

**v2.1.1** (2026-02-02)

## バージョン履歴

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
