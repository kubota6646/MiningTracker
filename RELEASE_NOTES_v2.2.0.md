# MiningTracker v2.2.0 リリースノート

**リリース日**: 2026-02-07  
**バージョン**: 2.2.0  
**対応Minecraft**: 1.21.x  
**必須Java**: 21+

## 🎉 主な新機能

### Bungeecord/Velocityネットワーク対応

MiningTracker v2.2.0では、Bungeecord/Velocityネットワーク環境での動作を正式にサポートしました。

#### 新機能のハイライト

1. **ネットワーク統計の表示**
   - Plan Player Analyticsのネットワークページに採掘統計が表示されます
   - 複数のバックエンドサーバーからデータを自動的に集約
   - サーバー間の採掘量比較が可能

2. **マルチサーバー対応の強化**
   - 各バックエンドサーバーで`server-name`を設定
   - サーバー別統計とネットワーク全体統計の両方を提供
   - プレイヤーがサーバー間を移動しても統計を正しく追跡

3. **Planネットワークページ統合**
   - ネットワーク総採掘数
   - 総アクティブマイナー数
   - ネットワークトップマイナーランキング
   - サーバー比較テーブル

## 📚 新規ドキュメント

### BUNGEECORD_SETUP.md

Bungeecord環境での詳細なセットアップガイドを追加しました：

- **アーキテクチャ図**: ネットワーク構成の視覚化
- **ステップバイステップ手順**: 
  - MySQLデータベースの準備
  - Plan Player Analyticsのインストールと設定
  - MiningTrackerの各サーバー設定
  - 動作確認方法
- **トラブルシューティング**: よくある問題と解決方法
- **パフォーマンス最適化**: 接続プール設定のベストプラクティス
- **FAQ**: よくある質問と回答

### ドキュメント更新

- **PLAN_INTEGRATION.md**: Bungeecord対応の情報を追加
- **README.md**: 主な機能セクションにBungeecord対応を追加

## 🔧 技術的詳細

### 動作要件

#### Bungeecordネットワークの場合

```
Bungeecord Proxy
├── Plan Player Analytics (Bungee版)
└── バックエンドサーバー × N
    ├── Spigot/Paper 1.21.x
    ├── Plan Player Analytics (Bukkit版)
    ├── MiningTracker
    └── 共有MySQL接続
```

#### 必須コンポーネント

- Bungeecord/Waterfall/Velocityプロキシ
- MySQL 5.7+ または 8.0+
- Plan Player Analytics 5.6+（Bungeecord版とBukkit版）
- Java 21+

### データフロー

1. 各バックエンドサーバーでプレイヤーがブロックを採掘
2. MiningTrackerが採掘データを共有MySQLデータベースに記録
3. Plan DataExtensionがMySQLから統計を取得
4. Planネットワークページが全サーバーのデータを集約して表示

## 📋 設定例

### バックエンドサーバー設定

各サーバーで異なる`server-name`を設定：

```yaml
# Survivalサーバー
server-name: "survival"

database:
  type: mysql
  mysql:
    host: "mysql.example.com"
    database: "minecraft_network"
    # 全サーバーで同じMySQL接続情報
```

```yaml
# Creativeサーバー
server-name: "creative"

database:
  type: mysql
  mysql:
    host: "mysql.example.com"
    database: "minecraft_network"
    # 全サーバーで同じMySQL接続情報
```

## 🎯 使用方法

### インストール

1. **MySQL**を全サーバーで共有
2. **Plan**をBungeecord + 全バックエンドサーバーにインストール
3. **MiningTracker**を全バックエンドサーバーにインストール
   - **注意**: Bungeecordプロキシには不要
4. 各サーバーで異なる`server-name`を設定

### 動作確認

1. Planダッシュボードにアクセス: `http://your-proxy-ip:8804`
2. ネットワークページを開く
3. MiningTracker統計が表示されることを確認
   - ネットワーク総採掘数
   - サーバー比較テーブル
   - トップマイナーランキング

## 🐛 バグ修正

### DatabaseManagerリソースリーク修正（継続）

v2.2.0初期リリースで修正されたバグ：

- ResultSetオブジェクトのリソースリーク
- 30箇所のtry-with-resources適用
- メモリリークとコネクションプール枯渇の防止

## 📊 パフォーマンス

### 接続プール最適化

サーバー規模に応じた推奨設定：

- **小規模ネットワーク**（2-3サーバー）: `maximum-pool-size: 5`
- **中規模ネットワーク**（4-10サーバー）: `maximum-pool-size: 10`
- **大規模ネットワーク**（10+サーバー）: `maximum-pool-size: 15`

## ⚠️ 重要な注意事項

### Bungeecord環境での制限事項

1. **MySQLが必須**: SQLiteは複数サーバーで共有できません
2. **サーバー名の一意性**: 各サーバーで異なる`server-name`を設定する必要があります
3. **プロキシへのインストール不要**: MiningTrackerはBungeecordプロキシには不要です

### 既存環境からの移行

既存のシングルサーバー環境からBungeecordネットワークに移行する場合：

1. MySQLデータベースを準備
2. 既存のSQLiteデータをMySQLに移行（必要に応じて）
3. `server-name`を設定（既存サーバーは"default"のまま可）
4. 新しいサーバーを追加して異なる`server-name`を設定

## 🔗 参考リンク

- **詳細なセットアップガイド**: [BUNGEECORD_SETUP.md](BUNGEECORD_SETUP.md)
- **Plan連携ガイド**: [PLAN_INTEGRATION.md](PLAN_INTEGRATION.md)
- **README**: [README.md](README.md)
- **変更履歴**: [CHANGELOG.md](CHANGELOG.md)

## 📝 FAQ

### Q: Bungeecordにもプラグインをインストールする必要がありますか？

A: **いいえ**。MiningTrackerはBukkitプラグインで、バックエンドサーバーのみにインストールします。

### Q: Velocityでも動作しますか？

A: はい。PlanはVelocityにも対応しているため、同じ設定で動作します。

### Q: 既存のデータは移行されますか？

A: シングルサーバーからの移行時、既存データは`server-name = "default"`として記録されます。

### Q: 一部のサーバーでのみ採掘統計を有効にできますか？

A: はい。各サーバーの`config.yml`で`tracking.enabled: false`に設定できます。

## 🎁 ダウンロード

- **GitHub Releases**: https://github.com/kubota6646/MiningTracker/releases
- **JAR ファイル**: `MiningTracker-2.2.0.jar`

## 💬 サポート

問題が発生した場合：

1. [BUNGEECORD_SETUP.md](BUNGEECORD_SETUP.md)のトラブルシューティングセクションを確認
2. GitHubのIssuesページで報告
3. 以下の情報を含めてください：
   - Bungeecordのバージョン
   - バックエンドサーバーのバージョン
   - Planのバージョン
   - MySQLのバージョン
   - 各サーバーの`server-name`設定
   - サーバーログ

## 🙏 謝辞

このリリースは、Plan Player Analyticsとの統合とBungeecordネットワーク対応を求める
コミュニティからのフィードバックに基づいて開発されました。

---

**MiningTracker v2.2.0** - Bungeecord Network Support  
© 2026 kubota6646

次のバージョンもお楽しみに！
