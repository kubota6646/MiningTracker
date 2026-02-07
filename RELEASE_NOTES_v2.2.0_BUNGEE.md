# MiningTracker v2.2.0 リリースノート - Bungeecordプラグイン対応

**リリース日**: 2026-02-07  
**バージョン**: 2.2.0  
**対応Minecraft**: 1.21.x  
**必須Java**: 21+

## 🎉 主な新機能

### Bungeecordプラグイン ⭐ NEW

MiningTracker v2.2.0では、**Bungeecordプロキシに直接インストール可能な新プラグイン**を追加しました！

#### 2つのインストール方法

**方法1: バックエンドサーバーのみ（推奨）**
- 従来通り、各バックエンドサーバーにMiningTracker-Bukkitをインストール
- すべての統計が表示される（プレイヤー、サーバー、ネットワーク）
- 最も簡単な構成

**方法2: Bungeecordプロキシにも（オプション）**
- Bungeecordプロキシに**MiningTracker-Bungee.jar**をインストール
- ネットワーク統計のみを表示（サーバー統計は非表示）
- プロキシレベルでのシンプルな統計表示

### Plan連携の違い

| インストール場所 | プレイヤー統計 | サーバー統計 | ネットワーク統計 |
|-----------------|--------------|------------|----------------|
| **Bukkit サーバー** | ✅ 表示 | ✅ 表示 | ✅ 表示 |
| **Bungee プロキシ** | ❌ 非表示 | ❌ 非表示 | ✅ 表示のみ |

**重要な違い:**
- **Bungee版はネットワーク統計のみ表示** - サーバー総採掘数は非表示
- **Bukkit版はすべて表示** - サーバー総採掘数も表示

## 🏗️ アーキテクチャ変更

### マルチモジュール化

プロジェクトをマルチモジュールGradleプロジェクトに変更しました：

```
MiningTracker/
├── miningtracker-common/   # 共通コード
│   ├── ConfigAdapter      # 設定インターフェース
│   └── CommonDatabaseManager  # DB処理
├── miningtracker-bukkit/   # Bukkitプラグイン
│   ├── 既存機能すべて維持
│   └── BukkitConfigAdapter
└── miningtracker-bungee/   # Bungeecordプラグイン（新規）
    ├── MiningTrackerBungee
    ├── BungeeConfigAdapter
    └── MiningTrackerBungeeExtension (Plan)
```

### 共通モジュール

**ConfigAdapter Interface**
- Bukkit/Bungeeの設定を統一的に扱う抽象化
- プラットフォーム非依存のコード

**CommonDatabaseManager**
- MySQL/SQLite対応のDB処理
- Plan統計用メソッド実装
- HikariCP接続プール対応

## 📦 ビルド成果物

v2.2.0では3つのJARファイルが生成されます：

1. **MiningTracker-Bukkit-2.2.0.jar** - バックエンドサーバー用
   - Spigot/Paper 1.21.x
   - すべての機能を含む
   - MySQL/SQLite対応

2. **MiningTracker-Bungee-2.2.0.jar** - Bungeecordプロキシ用
   - Bungeecord/Waterfall/Velocity
   - ネットワーク統計のみ
   - **MySQLのみ対応**（SQLiteは不可）

3. **MiningTracker-Common-2.2.0.jar** - 共通ライブラリ
   - 上記2つのJARに含まれる（shadowJar）

## 🚀 使用方法

### Bungeecord版のインストール

1. **前提条件**
   - Bungeecord/Waterfall/Velocity プロキシ
   - MySQL 5.7+ または 8.0+
   - Plan Player Analytics (Bungee版)
   - 各バックエンドサーバーにMiningTracker-Bukkit導入済み

2. **インストール**
```bash
# Bungeecordのpluginsフォルダに配置
cp MiningTracker-Bungee-2.2.0.jar /path/to/bungeecord/plugins/
```

