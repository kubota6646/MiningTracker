# バージョン管理

## 現在のバージョン

**v2.0.1** (2026-02-02)

## バージョン履歴

### v2.0.1 - HikariCP修正 (2026-02-02)
- **バグ修正**: HikariCP文字エンコーディング設定エラーを修正
- MySQL接続時の`Unsupported character encoding 'utf8mb4'`エラーを解決
- 接続パラメータをJDBC URLに移動（DataSourceプロパティから）
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
