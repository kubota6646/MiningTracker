# MiningTracker v2.3.8 リリースノート

**リリース日**: 2026-02-07  
**リリースタイプ**: バグ修正リリース

## 📋 概要

v2.3.8は、SLF4Jプロバイダーが見つからないエラーを修正するバグ修正リリースです。HikariCPのログ出力に必要なSLF4Jサービスプロバイダーが正しく機能するよう、shadowJar設定を改善しました。

## 🐛 修正された問題

### SLF4J Provider Not Found エラー

#### 症状
```log
[04:32:49 ERROR]: [MiningTracker] [STDERR] SLF4J: No SLF4J providers were found.
[04:32:49 ERROR]: [MiningTracker] [STDERR] SLF4J: Defaulting to no-operation (NOP) logger implementation
[04:32:49 ERROR]: [MiningTracker] [STDERR] SLF4J: See https://www.slf4j.org/codes.html#noProviders for further details.
```

MySQL接続時にHikariCPがSLF4Jを使用してログを出力しようとするが、SLF4Jプロバイダーが見つからずエラーが発生していました。

#### 根本原因

1. **SLF4Jのリロケーション**: shadowJar設定で`relocate 'org.slf4j', 'lib.slf4j'`を行っていた
2. **サービスプロバイダーの破損**: SLF4J 2.xはJava ServiceLoader APIを使用してプロバイダーを検索
3. **パッケージ名の依存**: `META-INF/services/org.slf4j.spi.SLF4JServiceProvider`はパッケージ名が`org.slf4j.*`である必要がある
4. **リロケーションの影響**: パッケージ名が変更されるとServiceLoaderがプロバイダーを見つけられなくなる

#### 解決方法

**shadowJar設定の変更** (bukkit & bungee両モジュール):

```gradle
shadowJar {
    archiveClassifier.set('')
    from(sourceSets.main.output)
    
    // HikariCPとMySQLのみリロケート（依存関係の競合回避）
    relocate 'com.zaxxer.hikari', 'lib.hikari'
    relocate 'com.mysql', 'lib.mysql'
    
    // SLF4Jはリロケートしない（サービスプロバイダー機能が必要）
    // relocate 'org.slf4j', 'lib.slf4j' <- 削除
    
    // サービスプロバイダーファイルを統合
    mergeServiceFiles()
}
```

**変更点**:
- ✅ SLF4Jリロケーションを削除
- ✅ `mergeServiceFiles()`を追加してサービスプロバイダーファイルを統合
- ✅ HikariCPとMySQLのリロケーションは維持（依存関係の競合回避）

## 🔧 技術的詳細

### SLF4J 2.xのサービスプロバイダーシステム

SLF4J 2.xは、Java ServiceLoader APIを使用してロギングプロバイダーを動的に検出します：

1. **プロバイダー検出**: `META-INF/services/org.slf4j.spi.SLF4JServiceProvider`ファイルを検索
2. **パッケージ名依存**: プロバイダークラスは`org.slf4j.*`パッケージに存在する必要がある
3. **リロケーションの影響**: パッケージ名が変更されるとServiceLoaderが機能しなくなる

### mergeServiceFiles()の役割

Shadow pluginの`mergeServiceFiles()`は以下の処理を行います：

- 複数のJARファイルから`META-INF/services/*`ファイルを収集
- 同名のサービスファイルをマージして統合
- 最終的なJARに正しいサービスプロバイダー情報を含める

これにより、slf4j-simpleのプロバイダー情報が最終JARに正しく含まれます。

### なぜHikariCPとMySQLはリロケートするのか

依存関係の競合を避けるため、以下のライブラリはリロケートを維持：

- **HikariCP** (`com.zaxxer.hikari` → `lib.hikari`): 他のプラグインとの競合回避
- **MySQL Connector** (`com.mysql` → `lib.mysql`): 他のプラグインとの競合回避

SLF4Jは標準的なロギングファサードであり、多くのライブラリと共有されるため、リロケートしない方が安全です。

## ✅ 影響と効果

### 修正後の動作

1. **SLF4Jエラーメッセージが解消**: プラグイン起動時にSLF4J関連のエラーが表示されなくなります
2. **HikariCPログの正常出力**: データベース接続プールのログが正しく出力されます
3. **Bukkitサーバーで正常動作**: Paper/Spigot環境で問題なく動作します
4. **Bungeecordで正常動作**: Bungeecord環境でも問題なく動作します

### 期待されるログ出力

修正後は、SLF4Jエラーが表示されず、HikariCPのログが正常に出力されます：

```log
[04:32:49 INFO]: [MiningTracker] Enabling MiningTracker v2.3.8
[04:32:49 INFO]: HikariPool-1 - Starting...
[04:32:49 INFO]: HikariPool-1 - Start completed.
[04:32:50 INFO]: [MiningTracker] データベース接続が確立されました
```

## 📦 アップグレード方法

### 必要な手順

1. **サーバー停止**: Minecraftサーバーを停止します
2. **JARファイル置き換え**:
   - Bukkitサーバー: `plugins/MiningTracker-Bukkit-2.3.7.jar` → `plugins/MiningTracker-Bukkit-2.3.8.jar`
   - Bungeecord: `plugins/MiningTracker-Bungee-2.3.7.jar` → `plugins/MiningTracker-Bungee-2.3.8.jar`
3. **サーバー起動**: サーバーを起動してログを確認
4. **動作確認**: SLF4Jエラーが表示されないことを確認

### 設定ファイル

- ✅ **設定変更不要**: config.ymlの変更は不要です
- ✅ **データベース互換性**: 既存のデータベーススキーマと完全互換

### ダウングレード

もし問題が発生した場合は、v2.3.7にダウングレードできます：

```bash
# Bukkitサーバー
mv plugins/MiningTracker-Bukkit-2.3.8.jar plugins/MiningTracker-Bukkit-2.3.8.jar.bak
# 以前のバージョンを戻す
```

## 🎯 互換性

- **Minecraft**: 1.21.x
- **Java**: 21以降必須
- **Spigot/Paper**: 1.21.x
- **Bungeecord**: 最新版
- **Plan**: 5.6以降

## 📚 関連リンク

- [GitHub リポジトリ](https://github.com/kubota6646/MiningTracker)
- [SLF4J 公式ドキュメント](https://www.slf4j.org/)
- [HikariCP ドキュメント](https://github.com/brettwooldridge/HikariCP)

## 🙏 謝辞

このリリースは、ユーザーからの詳細なエラーレポートにより実現しました。バグ報告に感謝します！

---

**次のリリース予定**: v2.4.0 - 新機能追加予定
