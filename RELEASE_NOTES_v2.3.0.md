# MiningTracker v2.3.0 リリースノート - ビルドエラー修正

**リリース日**: 2026-02-07  
**バージョン**: 2.3.0  
**対応Minecraft**: 1.21.x  
**必須Java**: 21+

## 🐛 主な修正

### ビルドエラーの解消

v2.2.0でマルチモジュールプロジェクトに移行した際、ルートディレクトリに古い`src/`ディレクトリが残っていたため、ビルドエラーが発生していました。

#### 問題の詳細

```
エラー: パッケージcom.zaxxer.hikariは存在しません
エラー: パッケージorg.bukkitは存在しません
```

**原因**: 
- マルチモジュール化後、ルートの`src/`ディレクトリが残存
- Gradleがルートプロジェクトの`src/`をコンパイルしようとする
- ルートプロジェクトには依存関係が設定されていない

**解決**:
- ルートディレクトリの古い`src/`を完全に削除
- gitから該当ファイルを削除

## ✨ v2.2.0の全機能を継承

v2.3.0は、v2.2.0の全機能をそのまま継承しています：

- ✅ Bungeecordプラグイン対応
- ✅ マルチモジュール構造
- ✅ ネットワーク統計表示
- ✅ Plan Player Analytics連携

## 📦 ビルド成果物

```
build/libs/
├── MiningTracker-Bukkit-2.3.0.jar   # バックエンドサーバー用
└── MiningTracker-Bungee-2.3.0.jar   # Bungeecordプロキシ用
```

## 🚀 使用方法

v2.2.0と同じインストール方法で使用できます。

### バックエンドサーバー
```bash
cp MiningTracker-Bukkit-2.3.0.jar /path/to/server/plugins/
```

### Bungeecordプロキシ（オプション）
```bash
cp MiningTracker-Bungee-2.3.0.jar /path/to/bungeecord/plugins/
```

## 🔄 v2.2.0からの移行

**必要な作業**: なし

単純にJARファイルを置き換えるだけで動作します。

```bash
# 古いバージョンを削除
rm MiningTracker-Bukkit-2.2.0.jar
rm MiningTracker-Bungee-2.2.0.jar

# 新バージョンをインストール
cp MiningTracker-Bukkit-2.3.0.jar server/plugins/
cp MiningTracker-Bungee-2.3.0.jar bungeecord/plugins/
```

## 📊 技術詳細

### 削除されたファイル

ルートディレクトリから以下のファイルが削除されました：

```
src/main/java/com/kubota6646/miningtracker/
├── MiningTracker.java
├── commands/
│   ├── RankingCommand.java
│   ├── ResetCommand.java
│   └── StatsCommand.java
├── database/
│   └── DatabaseManager.java
├── listeners/
│   └── BlockBreakListener.java
├── managers/
│   ├── DataManager.java
│   └── MessageManager.java
└── plan/
    └── MiningTrackerExtension.java

src/main/resources/
├── config.yml
├── messages.yml
└── plugin.yml
```

これらのファイルは、`miningtracker-bukkit/src/`に正しく配置されています。

### プロジェクト構造

```
MiningTracker/
├── miningtracker-common/     # 共通コード
├── miningtracker-bukkit/     # Bukkitプラグイン
├── miningtracker-bungee/     # Bungeecordプラグイン
├── build.gradle              # ルートビルド設定
└── settings.gradle           # モジュール設定
```

## ⚠️ 既知の問題

現時点では既知の問題はありません。

## 🙏 謝辞

ビルドエラーの報告をいただき、ありがとうございました。

## 🔗 参考リンク

- **v2.2.0リリースノート**: [RELEASE_NOTES_v2.2.0_BUNGEE.md](RELEASE_NOTES_v2.2.0_BUNGEE.md)
- **Bungeecordセットアップ**: [BUNGEECORD_SETUP.md](BUNGEECORD_SETUP.md)
- **変更履歴**: [CHANGELOG.md](CHANGELOG.md)
- **バージョン情報**: [VERSION.md](VERSION.md)

## 💬 サポート

問題が発生した場合は、GitHubのIssuesページで報告してください。

---

**MiningTracker v2.3.0** - Build Fix Release  
© 2026 kubota6646

安定したビルドをお楽しみください！
