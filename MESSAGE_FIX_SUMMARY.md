# メッセージ表示問題の修正サマリー

## 🐛 問題の詳細

### 症状
`/mtimport kubota6646 confirm` を実行すると：
- ✅ データベースに1105ブロックが正常にインポートされる
- ✅ トランザクションが正常にコミットされる
- ❌ チャットには `[MiningTracker]` のみが表示される
- ❌ 実際のメッセージ内容が表示されない

### ログ解析
```
[Server thread/INFO]: [MiningTracker] Success message: §8[§6MiningTracker§8]§r 
```

プレフィックス（`§8[§6MiningTracker§8]§r `）の後に何も含まれていない。

## 🔍 根本原因

### 問題1: 空のメッセージ値
ユーザーのランタイム `messages.yml` ファイルが古く、以下のいずれかの状態：
1. `import` セクションが存在しない
2. `import.success` キーが存在しない
3. `import.success: ""` のように空の値が設定されている

### 問題2: デフォルト値のフォールバック不足
Bukkit の `YamlConfiguration.setDefaults()` は：
- ✅ キーが**存在しない**場合 → デフォルト値を返す
- ❌ キーが**存在して空**の場合 → 空の値を返す（デフォルト値を使用しない）

これにより、空の値がそのまま返され、メッセージが表示されない。

### なぜこの問題が発生したか
1. プラグインを v2.5.0 → v2.5.1 に更新
2. v2.5.1 で `import.success` などの新しいメッセージを追加
3. 既存の `messages.yml` は上書きされない（`saveResource(..., false)` のため）
4. ユーザーの古い `messages.yml` には新しいキーが存在しない
5. または、空のセクションが存在する

## ✅ 実装した修正

### MessageManager.getMessage() の強化

**修正前**:
```java
public String getMessage(String path) {
    String message = messages.getString(path, "");
    return ChatColor.translateAlternateColorCodes('&', message);
}
```

**修正後**:
```java
public String getMessage(String path) {
    String message = messages.getString(path, "");
    
    // メッセージが空の場合、デフォルト設定から取得を試みる
    if (message == null || message.trim().isEmpty()) {
        if (messages.getDefaults() != null) {
            message = messages.getDefaults().getString(path, "");
        }
        if (message == null || message.trim().isEmpty()) {
            // それでも見つからない場合はエラーメッセージ
            return "[Message not found: " + path + "]";
        }
    }
    
    return ChatColor.translateAlternateColorCodes('&', message);
}
```

### 追加された詳細ログ

#### 1. メッセージ読み込み時
```java
plugin.getLogger().info("Loading messages from: " + messagesFile.getAbsolutePath());
plugin.getLogger().info("Messages file exists: " + messagesFile.exists());
plugin.getLogger().info("Loaded import.success message: '" + importSuccess + "'");
```

#### 2. メッセージ取得時
```java
plugin.getLogger().info("getMessage called - path: " + path + ", raw value: '" + message + "'");
plugin.getLogger().warning("Message '" + path + "' is empty, trying to get from defaults");
plugin.getLogger().info("Got from defaults: '" + message + "'");
```

#### 3. プレースホルダー置換時
```java
plugin.getLogger().info("Replacing {" + replacements[i] + "} with '" + replacements[i + 1] + "'");
plugin.getLogger().info("getMessage - after replacements: '" + message + "'");
```

## 📊 期待される動作

### 成功時のログ出力
```
[INFO] getMessage called - path: import.success, raw value: ''
[WARNING] Message 'import.success' is empty, trying to get from defaults
[INFO] Got from defaults: '&aプレイヤー &e{player}&a の統計をMinecraft統計から上書きインポートしました。'
[INFO] Replacing {player} with 'kubota6646'
[INFO] Success message: §8[§6MiningTracker§8]§r §aプレイヤー §ekubota6646§a の統計をMinecraft統計から上書きインポートしました。
```

### チャット表示
```
[MiningTracker] プレイヤー kubota6646 の統計をMinecraft統計から上書きインポートしました。
```

## 🎯 修正の利点

### 1. 後方互換性
- 古い `messages.yml` ファイルでも動作
- プラグイン更新時にユーザーアクション不要

### 2. 自己修復機能
- 欠落したメッセージキーを自動的に補完
- 空のメッセージ値をデフォルト値で置換

### 3. ユーザーフレンドリー
- 手動で `messages.yml` を削除/更新する必要なし
- プラグイン更新がシームレス

### 4. デバッグ可能
- 詳細なログでメッセージ読み込みの問題を特定可能
- トラブルシューティングが容易

## 🔧 テスト手順

### 1. プラグインを更新
最新版の MiningTracker-Bukkit-2.5.1.jar をデプロイ

### 2. コマンド実行
```bash
/mtimport kubota6646 confirm
```

### 3. 結果確認

**チャット**:
- ✅ `[MiningTracker] プレイヤー kubota6646 の統計をMinecraft統計から上書きインポートしました。`
- ❌ `[MiningTracker]` のみ

**ログ**:
- メッセージパスと取得値のログを確認
- デフォルトへのフォールバックログを確認
- 最終的なメッセージ内容を確認

### 4. データ確認
```bash
/mtr
```
- ランキングが更新されていることを確認

## 📝 追加の改善点

### より詳細なデバッグ情報
すべてのメッセージ取得で以下を記録：
- 要求されたメッセージパス
- YAMLから取得した生の値
- デフォルト値へのフォールバック
- プレースホルダー置換の各ステップ
- 最終的なメッセージ

### エラーハンドリング
メッセージが見つからない場合：
```
[Message not found: import.success]
```
のような明確なエラーメッセージを表示

## 🚨 トラブルシューティング

### まだメッセージが表示されない場合

1. **ログを確認**
   - `getMessage called - path: import.success` が表示されるか
   - `Got from defaults: '...'` が表示されるか
   - デフォルト値が正しく読み込まれているか

2. **messages.yml を削除してテスト**
   ```bash
   # サーバーを停止
   # plugins/MiningTracker/messages.yml を削除
   # サーバーを起動
   # 新しい messages.yml が生成される
   ```

3. **messages.yml の内容を確認**
   ```yaml
   # 以下が存在するか確認
   import:
     success: "&aプレイヤー &e{player}&a の統計をMinecraft統計から上書きインポートしました。"
   ```

4. **リソースファイルを確認**
   - JAR内の `messages.yml` が正しいか
   - UTF-8 エンコーディングか

## 📞 サポート

問題が解決しない場合、以下の情報を提供してください：
1. サーバーログ全体（MiningTracker関連）
2. `messages.yml` ファイルの内容
3. プラグインバージョン
4. Minecraftバージョン
5. 使用しているデータベース（MySQL/SQLite）

---

## 変更履歴

- **2026-02-07**: メッセージ表示問題を修正
- **2026-02-07**: デフォルト値フォールバック機能を追加
- **2026-02-07**: 詳細なデバッグログを追加
