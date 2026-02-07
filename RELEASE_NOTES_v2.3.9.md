# MiningTracker v2.3.9 リリースノート

**リリース日**: 2026-02-07

## 🎉 概要 / Overview

v2.3.9は、v2.3.8で修正を試みたが継続していたSLF4Jエラーを完全に解決するパッチリリースです。

## 🐛 修正された問題 / Bug Fixes

### SLF4J依存関係の明示化

#### 問題 / Problem
v2.3.8でSLF4Jリロケーションを削除してmergeServiceFiles()を追加したが、エラーが継続:
```
[ERROR]: [MiningTracker] [STDERR] SLF4J: No SLF4J providers were found.
[ERROR]: [MiningTracker] [STDERR] SLF4J: Defaulting to no-operation (NOP) logger implementation
[ERROR]: [MiningTracker] [STDERR] SLF4J: See https://www.slf4j.org/codes.html#noProviders for further details.
```

#### 根本原因 / Root Cause
- commonモジュールで`slf4j-simple:2.0.9`を宣言
- しかしbukkit/bungeeモジュールで明示的に宣言されていなかった
- 推移的依存関係として含まれるべきだったが、shadowJarで正しく処理されなかった
- 各モジュールがshadowJarを作成する際、commonから推移的に伝わる依存関係が含まれていなかった

#### 解決方法 / Solution
bukkit/bungee両モジュールの`build.gradle`に`slf4j-simple`を明示的に追加:

**miningtracker-bukkit/build.gradle:**
```gradle
dependencies {
    implementation project(':miningtracker-common')
    compileOnly 'io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT'
    compileOnly 'com.djrapitops:plan-api:5.6.3054'
    
    // Database dependencies
    implementation 'com.mysql:mysql-connector-j:8.3.0'
    implementation 'com.zaxxer:HikariCP:5.1.0'
    implementation 'org.slf4j:slf4j-simple:2.0.9'  // 明示的に追加
}
```

**miningtracker-bungee/build.gradle:**
```gradle
dependencies {
    implementation project(':miningtracker-common')
    compileOnly 'net.md-5:bungeecord-api:1.21-R0.4'
    compileOnly 'com.djrapitops:plan-api:5.6.3054'
    
    // Database dependencies
    implementation 'com.mysql:mysql-connector-j:8.3.0'
    implementation 'com.zaxxer:HikariCP:5.1.0'
    implementation 'org.slf4j:slf4j-simple:2.0.9'  // 明示的に追加
}
```

#### 影響 / Impact
✅ **SLF4Jエラーが完全に解消**
✅ **HikariCPのログが正常に出力される**
✅ **Bukkitサーバー・Bungeecord両方で正常動作**

## 📋 技術的詳細 / Technical Details

### なぜcommonモジュールの依存関係だけでは不十分だったのか

Gradleのマルチモジュールプロジェクトにおいて、`implementation`依存関係は推移的ですが、shadowJarプラグインは以下の理由で期待通りに動作しないことがあります:

1. **shadowJarのスコープ**: shadowJarは現在のプロジェクトの`dependencies`ブロックで宣言された`implementation`依存関係を含める
2. **プロジェクト依存関係の扱い**: `implementation project(':miningtracker-common')`は、commonモジュールのコンパイル済みクラスは含めるが、commonの依存関係は自動的には含めない
3. **明示的な宣言が必要**: shadowJarに確実に含めるには、各モジュールで明示的に依存関係を宣言する必要がある

### v2.3.8との違い

| 項目 | v2.3.8 | v2.3.9 |
|-----|--------|--------|
| SLF4Jリロケーション | 削除済み | 削除済み |
| mergeServiceFiles() | 追加済み | 追加済み |
| commonのslf4j-simple | あり | あり |
| bukkitのslf4j-simple | **なし** | **明示的に追加** ⭐ |
| bungeeのslf4j-simple | **なし** | **明示的に追加** ⭐ |

## 🔄 アップグレード手順 / Upgrade Instructions

### シングルサーバー（Bukkit/Paper）
1. サーバーを停止
2. `plugins/`の古い`MiningTracker-Bukkit-2.3.8.jar`を削除
3. 新しい`MiningTracker-Bukkit-2.3.9.jar`を配置
4. サーバーを起動
5. エラーログを確認（SLF4Jエラーが出ないことを確認）

### Bungeecordネットワーク
1. すべてのサーバーとBungeecordを停止
2. Bungeecordプロキシ（使用している場合）:
   - 古い`MiningTracker-Bungee-2.3.8.jar`を削除
   - 新しい`MiningTracker-Bungee-2.3.9.jar`を配置
3. 各バックエンドサーバー:
   - 古い`MiningTracker-Bukkit-2.3.8.jar`を削除
   - 新しい`MiningTracker-Bukkit-2.3.9.jar`を配置
4. すべてのサーバーとBungeecordを起動
5. エラーログを確認

## ⚠️ 既知の問題 / Known Issues

現在、既知の問題はありません。

## 🙏 謝辞 / Acknowledgments

バグ報告をしていただいたユーザーの皆様に感謝いたします。

## 📚 関連ドキュメント / Related Documentation

- [README.md](README.md) - インストール方法と基本的な使い方
- [BUNGEECORD_SETUP.md](BUNGEECORD_SETUP.md) - Bungeecordセットアップガイド
- [CHANGELOG.md](CHANGELOG.md) - 変更履歴
- [VERSION.md](VERSION.md) - バージョン管理

## 🔗 リンク / Links

- [GitHub Repository](https://github.com/kubota6646/MiningTracker)
- [Issue Tracker](https://github.com/kubota6646/MiningTracker/issues)

---

**Full Changelog**: v2.3.8...v2.3.9
