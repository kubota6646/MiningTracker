# MiningTracker v2.4.0 リリースノート

**リリース日**: 2026-02-07

## 🎉 概要

v2.4.0は、v2.3.8とv2.3.9で解決できなかったSLF4Jエラーを根本的に解決するメジャーアップデートです。slf4j-simpleからslf4j-jdk14への切り替えにより、BukkitのJava Util Loggingシステムとネイティブに統合されます。

## 🐛 解決した問題

### SLF4Jエラーの完全解決

**報告されていたエラー:**
```
[ERROR]: [MiningTracker] [STDERR] SLF4J: No SLF4J providers were found.
[ERROR]: [MiningTracker] [STDERR] SLF4J: Defaulting to no-operation (NOP) logger implementation
[WARN]: Nag author(s): '[kubota6646]' of 'MiningTracker' about their usage of System.out/err.print.
```

**これまでの試み:**
- **v2.3.8**: SLF4Jリロケーション削除 + `mergeServiceFiles()` → エラー継続
- **v2.3.9**: `slf4j-simple`を明示的に追加 → エラー継続

**根本原因:**
- `slf4j-simple`: スタンドアロンアプリケーション向けのSLF4J実装
- **Bukkitの要件**: Java Util Logging（JUL）を使用
- **競合**: slf4j-simpleとBukkitのJULが競合し、SLF4Jプロバイダーが正しく検出されない
- **System.out/errの警告**: slf4j-simpleが内部的にSystem.out/errを使用

## ✅ 実施した修正

### 1. slf4j-jdk14への切り替え

**全モジュール (common/bukkit/bungee):**
```gradle
// Before (v2.3.9まで)
implementation 'org.slf4j:slf4j-simple:2.0.9'

// After (v2.4.0)
implementation 'org.slf4j:slf4j-jdk14:2.0.9'
```

### 2. SLF4Jリロケーションの削除

**bukkit & bungee モジュール:**
```gradle
shadowJar {
    // HikariCPとMySQLのみリロケート
    relocate 'com.zaxxer.hikari', 'com.kubota6646.miningtracker.lib.hikari'
    relocate 'com.mysql', 'com.kubota6646.miningtracker.lib.mysql'
    
    // SLF4Jはリロケートしない - slf4j-jdk14がJULにブリッジする必要がある
    // relocate 'org.slf4j', ... <- 削除
    
    // サービスプロバイダーファイルをマージ
    mergeServiceFiles()
}
```

## 📋 slf4j-jdk14の技術的メリット

### SLF4J → JUL ブリッジ

**動作フロー:**
```
HikariCP (SLF4J) 
    ↓
slf4j-jdk14 (ブリッジ)
    ↓
Java Util Logging (JUL)
    ↓
Bukkitのロガー
    ↓
コンソール/ログファイル
```

### 利点

1. **ネイティブ統合**
   - BukkitのJava Util Loggingシステムと完全統合
   - Paper/Spigotの既存のログ設定を使用
   - 追加設定不要

2. **競合なし**
   - slf4j-simpleのような競合が発生しない
   - System.out/errを使用しない
   - Bukkitの警告が発生しない

3. **統一されたログ**
   - HikariCPのログがBukkitログに統合
   - 一貫したログフォーマット
   - ログレベルの統一管理

4. **パフォーマンス**
   - オーバーヘッドが最小
   - Bukkitのログバッファリングを活用

## 🎯 効果

### ユーザーへの影響

✅ **SLF4Jエラーメッセージが完全に消える**
- 「No SLF4J providers were found」エラー解消
- 「Defaulting to no-operation (NOP) logger implementation」警告解消

✅ **System.out/err警告が消える**
- Bukkitの「Nag author」警告解消
- プラグインログがクリーン

✅ **HikariCPログが正常に出力される**
- データベース接続ログが表示される
- 問題のトラブルシューティングが容易

✅ **全環境で動作**
- Paper/Spigot: ✅ 完全対応
- Bungeecord: ✅ 完全対応
- Waterfall: ✅ 完全対応

## 📦 成果物

### ビルド成果物

- **MiningTracker-Bukkit-2.4.0.jar**
  - Bukkitサーバー用プラグイン
  - プレイヤー統計、サーバー統計、ネットワーク統計すべて表示

- **MiningTracker-Bungee-2.4.0.jar**
  - Bungeecordプロキシ用プラグイン
  - ネットワーク統計のみ表示

### インストール

**シングルサーバー:**
```
plugins/
└── MiningTracker-Bukkit-2.4.0.jar
```

