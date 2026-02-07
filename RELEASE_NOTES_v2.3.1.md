# MiningTracker v2.3.1 リリースノート - HikariCP依存関係修正

**リリース日**: 2026-02-07  
**バージョン**: 2.3.1  
**対応Minecraft**: 1.21.x  
**必須Java**: 21+

## 🐛 主な修正

### HikariCP依存関係エラーの解消

v2.3.0でマルチモジュールプロジェクトに移行した際、依存関係の設定が不十分だったため、コンパイルエラーが発生していました。

#### 問題の詳細

```
エラー: パッケージcom.zaxxer.hikariは存在しません
import com.zaxxer.hikari.HikariConfig;
```

**原因**: 
- `miningtracker-bukkit`モジュールの`DatabaseManager`がHikariCPを直接使用
- `miningtracker-common`がHikariCPを`implementation`として宣言
- Gradleの`implementation`依存関係は**推移的でない**（消費側に自動的に伝播しない）
- 各モジュールで必要な依存関係を明示的に宣言する必要がある

**解決**:
- `miningtracker-bukkit/build.gradle`に依存関係を追加
- `miningtracker-bungee/build.gradle`に依存関係を追加

```gradle
dependencies {
    implementation project(':miningtracker-common')
    // ... 他の依存関係 ...
    
    // 明示的に追加
    implementation 'com.mysql:mysql-connector-j:8.3.0'
    implementation 'com.zaxxer:HikariCP:5.1.0'
    implementation 'org.slf4j:slf4j-simple:2.0.9'
}
```

## ✨ v2.3.0の全機能を継承

v2.3.1は、v2.3.0の全機能をそのまま継承しています：

- ✅ Bungeecordプラグイン対応
- ✅ マルチモジュール構造
- ✅ ネットワーク統計表示
- ✅ Plan Player Analytics連携

## 📦 ビルド成果物

```
build/libs/
├── MiningTracker-Bukkit-2.3.1.jar   # バックエンドサーバー用
└── MiningTracker-Bungee-2.3.1.jar   # Bungeecordプロキシ用
```

## 🚀 使用方法

v2.3.0と同じインストール方法で使用できます。

### バックエンドサーバー
```bash
cp MiningTracker-Bukkit-2.3.1.jar /path/to/server/plugins/
```

### Bungeecordプロキシ（オプション）
```bash
cp MiningTracker-Bungee-2.3.1.jar /path/to/bungeecord/plugins/
```

## 🔄 v2.3.0からの移行

**必要な作業**: なし

単純にJARファイルを置き換えるだけで動作します。

```bash
# 古いバージョンを削除
rm MiningTracker-Bukkit-2.3.0.jar
rm MiningTracker-Bungee-2.3.0.jar

# 新バージョンをインストール
cp MiningTracker-Bukkit-2.3.1.jar server/plugins/
cp MiningTracker-Bungee-2.3.1.jar bungeecord/plugins/
```

## 📊 技術詳細

### Gradleマルチモジュール依存関係

#### 問題のあった構成
```gradle
// miningtracker-common/build.gradle
dependencies {
    implementation 'com.zaxxer:HikariCP:5.1.0'  // 推移的でない
}

// miningtracker-bukkit/build.gradle
dependencies {
    implementation project(':miningtracker-common')
    // HikariCPが自動的に含まれない！
}
```

#### 修正後の構成
```gradle
// miningtracker-common/build.gradle
dependencies {
    implementation 'com.zaxxer:HikariCP:5.1.0'
}

// miningtracker-bukkit/build.gradle
dependencies {
    implementation project(':miningtracker-common')
    implementation 'com.zaxxer:HikariCP:5.1.0'  // 明示的に宣言
}
```

### 依存関係の種類

| 種類 | 推移性 | 用途 |
|------|-------|------|
| `api` | ✅ 推移的 | 公開API |
| `implementation` | ❌ 非推移的 | 内部実装 |
| `compileOnly` | ❌ 非推移的 | コンパイル時のみ |

## ⚠️ 既知の問題

現時点では既知の問題はありません。

## 🙏 謝辞

ビルドエラーの報告をいただき、ありがとうございました。

## 🔗 参考リンク

- **v2.3.0リリースノート**: [RELEASE_NOTES_v2.3.0.md](RELEASE_NOTES_v2.3.0.md)
- **v2.2.0リリースノート**: [RELEASE_NOTES_v2.2.0_BUNGEE.md](RELEASE_NOTES_v2.2.0_BUNGEE.md)
- **Bungeecordセットアップ**: [BUNGEECORD_SETUP.md](BUNGEECORD_SETUP.md)
- **変更履歴**: [CHANGELOG.md](CHANGELOG.md)
- **バージョン情報**: [VERSION.md](VERSION.md)

## 💬 サポート

問題が発生した場合は、GitHubのIssuesページで報告してください。

---

**MiningTracker v2.3.1** - Dependency Fix Release  
© 2026 kubota6646

正常なビルドをお楽しみください！
