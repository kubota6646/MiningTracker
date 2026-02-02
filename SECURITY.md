# セキュリティに関する情報

## MySQL Connector/J バージョンについて

### 使用バージョン
**MySQL Connector/J 8.3.0**

### バージョン選定理由

以前のバージョン（8.0.33以前）には以下の脆弱性が報告されていました：
- MySQL Connectors takeover vulnerability
- 影響を受けるバージョン: < 8.2.0 および <= 8.0.33

本プラグインでは、これらの脆弱性が修正された **8.3.0** を使用しています。

### セキュリティ推奨事項

1. **最新版の使用**
   - 常に最新の安定版を使用することを推奨します
   - 定期的に依存関係の更新を確認してください

2. **MySQL接続設定**
   - 本番環境では強固なパスワードを使用してください
   - 必要に応じてSSL/TLS接続を有効化してください
   - データベースユーザーには最小限の権限のみを付与してください

3. **設定ファイルのセキュリティ**
   - config.ymlに記載されたパスワードは平文で保存されます
   - ファイルのパーミッションを適切に設定してください（推奨: 600）
   - 公開リポジトリに実際のパスワードをコミットしないでください

## 推奨されるMySQL設定

### データベースユーザーの作成

```sql
-- 専用ユーザーを作成
CREATE USER 'miningtracker'@'localhost' IDENTIFIED BY 'strong_password_here';

-- 必要最小限の権限のみを付与
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, INDEX ON minecraft.* TO 'miningtracker'@'localhost';

-- 権限を反映
FLUSH PRIVILEGES;
```

### SSL/TLS接続の有効化（推奨）

config.ymlに以下を追加することで、将来的にSSL接続に対応可能です：

```yaml
database:
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: miningtracker
    password: your_secure_password
    # 将来的なSSL対応の準備
    useSSL: true
    requireSSL: true
```

## 依存関係の監視

定期的に以下をチェックすることを推奨します：

1. **GitHub Advisory Database**
   - https://github.com/advisories

2. **Maven Central Security**
   - MySQL Connector/Jの最新版を確認

3. **プラグインの更新**
   - 本プラグインの更新情報を確認
   - セキュリティアップデートは優先的に適用

## 脆弱性報告

もし本プラグインにセキュリティ上の問題を発見した場合は、
GitHubのIssuesではなく、プライベートに報告してください。

## 更新履歴

- **2024-01**: MySQL Connector/J 8.3.0にアップデート（脆弱性修正）
- **2024-01**: 初回リリース
