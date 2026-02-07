# MiningTracker v2.3.2 リリースノート - Bungeecord APIバージョン修正

**リリース日**: 2026-02-07  
**バージョン**: 2.3.2  
**対応Minecraft**: 1.21.x  
**必須Java**: 21+

## 🐛 主な修正

### Bungeecord APIバージョンエラーの解消

v2.3.1でBungeecordモジュールを追加した際、存在しないバージョンのBungeecord APIを指定していたため、ビルドエラーが発生していました。

#### 問題の詳細

```
Could not find net.md-5:bungeecord-api:1.21-R0.1-SNAPSHOT.
```

**原因**: 
- Bungeecord APIのバージョン`1.21-R0.1-SNAPSHOT`が存在しない
- BungeecordはSpigotと異なり、SNAPSHOTバージョンを使用しない
- 安定版リリース（例: `1.21-R0.4`）を使用する必要がある

**解決**:
- Bungeecord APIバージョンを正しい安定版に変更

```gradle
// 修正前
compileOnly 'net.md-5:bungeecord-api:1.21-R0.1-SNAPSHOT'

// 修正後
compileOnly 'net.md-5:bungeecord-api:1.21-R0.4'
```

## ✨ v2.3.1の全機能を継承

v2.3.2は、v2.3.1の全機能をそのまま継承しています：

- ✅ Bungeecordプラグイン対応
- ✅ マルチモジュール構造
- ✅ ネットワーク統計表示
- ✅ Plan Player Analytics連携
- ✅ HikariCP依存関係修正済み

## 📦 ビルド成果物

```
build/libs/
├── MiningTracker-Bukkit-2.3.2.jar   # バックエンドサーバー用
└── MiningTracker-Bungee-2.3.2.jar   # Bungeecordプロキシ用
```

## 🚀 使用方法

v2.3.1と同じインストール方法で使用できます。

### バックエンドサーバー
```bash
cp MiningTracker-Bukkit-2.3.2.jar /path/to/server/plugins/
```

### Bungeecordプロキシ（オプション）
```bash
cp MiningTracker-Bungee-2.3.2.jar /path/to/bungeecord/plugins/
```

## 🔄 v2.3.1からの移行

**必要な作業**: なし

単純にJARファイルを置き換えるだけで動作します。

```bash
# 古いバージョンを削除
rm MiningTracker-Bukkit-2.3.1.jar
rm MiningTracker-Bungee-2.3.1.jar

# 新バージョンをインストール
cp MiningTracker-Bukkit-2.3.2.jar server/plugins/
cp MiningTracker-Bungee-2.3.2.jar bungeecord/plugins/
```

## 📊 技術詳細

### Bungeecord APIバージョニング

Bungeecordは、Spigotとは異なるバージョン管理を使用しています：

| プラットフォーム | バージョン形式 | 例 |
|--------------|--------------|-----|
| Spigot | `X.XX-RX.X-SNAPSHOT` | `1.21-R0.1-SNAPSHOT` |
| Bungeecord | `X.XX-RX.X` (安定版) | `1.21-R0.4` |

Bungeecord APIは**SNAPSHOT版を使用せず**、常に安定版リリースを使用します。

### Maven座標

```gradle
// Bungeecord API (正しい)
compileOnly 'net.md-5:bungeecord-api:1.21-R0.4'

// Spigot API (参考)
compileOnly 'org.spigotmc:spigot-api:1.21-R0.1-SNAPSHOT'
```

### 利用可能なリポジトリ

Bungeecord APIは以下のリポジトリから取得可能：
- Maven Central
- https://repo.maven.apache.org/maven2/

## ⚠️ 既知の問題

現時点では既知の問題はありません。

## 🙏 謝辞

ビルドエラーの報告をいただき、ありがとうございました。

## 🔗 参考リンク

- **v2.3.1リリースノート**: [RELEASE_NOTES_v2.3.1.md](RELEASE_NOTES_v2.3.1.md)
- **v2.3.0リリースノート**: [RELEASE_NOTES_v2.3.0.md](RELEASE_NOTES_v2.3.0.md)
- **v2.2.0リリースノート**: [RELEASE_NOTES_v2.2.0_BUNGEE.md](RELEASE_NOTES_v2.2.0_BUNGEE.md)
- **Bungeecordセットアップ**: [BUNGEECORD_SETUP.md](BUNGEECORD_SETUP.md)
- **変更履歴**: [CHANGELOG.md](CHANGELOG.md)
- **バージョン情報**: [VERSION.md](VERSION.md)

## 💬 サポート

問題が発生した場合は、GitHubのIssuesページで報告してください。

---

**MiningTracker v2.3.2** - Bungeecord API Fix Release  
© 2026 kubota6646

正常なビルドをお楽しみください！
