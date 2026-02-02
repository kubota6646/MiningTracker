# MiningTracker v2.0.0 リリースノート

## 🎉 メジャーアップデート - Minecraft 1.21.x対応

**リリース日**: 2026年2月1日  
**対応Minecraft**: 1.21.x (1.21, 1.21.1, 1.21.3など)  
**必須Java**: 21

---

## 📦 ダウンロード

ビルドされたJARファイル: `MiningTracker-2.0.0.jar`

```bash
# ビルド方法
./gradlew clean build

# 成果物の場所
build/libs/MiningTracker-2.0.0.jar
```

---

## 🆕 新機能と変更点

### Minecraft 1.21.x完全対応

このバージョンは、Minecraft 1.21系（1.21, 1.21.1, 1.21.3等）に完全対応しています。

#### 主な変更

1. **Minecraft対応バージョン**
   - 1.19.4 → **1.21.x**
   - 最新のMinecraftバージョンで動作

2. **Java要件の変更**
   - Java 17以降 → **Java 21必須**
   - Minecraft 1.21の要件に対応

3. **Spigot API更新**
   - 1.19.4-R0.1-SNAPSHOT → **1.21-R0.1-SNAPSHOT**
   - 最新のAPI機能をサポート

4. **Gradle互換性**
   - Gradle 8.5でJava 21をフルサポート
   - ビルド環境の最新化

---

## ✨ 機能一覧

すべての既存機能が1.21.xで正常に動作します：

### コア機能
- ✅ **ブロック採掘トラッキング** - プレイヤーの採掘を自動記録
- ✅ **統計表示** - `/mtstats` コマンドで詳細な統計を表示
- ✅ **ランキングシステム** - `/mtranking` でプレイヤーランキング
- ✅ **統計リセット** - `/mtreset` で管理者が統計をリセット

### データベース
- ✅ **SQLiteサポート** - デフォルトの軽量データベース
- ✅ **MySQLサポート** - 大規模サーバー向け
- ✅ **HikariCP接続プール** - 高性能なMySQL接続管理
- ✅ **自動テーブル作成** - 初回起動時に自動セットアップ

### カスタマイズ
- ✅ **日本語メッセージ** - すべてのメッセージが日本語
- ✅ **設定ファイル** - config.ymlで柔軟なカスタマイズ
- ✅ **権限システム** - 細かい権限管理

---

## 📋 動作環境

### 必須要件

| 項目 | 要件 |
|------|------|
| Minecraft | 1.21.x |
| Java | **21以降（必須）** |
| サーバー | Spigot/Paper 1.21.x |
| Gradle | 8.5以降 |

### オプション

| 項目 | 推奨 |
|------|------|
| MySQL | 5.7以降 / 8.0以降 |
| メモリ | 512MB以上推奨 |

---

## ⚠️ 重要な注意事項

### Java 21必須

> **このバージョンからJava 21が必須です**

Minecraft 1.21.xはJava 21を必要とするため、このプラグインもJava 21が必須となります。

```bash
# Java バージョンの確認
java -version

# 期待される出力: openjdk version "21.x.x" ...
```

### 下位互換性

このバージョンはMinecraft 1.19.4と互換性がありません。

| Minecraftバージョン | プラグインバージョン |
|-------------------|-------------------|
| 1.19.4 | [v1.0.0](https://github.com/kubota6646/MiningTracker/releases/tag/v1.0.0) |
| **1.21.x** | **v2.0.0**（本リリース） |

---

## 🚀 インストール方法

### 新規インストール

1. **Javaの確認**
   ```bash
   java -version
   # Java 21以降であることを確認
   ```

2. **プラグインのインストール**
   ```bash
   # JARファイルをpluginsフォルダにコピー
   cp MiningTracker-2.0.0.jar /path/to/server/plugins/
   ```

3. **サーバーの起動**
   ```bash
   # サーバーを起動
   java -jar paper-1.21.jar
   ```

4. **設定のカスタマイズ**
   - `plugins/MiningTracker/config.yml` で設定を変更
   - `plugins/MiningTracker/messages.yml` でメッセージをカスタマイズ

### v1.0.0からのアップグレード

1. **データベースのバックアップ（重要）**
   ```bash
   # SQLiteの場合
   cp plugins/MiningTracker/mining_data.db plugins/MiningTracker/mining_data.db.backup
   
   # MySQLの場合
   mysqldump -u username -p minecraft > backup.sql
   ```

2. **Java 21へのアップグレード**
   - サーバーのJavaを21にアップグレード
   - `JAVA_HOME`環境変数の更新

3. **プラグインの置き換え**
   ```bash
   # 古いバージョンを削除
   rm plugins/MiningTracker-1.0.0.jar
   
   # 新しいバージョンをコピー
   cp MiningTracker-2.0.0.jar plugins/
   ```

4. **Minecraftサーバーの更新**
   - サーバーを1.21.xにアップグレード
   - Paper/Spigot 1.21.xを使用

5. **サーバーの起動と確認**
   ```bash
   # サーバーを起動
   java -jar paper-1.21.jar
   
   # ログで確認
   # [MiningTracker] MiningTracker が有効化されました。
   ```

---

## 🔧 設定例

### SQLite使用（デフォルト）

```yaml
database:
  type: sqlite
  sqlite:
    file: mining_data.db
```

### MySQL使用

```yaml
database:
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: minecraft_user
    password: your_password
    pool:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
```

---

## 📝 コマンド一覧

| コマンド | 説明 | 権限 |
|---------|------|------|
| `/mtstats [プレイヤー]` | 採掘統計を表示 | `miningtracker.use` |
| `/mtranking [ページ]` | ランキングを表示 | `miningtracker.use` |
| `/mtreset <player\|all> [confirm]` | 統計をリセット | `miningtracker.reset` |

---

## 🐛 既知の問題

現在、重大な既知の問題はありません。

問題を発見した場合は、[Issues](https://github.com/kubota6646/MiningTracker/issues)で報告してください。

---

## 🔄 変更履歴

詳細な変更履歴は [CHANGELOG.md](CHANGELOG.md) を参照してください。

### v2.0.0の主な変更
- Minecraft 1.21.x対応
- Java 21必須化
- Spigot API 1.21への更新
- ドキュメントの更新

---

## 📚 ドキュメント

- [README.md](README.md) - 基本的な使い方
- [CHANGELOG.md](CHANGELOG.md) - 変更履歴
- [MYSQL_SETUP.md](MYSQL_SETUP.md) - MySQL設定ガイド
- [MYSQL_IMPLEMENTATION.md](MYSQL_IMPLEMENTATION.md) - 技術詳細
- [BUILD_NOTES.md](BUILD_NOTES.md) - ビルドに関する注意事項

---

## 🤝 コントリビューション

バグ報告や機能リクエストは [Issues](https://github.com/kubota6646/MiningTracker/issues) へ。

---

## 📄 ライセンス

このプロジェクトは [MIT License](LICENSE) の下で公開されています。

---

## 🙏 謝辞

このプラグインを使用していただきありがとうございます！

問題や質問がある場合は、お気軽にIssuesで報告してください。

---

**MiningTracker v2.0.0** - Minecraft 1.21.x対応版  
© 2026 kubota6646