3. **設定** (`plugins/MiningTracker/config.yml`)
```yaml
database:
  type: mysql  # MySQLのみ
  mysql:
    host: "mysql.example.com"
    database: "minecraft_network"  # バックエンドと同じDB
    username: "minecraft"
    password: "secure_password"
```

4. **起動確認**
```
[MiningTracker] MiningTracker (Bungee) が有効化されました。
[MiningTracker] Plan拡張機能を正常に登録しました
```

### いつBungee版を使うか

**使用を推奨する場合:**
- Planのネットワークページをシンプルに保ちたい
- ネットワーク全体の統計のみを表示したい
- プロキシレベルでの統計可視化が必要

**使用しない方が良い場合:**
- すべての統計（サーバー統計含む）を表示したい
- シンプルな構成を保ちたい
- SQLiteを使用している

## 📊 Plan表示例

### Bukkit版（バックエンドサーバー）

```
Plan > Server > MiningTracker
├── プレイヤー統計
│   ├── 総採掘ブロック数（全サーバー）
│   ├── 総採掘ブロック数（このサーバー）
│   └── 採掘ランキング順位
├── サーバー統計
│   ├── サーバー総採掘数 ✅
│   ├── アクティブマイナー数
│   └── トップマイナーランキング
└── ネットワーク統計
    ├── ネットワーク総採掘数
    ├── 総アクティブマイナー数
    └── サーバー比較
```

### Bungee版（プロキシ）

```
Plan > Network > MiningTracker
└── ネットワーク統計のみ
    ├── ネットワーク総採掘数 ✅
    ├── 総アクティブマイナー数
    ├── ネットワークトップランキング
    └── サーバー比較
    
注: サーバー統計は非表示 ❌
```

## ⚠️ 重要な制限事項

### Bungee版の制限

1. **MySQLのみ対応**
   - SQLiteは使用不可
   - 共有MySQLデータベースが必須

2. **読み取り専用**
   - 採掘トラッキング機能なし
   - 統計の表示のみ

3. **コマンドなし**
   - `/mtstats`, `/mtranking`等のコマンドなし
   - Planでのみ統計を表示

4. **ネットワーク統計のみ**
   - プレイヤー個別統計なし
   - サーバー別統計なし

## 🔧 技術詳細

### 設定アダプター

```java
// Bukkit
public class BukkitConfigAdapter implements ConfigAdapter {
    private final FileConfiguration config;
    // Bukkit設定ファイルをラップ
}

// Bungee
public class BungeeConfigAdapter implements ConfigAdapter {
    private final Configuration config;
    // Bungee設定ファイルをラップ
}
```

### データベース管理

```java
// 共通DB処理
public class CommonDatabaseManager {
    // ConfigAdapter経由で設定取得
    // MySQL/SQLite両対応
    // Plan統計メソッド実装
}
```

## 📝 移行ガイド

### 既存環境からの移行

**v2.1.x → v2.2.0 (Bungee版追加)**

1. 既存のバックエンドサーバーは変更不要
2. オプションでBungeecordにBungee版を追加
3. 設定ファイル構造は変更なし
4. データベーススキーマは変更なし

## 🐛 既知の問題

現時点では既知の問題はありません。

## 🙏 謝辞

この機能は「Bungeecordにもインストールできる様に変更してください」というフィードバックに基づいて開発されました。

## 🔗 参考リンク

- **詳細セットアップ**: [BUNGEECORD_SETUP.md](BUNGEECORD_SETUP.md)
- **Plan連携ガイド**: [PLAN_INTEGRATION.md](PLAN_INTEGRATION.md)
- **変更履歴**: [CHANGELOG.md](CHANGELOG.md)
- **バージョン情報**: [VERSION.md](VERSION.md)

## 💬 サポート

問題が発生した場合は、GitHubのIssuesページで報告してください：
- プラグインのバージョン
- サーバー/プロキシのバージョン
- 設定ファイル（パスワードは除く）
- エラーログ

---

**MiningTracker v2.2.0** - Bungeecord Plugin Support  
© 2026 kubota6646

次のバージョンもお楽しみに！
