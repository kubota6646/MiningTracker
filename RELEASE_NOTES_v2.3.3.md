# MiningTracker v2.3.3 リリースノート - Paper/Bungeecord動作問題修正

**リリース日**: 2026-02-07  
**バージョン**: 2.3.3  
**対応Minecraft**: 1.21.x  
**必須Java**: 21+

## 🐛 修正した問題

### ユーザー報告の問題

#### 1. Paperサーバーで動作しない
**原因**: ドキュメント不足により、正しいJARファイルの使い方が不明瞭

**解決**: 
- READMEに詳細なインストール手順を追加
- トラブルシューティングセクションを追加
- 正しいJARファイルの使い分けを明確化

#### 2. Bungeecord: "Plugin must have a plugin.yml or bungee.yml"
**原因**: 間違ったJARファイルをBungeecordで使用

**解決**:
- 2種類のJARがあることを明示
- 各プラットフォーム専用JARの使用を強調
- エラーの原因と解決方法を文書化

## 🔧 技術的改善

### shadowJar設定の強化

両モジュール（bukkit/bungee）のshadowJar設定を改善：

```gradle
shadowJar {
    // リソースを明示的に包含
    from(sourceSets.main.output)
    
    // 依存関係の競合を回避
    relocate 'com.zaxxer.hikari', 'com.kubota6646.miningtracker.lib.hikari'
    relocate 'com.mysql', 'com.kubota6646.miningtracker.lib.mysql'
    relocate 'org.slf4j', 'com.kubota6646.miningtracker.lib.slf4j'
}
```

**メリット**:
- plugin.yml/bungee.ymlが確実にJARに含まれる
- 他のプラグインとの依存関係競合を防止
- より安定した動作

## 📚 ドキュメント改善

### README.md

#### ビルド成果物の明確化
```
ビルド成功時:
- Bukkit/Paper用: MiningTracker-Bukkit-2.3.3.jar
- Bungeecord用: MiningTracker-Bungee-2.3.3.jar
```

#### インストール手順の詳細化
- **シングルサーバー**: Bukkit版の使い方
- **Bungeecordネットワーク**: 
  - バックエンドサーバー: Bukkit版
  - プロキシ（オプション）: Bungee版

#### トラブルシューティング追加
1. "Plugin must have plugin.yml or bungee.yml" エラー
2. Paper/Spigotで動作しない場合
3. データベース接続エラー
4. Bungeecord統計が表示されない場合

### BUNGEECORD_SETUP.md

#### 冒頭に警告セクション追加

| 用途 | JARファイル | インストール場所 |
|------|-----------|---------------|
| Bukkit/Paper | MiningTracker-Bukkit-2.3.3.jar | バックエンド |
| Bungeecord | MiningTracker-Bungee-2.3.3.jar | プロキシ |

## ⚠️ 重要な注意事項

### 正しいJARファイルの使用

**✅ 正しい使用:**
```bash
# Bukkit/Paper サーバー
cp MiningTracker-Bukkit-2.3.3.jar server/plugins/

# Bungeecord プロキシ
cp MiningTracker-Bungee-2.3.3.jar bungeecord/plugins/
```

**❌ 間違った使用（エラー発生）:**
```bash
# Bungeecordに Bukkit版 を使用 → エラー!
cp MiningTracker-Bukkit-2.3.3.jar bungeecord/plugins/

# Bukkitに Bungee版 を使用 → 動作しない!
cp MiningTracker-Bungee-2.3.3.jar server/plugins/
```

### エラーメッセージと解決方法

| エラー | 原因 | 解決方法 |
|--------|------|---------|
| "Plugin must have plugin.yml or bungee.yml" | 間違ったJAR | 正しいJARを使用 |
| プラグインが読み込まれない | 間違ったJAR | 正しいJARを使用 |
| Java version エラー | Java 17以下 | Java 21に更新 |

## 📦 ビルド成果物

```
miningtracker-bukkit/build/libs/
└── MiningTracker-Bukkit-2.3.3.jar   # Bukkit/Paper用

miningtracker-bungee/build/libs/
└── MiningTracker-Bungee-2.3.3.jar   # Bungeecord用
```

## 🚀 使用方法

### シングルサーバー（Bukkit/Paper）

```bash
# 1. 正しいJARをダウンロード
wget https://github.com/kubota6646/MiningTracker/releases/download/v2.3.3/MiningTracker-Bukkit-2.3.3.jar

# 2. pluginsフォルダにコピー
cp MiningTracker-Bukkit-2.3.3.jar /path/to/server/plugins/

# 3. サーバーを再起動
```

### Bungeecordネットワーク

```bash
# バックエンドサーバー（各サーバー）
cp MiningTracker-Bukkit-2.3.3.jar /path/to/survival/plugins/
cp MiningTracker-Bukkit-2.3.3.jar /path/to/creative/plugins/

# Bungeecordプロキシ（オプション）
cp MiningTracker-Bungee-2.3.3.jar /path/to/bungeecord/plugins/
```

## 🔄 v2.3.2からの移行

**必要な作業**: JARファイルの置き換えのみ

```bash
# 古いバージョンを削除
rm MiningTracker-Bukkit-2.3.2.jar
rm MiningTracker-Bungee-2.3.2.jar

# 新バージョンをインストール
cp MiningTracker-Bukkit-2.3.3.jar server/plugins/
cp MiningTracker-Bungee-2.3.3.jar bungeecord/plugins/
```

## ✨ v2.3.2の全機能を継承

- ✅ Bungeecordプラグイン対応
- ✅ マルチモジュール構造
- ✅ ネットワーク統計表示
- ✅ Plan Player Analytics連携
- ✅ HikariCP依存関係修正済み
- ✅ Bungeecord API 1.21-R0.4使用

## 🙏 謝辞

Paper/Bungeecordでの動作問題の報告をいただき、ありがとうございました。このフィードバックにより、ドキュメントとビルド設定を大幅に改善できました。

## 🔗 参考リンク

- **v2.3.2リリースノート**: [RELEASE_NOTES_v2.3.2.md](RELEASE_NOTES_v2.3.2.md)
- **v2.3.1リリースノート**: [RELEASE_NOTES_v2.3.1.md](RELEASE_NOTES_v2.3.1.md)
- **Bungeecordセットアップ**: [BUNGEECORD_SETUP.md](BUNGEECORD_SETUP.md)
- **トラブルシューティング**: [README.md#トラブルシューティング](README.md#トラブルシューティング)
- **変更履歴**: [CHANGELOG.md](CHANGELOG.md)
- **バージョン情報**: [VERSION.md](VERSION.md)

## 💬 サポート

問題が発生した場合は、以下を確認してください：

1. **正しいJARファイルを使用しているか**
2. **Java 21を使用しているか** (`java -version`)
3. **トラブルシューティングセクション**を確認

それでも解決しない場合は、GitHubのIssuesページで報告してください。

---

**MiningTracker v2.3.3** - Paper/Bungeecord Fix Release  
© 2026 kubota6646

快適なマイニングライフをお楽しみください！
