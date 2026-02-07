# MiningTracker v2.3.5 リリースノート - Plan統計表示修正

**リリース日**: 2026-02-07  
**バージョン**: 2.3.5  
**対応Minecraft**: 1.21.x  
**必須Java**: 21+

## 🐛 修正した問題

### Planのサーバー総採掘数とネットワーク総採掘数が表示されない

#### 報告された症状

Plan Player Analyticsで以下の値が正常に表示されない：
- **サーバー総採掘数**: サーバー全体の総採掘ブロック数
- **ネットワーク総採掘数**: 全サーバー合計の総採掘ブロック数

表示される値：
- 0が表示される
- または不正な値が表示される
- データが存在するにもかかわらず表示されない

#### 根本原因

**SQLの`SUM(count)`の動作:**
```sql
SELECT SUM(count) as total FROM mining_data WHERE server_name = ?
```

- 結果が存在しない場合、`SUM()`は`NULL`を返す
- データが0件の場合も`NULL`を返す

**JDBCの動作:**
```java
long total = rs.getLong("total");  // NULLの場合、0を返す
return total;  // しかし、rs.wasNull()をチェックしていない
```

**問題点:**
- `rs.getLong()`は`NULL`を`0`として扱う
- しかし、`rs.wasNull()`でチェックしないと不正確な動作になる可能性
- 明示的なNULLチェックが不足していた

## ✅ 実施した修正

### 1. NULL値の明示的チェック

**修正前:**
```java
if (rs.next()) {
    return rs.getLong("total");
}
```

**修正後:**
```java
if (rs.next()) {
    long total = rs.getLong("total");
    // rs.wasNull()をチェックしてNULLの場合は0を返す
    return rs.wasNull() ? 0 : total;
}
```

### 2. エラーログの改善

**修正前:**
```java
} catch (SQLException e) {
    plugin.getLogger().warning("統計取得エラー: " + e.getMessage());
}
```

**修正後:**
```java
} catch (SQLException e) {
    plugin.getLogger().warning("サーバー総採掘数取得エラー: " + e.getMessage());
    e.printStackTrace();  // スタックトレース追加
}
```

### 3. 修正した箇所

#### DatabaseManager.java (Bukkit)

1. **getServerTotalMined(String serverName)**
   - サーバー総採掘数取得
   - NULL処理追加

2. **getNetworkTotalMined()**
   - ネットワーク総採掘数取得
   - NULL処理追加

#### CommonDatabaseManager.java (Common)

1. **getNetworkTotalMined()**
   - ネットワーク総採掘数取得（Bungee版用）
   - NULL処理追加

## 📊 修正の効果

### Before (v2.3.4以前)

```
[Plan] サーバー統計タブ
├─ サーバー総採掘数: 0 ❌ (データがあるのに表示されない)
└─ アクティブマイナー数: 5 ✓

[Plan] ネットワーク統計タブ
├─ ネットワーク総採掘数: 0 ❌ (データがあるのに表示されない)
└─ 総アクティブマイナー数: 15 ✓
```

### After (v2.3.5)

```
[Plan] サーバー統計タブ
├─ サーバー総採掘数: 12,345 ✅ (正しく表示)
└─ アクティブマイナー数: 5 ✓

[Plan] ネットワーク統計タブ
├─ ネットワーク総採掘数: 45,678 ✅ (正しく表示)
└─ 総アクティブマイナー数: 15 ✓
```

## 🔧 技術詳細

### SQLのSUM()とNULL

**SQLの動作:**
```sql
-- データが0件の場合
SELECT SUM(count) FROM mining_data WHERE server_name = 'test';
-- 結果: NULL

-- データがある場合
SELECT SUM(count) FROM mining_data WHERE server_name = 'survival';
-- 結果: 12345
```

### JDBCのgetLong()とwasNull()

**JDBCの仕様:**
```java
ResultSet rs = ...;
long value = rs.getLong(1);  // NULLの場合、0を返す
boolean isNull = rs.wasNull();  // 直前のgetXXX()がNULLだったか確認
```

