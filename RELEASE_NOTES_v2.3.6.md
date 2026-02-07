# MiningTracker v2.3.6 リリースノート

**リリース日**: 2026-02-07

## 🎯 概要

v2.3.6は、Java 21環境でのビルドエラーを解消するための重要なアップデートです。Shadow pluginとGradleを最新版に更新し、Java 21バイトコードの完全サポートを実現しました。

## 🐛 修正された問題

### shadowJar Java 21互換性問題

#### 症状
```
> Task :miningtracker-bukkit:shadowJar FAILED

org.gradle.api.GradleException: Could not add file 'RankingCommand.class' to ZIP 'MiningTracker-Bukkit-2.3.5.jar'.

Caused by: java.lang.IllegalArgumentException: Unsupported class file major version 65
```

ビルド時にshadowJarタスクが失敗し、JARファイルが生成されない問題が発生していました。

#### 根本原因

1. **Java 21バイトコード**: 
   - プロジェクトはJava 21でコンパイル（class file major version 65）
   - 最新のJavaバージョンを使用

2. **Shadow plugin 8.1.1の制限**:
   - 内部で使用しているASMライブラリがJava 21バイトコードを完全にサポートしていない
   - Java 21で生成されたクラスファイルの処理に失敗

3. **ビルドプロセスの中断**:
   - shadowJarタスクでエラーが発生
   - JARファイルが生成されない
   - プラグインのデプロイができない

## 🔧 実施した修正

### 1. Shadow Plugin更新

**変更内容**:
```gradle
// Before
id 'com.github.johnrengelman.shadow' version '8.1.1'

// After
id 'com.github.johnrengelman.shadow' version '8.1.7'
```

**改善点**:
- ✅ Java 21完全サポート
- ✅ ASMライブラリ最新化（Java 21バイトコード対応）
- ✅ 多数のバグ修正とパフォーマンス改善
- ✅ 安定性向上

### 2. Gradle更新

**変更内容**:
```properties
// Before
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip

// After
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
```

**改善点**:
- ✅ Java 21サポート強化
- ✅ ビルドパフォーマンス向上
- ✅ セキュリティ修正
- ✅ 依存関係解決の改善

## 📊 技術的詳細

### Java バージョンとClass File Major Version

| Javaバージョン | Class File Major Version |
|--------------|-------------------------|
| Java 8       | 52                      |
| Java 11      | 55                      |
| Java 17      | 61                      |
| **Java 21**  | **65** ← 今回のエラー原因 |

### ASM（Abstract Syntax Model）

Shadow pluginは内部でASMライブラリを使用してJavaバイトコードを処理します：
- **ASMの役割**: Javaクラスファイルの読み取り、変更、生成
- **バージョン依存**: 各ASMバージョンは特定のJavaバージョンまでサポート
- **Shadow 8.1.7**: 最新のASMバージョンを使用し、Java 21を完全サポート

### 影響範囲

#### 影響を受けたモジュール
- ✅ `miningtracker-bukkit` - shadowJarでビルド
- ✅ `miningtracker-bungee` - shadowJarでビルド

#### 影響を受けなかったモジュール
- ⚪ `miningtracker-common` - 通常のjarタスクのみ使用

## ✨ 効果

### ビルド成功

```
> Task :miningtracker-bukkit:shadowJar
> Task :miningtracker-bukkit:build
> Task :miningtracker-bungee:shadowJar
> Task :miningtracker-bungee:build

BUILD SUCCESSFUL
```

### 生成されるJARファイル

1. **MiningTracker-Bukkit-2.3.6.jar**
   - Bukkit/Spigot/Paperサーバー用
   - すべての依存関係を含む（shadowed）
   - プレイヤー統計、サーバー統計、ネットワーク統計対応

2. **MiningTracker-Bungee-2.3.6.jar**
   - Bungeecordプロキシ用
   - すべての依存関係を含む（shadowed）
   - ネットワーク統計のみ対応

