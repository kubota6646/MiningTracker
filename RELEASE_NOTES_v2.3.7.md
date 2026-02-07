# MiningTracker v2.3.7 リリースノート

**リリース日**: 2026-02-07

## 🎉 概要

v2.3.7では、Shadow pluginの移行を行い、ビルドエラーを解消しました。

## 🐛 修正された問題

### ビルドエラー: Shadow plugin not found

**症状:**
```
Plugin [id: 'com.github.johnrengelman.shadow', version: '8.1.7', apply: false] was not found in any of the following sources:
- Gradle Core Plugins
- Included Builds
- Plugin Repositories
```

**原因:**
- v2.3.6で指定したShadow plugin version 8.1.7が存在しない
- 旧プラグインID（`com.github.johnrengelman.shadow`）の最終版は8.1.1
- Java 21完全サポートには新しいプラグインIDが必要

**解決:**
Shadow pluginを新しいプラグインIDに移行し、Java 21完全サポート版に更新しました。

## 🔧 技術的変更

### Shadow Plugin移行

#### プラグインID変更
```gradle
// Before (旧プラグインID、メンテナンス終了)
plugins {
    id 'com.github.johnrengelman.shadow' version '8.1.7' apply false
}

// After (新プラグインID、現在のメンテナンス版)
plugins {
    id 'com.gradleup.shadow' version '8.3.3' apply false
}
```

#### subprojects設定更新
```gradle
// Before
subprojects {
    apply plugin: 'com.github.johnrengelman.shadow'
    // ...
}

// After
subprojects {
    apply plugin: 'com.gradleup.shadow'
    // ...
}
```

### Shadow Plugin 8.3.3の特徴

1. **Java 21完全サポート**
   - Class file major version 65（Java 21）対応
   - ASMライブラリ最新化

2. **Gradle互換性**
   - Gradle 8.3以上をサポート
   - 現在のGradle 8.9と完全互換

3. **組織移行**
   - 開発者: John Rengelman → GradleUp
   - プラグインID: `com.github.johnrengelman.shadow` → `com.gradleup.shadow`
   - GitHub: https://github.com/GradleUp/shadow

## 📋 変更ファイル

- `build.gradle`: Shadow pluginのIDとバージョン更新
- `VERSION.md`: v2.3.7エントリー追加
- `CHANGELOG.md`: v2.3.7変更履歴追加
- `README.md`: バージョン番号更新

## ✨ 影響と効果

### ビルドプロセス
✅ ビルドエラー完全解消  
✅ Java 21環境で安定動作  
✅ shadowJarタスク正常実行

### メンテナンス性
✅ 最新のShadow pluginで継続的メンテナンス  
✅ 将来のJava/Gradleバージョンアップに対応  
✅ セキュリティ修正・バグ修正の恩恵

## 📦 成果物

### Bukkit/Spigot/Paper用
- **ファイル名**: `MiningTracker-Bukkit-2.3.7.jar`
- **場所**: `miningtracker-bukkit/build/libs/`
- **用途**: バックエンドサーバーにインストール

### Bungeecord用
- **ファイル名**: `MiningTracker-Bungee-2.3.7.jar`
- **場所**: `miningtracker-bungee/build/libs/`
- **用途**: Bungeecordプロキシにインストール（オプション）

## 🔄 アップグレード手順

### 既存ユーザー（v2.3.6以前から）

1. **サーバー停止**
   ```bash
   # 各サーバーを停止
   ```

2. **JARファイル置き換え**
   ```bash
   # Bukkit/Spigot/Paperサーバー
   cd /path/to/server/plugins
   rm MiningTracker-Bukkit-2.3.6.jar
   # 新しいJARをダウンロード・配置
   wget https://github.com/kubota6646/MiningTracker/releases/download/v2.3.7/MiningTracker-Bukkit-2.3.7.jar
   
   # Bungeecord（使用している場合）
   cd /path/to/bungeecord/plugins
   rm MiningTracker-Bungee-2.3.6.jar
   wget https://github.com/kubota6646/MiningTracker/releases/download/v2.3.7/MiningTracker-Bungee-2.3.7.jar
   ```

3. **サーバー起動**
   ```bash
   # 各サーバーを起動
   ```

4. **動作確認**
   ```
   /mtr stats
   /mtr ranking
   ```

### 設定ファイル
- **変更不要**: 設定ファイルの互換性維持
- データベース構造も変更なし

## 🆕 新規ユーザー

[README.md](README.md)のインストール手順を参照してください。

## 🔍 既知の問題

現在、特に報告されている問題はありません。

## 📚 関連ドキュメント

- [README.md](README.md) - 基本的な使い方
- [BUNGEECORD_SETUP.md](BUNGEECORD_SETUP.md) - Bungeecordセットアップガイド
- [PLAN_INTEGRATION.md](PLAN_INTEGRATION.md) - Plan連携ガイド
- [VERSION.md](VERSION.md) - バージョン履歴
- [CHANGELOG.md](CHANGELOG.md) - 詳細な変更履歴

## 🙏 謝辞

このリリースは、以下のプロジェクトに依存しています：

- **Gradle**: https://gradle.org/
- **Shadow Plugin (GradleUp)**: https://github.com/GradleUp/shadow
- **HikariCP**: https://github.com/brettwooldridge/HikariCP
- **MySQL Connector/J**: https://dev.mysql.com/downloads/connector/j/
- **Spigot/Paper API**: https://www.spigotmc.org/
- **Bungeecord API**: https://github.com/SpigotMC/BungeeCord
- **Plan**: https://github.com/plan-player-analytics/Plan

## 📞 サポート

問題が発生した場合：

1. **GitHub Issues**: https://github.com/kubota6646/MiningTracker/issues
2. **Wiki**: プロジェクトWikiを確認
3. **ログ確認**: サーバーログでエラーメッセージを確認

---

**Happy Mining! 🪨⛏️**
