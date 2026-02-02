# Gradle依存関係修正ガイド

## 問題の詳細

### エラーメッセージ
```
Could Not Resolve com.djrapitops:plan:5.6.3027 for MiningTracker:main

MiningTracker:main: Could not find com.djrapitops:plan:5.6.3027.
Searched in the following locations:
  - https://repo.maven.apache.org/maven2/com/djrapitops/plan/5.6.3027/plan-5.6.3027.pom
  - https://repo.papermc.io/repository/maven-public/com/djrapitops/plan/5.6.3027/plan-5.6.3027.pom
  - https://repo.playeranalytics.net/releases/com/djrapitops/plan/5.6.3027/plan-5.6.3027.pom
```

### 原因分析

1. **誤った依存関係座標**
   - 使用していた: `com.djrapitops:plan:5.6.3027`
   - この形式のアーティファクトは存在しない

2. **誤ったリポジトリ**
   - 使用していた: `https://repo.playeranalytics.net/releases`
   - このリポジトリは利用できないか、該当バージョンが存在しない

3. **Plan配布方法の誤解**
   - Planは独自のMavenリポジトリではなく、**JitPack**経由で配布されている

## 解決方法

### 正しい依存関係の設定

#### build.gradle

```gradle
repositories {
    mavenCentral()
    maven {
        name = 'papermc'
        url = 'https://repo.papermc.io/repository/maven-public/'
    }
    maven {
        name = 'jitpack'
        url = 'https://jitpack.io'
    }
}

dependencies {
    compileOnly 'org.spigotmc:spigot-api:1.21-R0.1-SNAPSHOT'
    compileOnly 'com.github.plan-player-analytics:Plan:5.6.2959'
    implementation 'com.mysql:mysql-connector-j:8.3.0'
    implementation 'com.zaxxer:HikariCP:5.1.0'
    implementation 'org.slf4j:slf4j-simple:2.0.9'
}
```

### 重要なポイント

#### 1. JitPackリポジトリの追加
```gradle
maven {
    name = 'jitpack'
    url = 'https://jitpack.io'
}
```
- PlanはJitPack経由で配布されている
- JitPackはGitHubリリースをMavenアーティファクトとして提供

#### 2. 正しいgroupIdとartifactId
```gradle
compileOnly 'com.github.plan-player-analytics:Plan:5.6.2959'
```
- **groupId**: `com.github.plan-player-analytics`
- **artifactId**: `Plan`
- **version**: GitHubタグ名（例: `5.6.2959`）

#### 3. compileOnlyスコープ
- Planはランタイムで提供される（サーバーにインストール済み）
- プラグインJARに含める必要はない
- `implementation`ではなく`compileOnly`を使用

## Plan APIのバージョン管理

### バージョンの確認方法

1. **GitHubタグページ**
   - URL: https://github.com/plan-player-analytics/Plan/tags
   - 最新のタグが最新バージョン

2. **JitPackページ**
   - URL: https://jitpack.io/#plan-player-analytics/Plan
   - 利用可能なバージョン一覧が表示される

3. **Maven Repository**
   - URL: https://mvnrepository.com/artifact/com.github.plan-player-analytics/Plan
   - バージョン履歴を確認可能

### 推奨バージョン

| バージョン | リリース日 | 推奨度 | 備考 |
|-----------|-----------|-------|------|
| 5.6.2959 | 2025-01-17 | ⭐⭐⭐ | 最新安定版 |
| 5.6.2850 | 2024-04-20 | ⭐⭐ | 前バージョン |
| 5.6.2614 | - | ⭐ | 旧バージョン |

### バージョンの更新

build.gradleのバージョン番号を変更するだけ:
```gradle
compileOnly 'com.github.plan-player-analytics:Plan:5.6.2959'
```

Gradle同期を実行すると、新しいバージョンがダウンロードされます。

## Plan API 2つの依存関係の違い

### 1. 完全なPlanプラグイン（推奨）
```gradle
compileOnly 'com.github.plan-player-analytics:Plan:5.6.2959'
```
- **内容**: Plan全体のクラス
- **用途**: DataExtension、Query API、PageExtension
- **サイズ**: 大きい（プラグイン全体）
- **推奨**: ほとんどの場合これを使用

### 2. Plan API専用（限定的）
```gradle
compileOnly 'com.djrapitops:plan-api:5.2-R0.1'
```
- **内容**: API部分のみ
- **用途**: Query APIのみ使用する場合
- **サイズ**: 小さい
- **制限**: DataExtension等は含まれない
- **注意**: 古いバージョン（5.2）で更新が少ない

MiningTrackerはDataExtensionを使用しているため、完全なPlanプラグインを依存関係として使用します。

## トラブルシューティング

### 問題1: JitPackが応答しない

**症状:**
```
Could not GET 'https://jitpack.io/...'
```

**解決方法:**
1. インターネット接続を確認
2. プロキシ設定を確認（必要な場合）
3. Gradle Daemonを再起動: `./gradlew --stop`
4. キャッシュをクリア: `./gradlew clean --refresh-dependencies`

### 問題2: 依然として解決できない

**症状:**
```
Could not resolve com.github.plan-player-analytics:Plan:5.6.2959
```

**解決方法:**
1. バージョン番号を確認（GitHubタグと一致するか）
2. リポジトリの順序を確認（JitPackが最後）
3. Gradle設定を確認（settings.gradle）
4. ビルドキャッシュをクリア

### 問題3: NoClassDefFoundError

**症状:**
```
java.lang.NoClassDefFoundError: com/djrapitops/plan/extension/DataExtension
```

**解決方法:**
1. Planプラグインがサーバーにインストールされているか確認
2. plugin.ymlに`softdepend: [Plan]`があるか確認
3. Planバージョンが5.6以降か確認

## Maven設定（参考）

Mavenを使用する場合の設定:

```xml
<repositories>
    <repository>
        <id>jitpack</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.plan-player-analytics</groupId>
        <artifactId>Plan</artifactId>
        <version>5.6.2959</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## 公式リソース

### Plan関連
- **GitHub**: https://github.com/plan-player-analytics/Plan
- **Wiki**: https://github.com/plan-player-analytics/Plan/wiki
- **API Documentation**: https://github.com/plan-player-analytics/Plan/wiki/APIv5
- **Javadocs**: https://plan-player-analytics.github.io/Plan/api/

### JitPack
- **Plan on JitPack**: https://jitpack.io/#plan-player-analytics/Plan
- **JitPack Docs**: https://jitpack.io/docs/

### Maven Repository
- **Plan Artifacts**: https://mvnrepository.com/artifact/com.github.plan-player-analytics/Plan

## チェックリスト

ビルド設定を確認する際のチェックリスト:

- [ ] JitPackリポジトリが追加されている
- [ ] groupIdが`com.github.plan-player-analytics`である
- [ ] artifactIdが`Plan`である（大文字のP）
- [ ] バージョン番号がGitHubタグと一致する
- [ ] スコープが`compileOnly`である
- [ ] plugin.ymlに`softdepend: [Plan]`がある
- [ ] Gradleキャッシュをクリアした
- [ ] インターネット接続が正常

すべてチェックできたら、`./gradlew clean build`を実行してビルドが成功することを確認してください。

---

**更新日**: 2026-02-02  
**バージョン**: v2.1.1  
**対象**: MiningTracker Plan連携機能
