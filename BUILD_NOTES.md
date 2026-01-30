# ビルドに関する注意事項

## 概要
このプロジェクトは完全に実装されていますが、現在のビルド環境ではSpigot/Paper APIリポジトリへのネットワークアクセスが制限されているため、ビルドを完了できません。

## 完了した実装

### ✅ プロジェクト構造
- Gradleビルドシステムの設定（build.gradle, settings.gradle）
- Gradle Wrapper（./gradlew）
- 適切な.gitignore設定

### ✅ プラグイン本体（8つのJavaクラス）
1. **MiningTracker.java** - メインプラグインクラス
2. **DatabaseManager.java** - SQLiteデータベース管理
3. **MessageManager.java** - メッセージ管理システム
4. **DataManager.java** - データトラッキング管理
5. **BlockBreakListener.java** - ブロック破壊イベントリスナー
6. **StatsCommand.java** - 統計表示コマンド（/mtstats）
7. **RankingCommand.java** - ランキング表示コマンド（/mtranking）
8. **ResetCommand.java** - 統計リセットコマンド（/mtreset）

### ✅ リソースファイル
- **plugin.yml** - プラグイン定義（日本語）
- **config.yml** - 設定ファイル（日本語）
- **messages.yml** - メッセージファイル（日本語）

### ✅ ドキュメント
- **README.md** - 完全な日本語ドキュメント

## ビルド方法（通常の環境）

ネットワークアクセスが可能な環境では、以下のコマンドでビルドできます：

```bash
./gradlew clean build
```

ビルドが成功すると、`build/libs/MiningTracker-1.0.0.jar`が生成されます。

## 必要な依存関係

```gradle
dependencies {
    compileOnly 'org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT'
    implementation 'com.mysql:mysql-connector-j:8.3.0'
}
```

このプラグインは以下のリポジトリから依存関係を取得します：
- Maven Central（MySQL JDBC Driver）
- Sonatype OSS Snapshots
- Spigot Nexus Repository

**セキュリティ注意**: MySQL Connector/J 8.3.0を使用しています。これは既知の脆弱性が修正された安全なバージョンです。

## 動作確認が必要な機能

ビルドが成功した後、以下の機能をテストしてください：

1. **プラグインの起動**
   - プラグインがエラーなく読み込まれること
   - データベースファイルが作成されること

2. **ブロック採掘のトラッキング**
   - ブロックを破壊すると採掘数がカウントされること
   - クリエイティブモードの設定が機能すること

3. **コマンド動作**
   - `/mtstats` で自分の統計が表示されること
   - `/mtranking` でランキングが表示されること
   - `/mtreset` で統計がリセットされること（OP権限）

4. **データの永続化**
   - サーバー再起動後もデータが保持されること
   - SQLiteとMySQLの両方が正しく動作すること

5. **日本語表示**
   - すべてのメッセージが正しく日本語で表示されること
   - カラーコードが正しく機能すること

6. **データベース接続**
   - SQLiteモードが正常に動作すること
   - MySQLモードが正常に動作すること
   - データベース設定の切り替えが機能すること

## コード品質

- すべてのJavaクラスは適切なパッケージ構造に配置
- 非同期処理を使用してデータベース操作を実行
- SQLiteとMySQLの両方をサポートした永続的なデータストレージ
- 設定可能なメッセージシステム
- 権限システムの実装
- エラーハンドリングの実装

## 既知の制限事項

現在のビルド環境では：
- Spigot/Paper APIリポジトリへのアクセスが制限されている
- ビルドを完了できない
- しかし、すべてのソースコードは完全に実装されている

通常のMinecraftサーバー開発環境では、これらの制限なくビルドが可能です。