**ベストプラクティス:**
```java
if (rs.next()) {
    long value = rs.getLong("column");
    if (rs.wasNull()) {
        // NULLの場合の処理
        return 0;
    }
    return value;
}
```

### COALESCEを使った代替案

SQLレベルでNULLを処理する方法もあります（将来的な改善案）:
```sql
SELECT COALESCE(SUM(count), 0) as total FROM mining_data WHERE server_name = ?
```

## 📦 ビルド成果物

```
miningtracker-bukkit/build/libs/
└── MiningTracker-Bukkit-2.3.5.jar   # Bukkit/Paper用 ⭐ 更新

miningtracker-bungee/build/libs/
└── MiningTracker-Bungee-2.3.5.jar   # Bungeecord用 ⭐ 更新
```

## 🔄 v2.3.4からの移行

**必要な作業**: JARファイルの置き換えのみ

```bash
# 古いバージョンを削除
rm MiningTracker-Bukkit-2.3.4.jar
rm MiningTracker-Bungee-2.3.4.jar

# 新バージョンをインストール
cp MiningTracker-Bukkit-2.3.5.jar server1/plugins/
cp MiningTracker-Bukkit-2.3.5.jar server2/plugins/

# Bungeecord版を使用している場合
cp MiningTracker-Bungee-2.3.5.jar bungeecord/plugins/
```

**設定変更**: 不要

## ✅ 動作確認方法

### Planで統計を確認

1. **Planにアクセス**
   ```
   http://your-server:8804
   ```

2. **サーバー統計タブを確認**
   - サーバー総採掘数が表示されているか
   - 数値が0以外の適切な値か

3. **ネットワーク統計タブを確認**
   - ネットワーク総採掘数が表示されているか
   - 全サーバーの合計値になっているか

### トラブルシューティング

**それでも統計が表示されない場合:**

1. **Planプラグインが正しくインストールされているか**
   ```
   /plugins
   ```
   Planが緑色で表示されることを確認

2. **MiningTrackerがPlanに登録されているか**
   - `logs/latest.log`で「Plan拡張機能を正常に登録しました」を確認

3. **データベースにデータが存在するか**
   ```sql
   SELECT COUNT(*) FROM mining_data;
   ```

4. **ログでエラーを確認**
   ```bash
   tail -f logs/latest.log | grep "MiningTracker\|Plan"
   ```

## 🎯 v2.3.4の全機能を継承

- ✅ サーバー間リアルタイム同期（v2.3.4で修正）
- ✅ Bungeecordプラグイン対応
- ✅ マルチモジュール構造
- ✅ Plan Player Analytics連携
- ✅ HikariCP依存関係修正済み
- ✅ Bungeecord API 1.21-R0.4使用
- ✅ shadowJar依存関係リロケーション
- ✅ 詳細なトラブルシューティングガイド

## 🙏 謝辞

Plan統計表示の問題を報告いただき、ありがとうございました。この修正により、Planでより正確な統計情報を表示できるようになりました。

## 🔗 参考リンク

- **v2.3.4リリースノート**: [RELEASE_NOTES_v2.3.4.md](RELEASE_NOTES_v2.3.4.md)
- **Plan連携ガイド**: [PLAN_INTEGRATION.md](PLAN_INTEGRATION.md)
- **トラブルシューティング**: [README.md#トラブルシューティング](README.md#トラブルシューティング)
- **変更履歴**: [CHANGELOG.md](CHANGELOG.md)
- **バージョン情報**: [VERSION.md](VERSION.md)

## 💬 サポート

問題が発生した場合は、以下を確認してください：

1. **Plan Player Analyticsインストール済み**
2. **MiningTrackerとPlan両方が有効**
3. **v2.3.5使用**: 全サーバーで最新版を使用

それでも解決しない場合は、GitHubのIssuesページで報告してください。

---

**MiningTracker v2.3.5** - Plan Statistics Fix Release  
© 2026 kubota6646

正確な統計で快適なマイニングライフを！