**Bungeecordネットワーク (方法1 - 推奨):**
```
[Backend Server 1] plugins/
└── MiningTracker-Bukkit-2.4.0.jar

[Backend Server 2] plugins/
└── MiningTracker-Bukkit-2.4.0.jar

[Bungeecord] (不要)
```

**Bungeecordネットワーク (方法2 - オプション):**
```
[Backend Server 1] plugins/
└── MiningTracker-Bukkit-2.4.0.jar

[Backend Server 2] plugins/
└── MiningTracker-Bukkit-2.4.0.jar

[Bungeecord] plugins/
└── MiningTracker-Bungee-2.4.0.jar  <- ネットワーク統計のみ
```

## 🔧 アップグレード手順

### v2.3.x からのアップグレード

1. **サーバー停止**
   ```bash
   /stop
   ```

2. **古いJAR削除**
   ```bash
   rm plugins/MiningTracker-Bukkit-2.3.*.jar
   rm plugins/MiningTracker-Bungee-2.3.*.jar  # Bungeecordの場合
   ```

3. **新しいJARインストール**
   ```bash
   # Bukkitサーバー
   cp MiningTracker-Bukkit-2.4.0.jar plugins/
   
   # Bungeecord (オプション)
   cp MiningTracker-Bungee-2.4.0.jar plugins/
   ```

4. **サーバー起動**
   ```bash
   # サーバー起動
   ```

5. **ログ確認**
   - SLF4Jエラーが表示されないことを確認
   - HikariCPログが正常に表示されることを確認
   ```
   [INFO]: [MiningTracker] Enabling MiningTracker v2.4.0
   [INFO]: [MiningTracker] MySQLデータベースに接続しました（HikariCP使用）。
   [INFO]: [MiningTracker] 接続プール設定: 最大=10, 最小=2
   ```

### 設定ファイル

- **config.yml**: 変更不要（互換性あり）
- **messages.yml**: 変更不要（互換性あり）
- **データベース**: マイグレーション不要

## 🆕 技術的変更

### 依存関係の変更

| 依存関係 | v2.3.9 | v2.4.0 |
|---------|--------|--------|
| SLF4J API | 2.0.9 | 2.0.9 |
| SLF4J実装 | slf4j-simple:2.0.9 | slf4j-jdk14:2.0.9 ⭐ |
| HikariCP | 5.1.0 | 5.1.0 |
| MySQL Connector | 8.3.0 | 8.3.0 |

### ビルド設定の変更

- **SLF4Jリロケーション削除**: slf4j-jdk14がJULにブリッジする必要があるため
- **mergeServiceFiles()追加**: SLF4Jサービスプロバイダーファイルを統合

## 📚 技術参考情報

### SLF4Jバインディングの選択

| バインディング | 用途 | Bukkit互換性 |
|--------------|------|-------------|
| slf4j-simple | スタンドアロンアプリ | ❌ 競合 |
| slf4j-jdk14 | JUL統合 | ✅ 最適 |
| slf4j-log4j12 | Log4j 1.x | ⚠️ 非推奨 |
| logback-classic | 高機能ロギング | ⚠️ 複雑 |

### なぜslf4j-jdk14なのか

1. **Bukkitの標準**: BukkitはJULを使用
2. **軽量**: 追加の依存関係不要
3. **シンプル**: 設定不要で動作
4. **互換性**: すべてのBukkit系サーバーで動作

## 🔍 トラブルシューティング

### まだSLF4Jエラーが出る場合

1. **JARファイルの確認**
   ```bash
   ls -la plugins/MiningTracker*.jar
   # -rw-r--r-- 1 minecraft minecraft ... MiningTracker-Bukkit-2.4.0.jar
   ```

2. **古いJARの削除確認**
   ```bash
   # 2.3.x のJARが残っていないか確認
   ```

3. **キャッシュクリア**
   ```bash
   rm -rf cache/
   rm -rf world/playerdata/.*.dat.tmp
   ```

4. **ログの確認**
   ```bash
   tail -f logs/latest.log | grep -i slf4j
   ```

### HikariCPログが表示されない場合

1. **ログレベルの確認**
   - Bukkitのログレベル設定を確認
   - `bukkit.yml`または`paper.yml`

2. **データベース接続の確認**
   - `config.yml`の設定を確認
   - MySQLサーバーが起動しているか確認

## 🙏 謝辞

この問題の報告と継続的なフィードバックをいただいた皆様に感謝いたします。

## 📞 サポート

問題が発生した場合:
1. GitHubのIssueを確認
2. 新しいIssueを作成
3. ログファイルを添付

---

**MiningTracker v2.4.0** - SLF4J完全修正版
