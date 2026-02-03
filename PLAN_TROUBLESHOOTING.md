# Plan連携トラブルシューティングガイド

## 問題: 「Extension Dataが存在しません」と表示される

### 症状
- Planのプラグイン管理ページでMiningTrackerが表示されない
- または「Extension Data does not exist」と表示される
- プレイヤーページにMining Statsタブが表示されない

---

## 解決方法

### 1. バージョンの確認

#### MiningTrackerのバージョン
**必須**: v2.1.5以降

```bash
# サーバーログで確認
[MiningTracker] MiningTracker v2.1.5 が有効化されました。
```

v2.1.4以前のバージョンには、Plan登録に問題があります。
必ずv2.1.5以降にアップグレードしてください。

#### Planのバージョン
**必須**: Plan 5.6以降

```bash
# プラグイン一覧で確認
/plugins
# または
/version Plan
```

### 2. ログの確認

サーバー起動ログで以下のメッセージを確認してください:

#### ✅ 正常な場合
```
[MiningTracker] MiningTracker が有効化されました。
[MiningTracker] Plan拡張機能を正常に登録しました
```

#### ❌ 問題がある場合

**パターンA: Planが見つからない**
```
[MiningTracker] Plan Player Analyticsが見つかりません。通常モードで動作します。
```
**原因**: Planがインストールされていない、または正しく読み込まれていない
**解決**: 
1. Planがpluginsフォルダにあるか確認
2. Planが正常に起動しているか確認（`/plugins`コマンド）

**パターンB: 有効化エラー**
```
[MiningTracker] Planが有効化されていません: [エラーメッセージ]
```
**原因**: Planの初期化に失敗している
**解決**: 
1. Planのログでエラーを確認
2. Planの設定ファイルをチェック

**パターンC: 実装エラー**
```
[MiningTracker] Plan拡張機能の実装に問題があります: [エラーメッセージ]
[スタックトレース]
```
**原因**: DataExtensionの実装に問題がある
**解決**: 
1. スタックトレースを確認
2. GitHubのIssueで報告

### 3. プラグインの読み込み順序

Planは他のプラグインよりも**先に**読み込まれる必要があります。

#### 確認方法
```bash
/plugins
```

プラグイン一覧で、Planがアルファベット順でMiningTrackerより前にあるか確認。

#### 読み込み順序を強制する方法

**plugin.yml（MiningTracker）**
```yaml
depend: [Plan]  # Planが必須の場合
# または
softdepend: [Plan]  # Planがオプショナルの場合（現在の設定）
```

現在の設定では`softdepend`を使用しているため、
Planがなくても動作しますが、Planがある場合は先に読み込まれます。

### 4. サーバーの再起動

プラグインを更新した後は、必ずサーバーを**完全に再起動**してください。

**❌ 動作しない:**
```bash
/reload
/reload confirm
```

**✅ 正しい方法:**
1. サーバーを停止
2. JARファイルを置き換え
3. サーバーを起動

### 5. Planのキャッシュクリア

#### 方法1: コマンドでリロード
```bash
/plan reload
```

#### 方法2: Planのデータベースを再構築
```bash
/plan db backup
/plan db restore
```

#### 方法3: キャッシュファイルを削除
```bash
# サーバーを停止
# Plan/cacheフォルダを削除
rm -rf plugins/Plan/cache
# サーバーを起動
```

### 6. データベースの確認

MiningTrackerがデータベースに正常に接続しているか確認:

```bash
# ログで確認
[MiningTracker] SQLiteデータベースに接続しました
# または
[MiningTracker] MySQLデータベースに接続しました（HikariCP使用）
```

データベース接続に失敗している場合、
Plan拡張も正常に動作しません。

### 7. Java バージョンの確認

**必須**: Java 21

```bash
java -version
```

出力例:
```
openjdk version "21.0.1" 2023-10-17
```

Java 17以下では動作しません。

---

## デバッグ手順

### ステップ1: 基本情報の収集

以下の情報を集めてください:

```bash
# サーバーバージョン
/version

# プラグイン一覧
/plugins

# MiningTrackerのバージョン
# サーバーログで確認

# Planのバージョン
/plan info
```

### ステップ2: 起動ログの確認

`logs/latest.log`ファイルで以下を検索:

```bash
# MiningTracker関連
grep "MiningTracker" logs/latest.log

# Plan関連
grep "Plan" logs/latest.log
```

### ステップ3: Plan管理画面の確認

1. Planの管理画面を開く（通常: http://サーバーIP:8804）
2. 「Manage」→「Plugins」に移動
3. MiningTrackerが一覧に表示されているか確認

### ステップ4: データの確認

ブロックを破壊して、データが記録されているか確認:

```bash
# コマンドで確認
/mtstats

# 出力例:
あなたの採掘統計:
STONE: 100個
DIRT: 50個
総採掘数: 150個
```

データが記録されていない場合、データベース接続に問題があります。

---

## よくある問題と解決策

### 問題1: 「Plan拡張機能を正常に登録しました」と出るが、Planに表示されない

**原因**: Planのキャッシュの問題

**解決策**:
```bash
/plan reload
```

または、サーバーを再起動してください。

### 問題2: プレイヤーページにMining Statsタブが表示されない

**原因**: 
- プレイヤーのデータがない
- タブの読み込みに失敗

**解決策**:
1. そのプレイヤーでブロックを破壊
2. `/mtstats`で確認
3. Planでプレイヤーページを再読み込み

### 問題3: 数値が0と表示される

**原因**: 
- データベースにデータがない
- サーバー名の不一致（マルチサーバー環境）

**解決策**:

#### シングルサーバーの場合
```yaml
# config.yml
server-name: "default"  # デフォルトのまま
```

#### マルチサーバーの場合
各サーバーで異なる`server-name`を設定:
```yaml
# サーバーA
server-name: "survival"

# サーバーB
server-name: "creative"
```

そして、すべてのサーバーが**同じMySQLデータベース**を使用する必要があります。

### 問題4: テーブルが2つずつ表示される（重複表示）

**症状**:
- Planで同じテーブルが2回表示される
- 1つは英語の古いメソッド名、もう1つは日本語Romajiの新しいメソッド名

**原因**: 
MiningTracker v2.1.8でメソッド名を変更したが、Planのデータベースに古いメソッド名のデータが残っている。

**解決策**: MiningTracker v2.1.9以降にアップデート

v2.1.9では`@InvalidateMethod`アノテーションが追加され、古いメソッド名から新しいメソッド名への移行が自動的に行われます。

#### ステップ1: プラグインをアップデート

1. MiningTracker v2.1.9以降をダウンロード
2. サーバーを停止
3. 古いJARファイルを削除
4. 新しいJARファイルを`plugins`フォルダに配置
5. サーバーを起動

#### ステップ2: Planのキャッシュをクリア（必要な場合）

```bash
/plan reload
```

または

```bash
# サーバーを停止
rm -rf plugins/Plan/cache
# サーバーを起動
```

#### 結果

アップデート後、テーブルは1つずつ正しく表示されます。

### 問題5: Planの表示が一部英語になる（「Average 総採掘ブロック数」など）

**症状**:
- プレイヤー一覧テーブルで「Average 総採掘ブロック数」のように英語と日本語が混在
- 統計ラベルが英語で表示される

**注意**: MiningTracker v2.1.8以降を使用していますか？
- v2.1.8以降では、テーブル名は日本語Romaji（ローマ字）になっています
- 例: `burokku_shubetsu_naiwake`、`toppu_maina`など
- `@TableProvider`は`text`パラメータをサポートしていないため、メソッド名が表示されます
- それでも英語が表示される場合は、Planの言語設定が原因です

**原因**: 
Planの言語設定が英語（デフォルト）になっている。Planは統計をプレイヤー一覧テーブルに表示する際、自動的に「Average」（平均）、「Total」（合計）などのラベルを追加します。

**解決策**: Planを日本語に設定

#### ステップ1: Planの設定ファイルを編集

1. サーバーを停止
2. `plugins/Plan/config.yml`を開く
3. 以下のように変更:

```yaml
Plugin:
  Locale: ja  # 英語から日本語に変更
  Logging:
    Create_new_locale_file_on_next_enable: true  # ロケールファイル生成を有効化
```

4. サーバーを起動（または`/plan reload`）

#### ステップ2: 結果を確認

変更後、ラベルが日本語に変わります：

**変更前:**
- Average 総採掘ブロック数
- Total 総採掘ブロック数

**変更後:**
- 平均 総採掘ブロック数
- 合計 総採掘ブロック数

#### 補足: Webインターフェースから変更する方法

1. Plan管理画面を開く（例: `http://サーバーIP:8804`）
2. 「Manage」→「Server Settings」
3. 「General Settings」で「Locale」を「日本語 (ja)」に変更
4. 設定を保存し、リロード

**参考**: 詳細は [PLAN_INTEGRATION.md](PLAN_INTEGRATION.md#planの表示が一部英語になるaverage-総採掘ブロック数など) を参照してください。

### 問題5: ビルドエラーが発生する

**症状**:
```
Could not find com.github.plan-player-analytics:Plan:5.6.2959
```

**原因**: JitPackリポジトリが設定されていない

**解決策**: `build.gradle`を確認:
```gradle
repositories {
    maven {
        name = 'jitpack'
        url = 'https://jitpack.io'
    }
}

dependencies {
    compileOnly 'com.github.plan-player-analytics:Plan:5.6.2959'
}
```

---

## チェックリスト

トラブルシューティングの前に、以下を確認してください:

- [ ] MiningTracker v2.1.5以降を使用
- [ ] Plan 5.6以降を使用
- [ ] Java 21を使用
- [ ] サーバーを完全に再起動した
- [ ] ログで「Plan拡張機能を正常に登録しました」を確認
- [ ] `/plugins`でPlanとMiningTrackerが有効になっている
- [ ] `/mtstats`でデータが記録されている
- [ ] Planのキャッシュをクリアした

---

## それでも解決しない場合

### サポートを受ける

GitHubのIssueで報告してください:
https://github.com/kubota6646/MiningTracker/issues

**必要な情報**:
1. サーバーバージョン（`/version`の出力）
2. MiningTrackerのバージョン
3. Planのバージョン
4. Javaのバージョン（`java -version`）
5. 起動ログ（`logs/latest.log`）
6. エラーメッセージ（あれば）
7. `config.yml`の内容

### ログの提供方法

```bash
# ログをファイルにコピー
cp logs/latest.log miningtracker-log.txt

# MiningTracker関連のログのみ抽出
grep "MiningTracker" logs/latest.log > miningtracker-log.txt
```

このファイルをGitHub Issueに添付してください。

---

## 関連ドキュメント

- [PLAN_INTEGRATION.md](PLAN_INTEGRATION.md) - Plan連携の完全ガイド
- [README.md](README.md) - 基本的な使い方
- [MYSQL_SETUP.md](MYSQL_SETUP.md) - MySQLセットアップガイド
- [CHANGELOG.md](CHANGELOG.md) - 変更履歴

---

**最終更新**: v2.1.5 (2026-02-02)