## 🎯 検証項目

ビルド後、以下の点を確認してください：

### ビルド確認
```bash
./gradlew clean build
```

期待される結果：
- ✅ すべてのタスクが成功
- ✅ `miningtracker-bukkit/build/libs/MiningTracker-Bukkit-2.3.6.jar` が生成
- ✅ `miningtracker-bungee/build/libs/MiningTracker-Bungee-2.3.6.jar` が生成

### JARファイルの検証
```bash
# JARファイルの内容を確認
jar tf miningtracker-bukkit/build/libs/MiningTracker-Bukkit-2.3.6.jar | head -20

# plugin.ymlの存在確認
jar tf miningtracker-bukkit/build/libs/MiningTracker-Bukkit-2.3.6.jar | grep plugin.yml

# 依存関係のリロケーション確認
jar tf miningtracker-bukkit/build/libs/MiningTracker-Bukkit-2.3.6.jar | grep "lib/hikari"
```

### サーバーでの動作確認
1. Bukkitサーバーに`MiningTracker-Bukkit-2.3.6.jar`を配置
2. サーバー起動
3. プラグインが正常にロードされることを確認
4. コマンドが動作することを確認

## 📝 アップグレード手順

### 既存ユーザー向け

#### 1. ビルド環境の更新（オプション）

開発者の場合、Gradle Wrapperを更新：
```bash
./gradlew wrapper --gradle-version 8.9
```

#### 2. プラグインの更新

**Bukkitサーバー**:
1. サーバー停止
2. 古い`MiningTracker-Bukkit-x.x.x.jar`を削除
3. 新しい`MiningTracker-Bukkit-2.3.6.jar`を配置
4. サーバー起動

**Bungeecordプロキシ**:
1. プロキシ停止
2. 古い`MiningTracker-Bungee-x.x.x.jar`を削除
3. 新しい`MiningTracker-Bungee-2.3.6.jar`を配置
4. プロキシ起動

#### 3. 設定ファイルの確認

設定ファイル（`config.yml`）は**変更不要**です。既存の設定がそのまま使用されます。

## 🔄 互換性

### 前バージョンとの互換性
- ✅ **完全互換**: v2.3.5以前からのアップグレードは問題なし
- ✅ **データベース互換**: データベーススキーマの変更なし
- ✅ **設定ファイル互換**: config.ymlの変更不要
- ✅ **API互換**: 外部プラグインからの使用も問題なし

### 動作環境
- **Minecraft**: 1.21.x
- **Java**: 21以降（必須）
- **Spigot/Paper**: 1.21以降
- **Bungeecord**: 1.21対応版
- **Plan**: 5.6.2959以降（オプション）

## 🆕 新機能・改善（v2.3.0以降の累積）

このバージョンには、以下の累積改善も含まれています：

### v2.3.5の改善
- Plan統計表示のNULL処理修正
- サーバー総採掘数とネットワーク総採掘数の表示問題解消

### v2.3.4の改善
- サーバー間リアルタイム同期修正
- HikariCP autoCommit設定の最適化

### v2.3.3の改善
- shadowJar設定改善
- 依存関係リロケーション追加
- トラブルシューティングガイド追加

### v2.3.2の改善
- Bungeecord APIバージョン修正

### v2.3.1の改善
- HikariCP依存関係修正

### v2.3.0の改善
- マルチモジュールプロジェクト化
- ビルドエラー修正

## 🐛 既知の問題

現時点で既知の問題はありません。

問題を発見した場合は、GitHubのIssuesで報告してください。

## 🤝 貢献

バグ報告、機能提案、プルリクエストを歓迎します！

## 📄 ライセンス

MIT License

## 🔗 リンク

- **リポジトリ**: https://github.com/kubota6646/MiningTracker
- **Issues**: https://github.com/kubota6646/MiningTracker/issues
- **Wiki**: 準備中

---

**MiningTracker v2.3.6** をお楽しみください！ 🎉
