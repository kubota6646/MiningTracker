# HikariCP Character Encoding Fix

## 問題の概要

### エラーメッセージ
```
com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: Unsupported character encoding 'utf8mb4'
```

### 原因
**v2.0.0-2.0.1での問題:**
MySQL JDBC Driverでは、文字エンコーディングなどの接続パラメータはJDBC URL内で指定する必要があります。HikariCPの`addDataSourceProperty()`メソッドでこれらを設定すると、ドライバーが認識できずエラーになります。

**v2.0.1でも発生する問題:**
MySQL Connector/J 8.xでは、`characterEncoding`パラメータが非推奨（deprecated）となり、`utf8mb4`という値は認識されなくなりました。MySQL Connector/J 8.xではデフォルトでUTF-8（utf8mb4）が使用されるため、このパラメータは不要です。

## 修正内容（v2.0.2）

### MySQL Connector/J 8.x対応

**問題のコード (v2.0.1):**
```java
String jdbcUrl = String.format(
    "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8mb4",
    host, port, database
);
// ❌ characterEncoding=utf8mb4 はMySQL Connector/J 8.xでサポートされていない
```

**修正後のコード (v2.0.2):**
```java
// ✅ MySQL Connector/J 8.x用の正しい設定
String jdbcUrl = String.format(
    "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectionCollation=utf8mb4_unicode_ci",
    host, port, database
);
// connectionCollation を使用（characterEncodingは削除）
```

### MySQL Connector/J バージョン別の違い

| バージョン | 文字エンコーディング設定 | 説明 |
|-----------|------------------------|------|
| 5.x | `characterEncoding=utf8` または `utf8mb4` | characterEncodingパラメータが有効 |
| 8.x | `connectionCollation=utf8mb4_unicode_ci` または省略 | characterEncodingは非推奨、デフォルトがutf8mb4 |

## HikariCPにおける設定の種類

### 1. JDBC URLパラメータ（接続レベルの設定）
JDBC URL内に`?param=value&param2=value2`形式で指定します。

**使用すべき設定:**
- `useSSL` - SSL接続の有効/無効
- `allowPublicKeyRetrieval` - 公開鍵の取得許可
- `serverTimezone` - サーバーのタイムゾーン
- `characterEncoding` - 文字エンコーディング
- `useUnicode` - Unicode使用の有効化
- `autoReconnect` - 自動再接続

**例:**
```java
String jdbcUrl = "jdbc:mysql://localhost:3306/mydb?useSSL=false&characterEncoding=utf8mb4";
```

### 2. DataSourceプロパティ（最適化設定）
`addDataSourceProperty()`メソッドで設定します。

**使用すべき設定:**
- `cachePrepStmts` - PreparedStatementキャッシュ
- `prepStmtCacheSize` - キャッシュサイズ
- `prepStmtCacheSqlLimit` - SQL長さ制限
- `useServerPrepStmts` - サーバー側PreparedStatement
- `rewriteBatchedStatements` - バッチ書き込み最適化

**例:**
```java
hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
```

## よくある間違い

### ❌ 間違い1: 接続パラメータをDataSourceプロパティとして設定
```java
hikariConfig.addDataSourceProperty("characterEncoding", "utf8mb4");
// エラー: Unsupported character encoding 'utf8mb4'
```

### ✅ 正しい方法: JDBC URLパラメータとして設定
```java
String jdbcUrl = "jdbc:mysql://localhost:3306/db?characterEncoding=utf8mb4";
hikariConfig.setJdbcUrl(jdbcUrl);
```

### ❌ 間違い2: 最適化設定をURLパラメータに含める
```java
String jdbcUrl = "jdbc:mysql://localhost:3306/db?cachePrepStmts=true";
// 動作するが推奨されない
```

### ✅ 正しい方法: DataSourceプロパティとして設定
```java
hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
```

## 推奨される完全な設定例

```java
// 1. 接続パラメータをJDBC URLに含める
String jdbcUrl = String.format(
    "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8mb4",
    host, port, database
);

HikariConfig config = new HikariConfig();
config.setJdbcUrl(jdbcUrl);
config.setUsername(username);
config.setPassword(password);

// 2. 接続プール設定
config.setMaximumPoolSize(10);
config.setMinimumIdle(2);
config.setConnectionTimeout(30000);

// 3. MySQL最適化設定（DataSourceプロパティ）
config.addDataSourceProperty("cachePrepStmts", "true");
config.addDataSourceProperty("prepStmtCacheSize", "250");
config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
config.addDataSourceProperty("useServerPrepStmts", "true");
config.addDataSourceProperty("useLocalSessionState", "true");
config.addDataSourceProperty("rewriteBatchedStatements", "true");
config.addDataSourceProperty("cacheResultSetMetadata", "true");
config.addDataSourceProperty("cacheServerConfiguration", "true");

HikariDataSource dataSource = new HikariDataSource(config);
```

## MySQL JDBC URLパラメータ一覧

### 接続関連
| パラメータ | 説明 | 推奨値 |
|-----------|------|--------|
| useSSL | SSL接続を使用 | false（開発）、true（本番） |
| allowPublicKeyRetrieval | 公開鍵の取得を許可 | true |
| requireSSL | SSLを必須にする | false |

### 文字エンコーディング
| パラメータ | 説明 | 推奨値 |
|-----------|------|--------|
| characterEncoding | 文字エンコーディング | utf8mb4（日本語対応） |
| useUnicode | Unicode使用 | true |

### タイムゾーン
| パラメータ | 説明 | 推奨値 |
|-----------|------|--------|
| serverTimezone | サーバータイムゾーン | UTC、Asia/Tokyo |
| useLegacyDatetimeCode | 古い日時コード使用 | false |

### パフォーマンス
| パラメータ | 説明 | 推奨値 |
|-----------|------|--------|
| useCompression | 圧縮通信 | true（大量データ時） |
| cacheCallableStmts | CallableStatementキャッシュ | true |

## トラブルシューティング

### エラー: Unsupported character encoding
**原因:** characterEncodingをDataSourceプロパティとして設定
**解決:** JDBC URLパラメータとして設定

### エラー: Unknown system variable 'query_cache_size'
**原因:** MySQL 8.0ではクエリキャッシュが削除された
**解決:** 該当する設定を削除

### エラー: Public Key Retrieval is not allowed
**原因:** `allowPublicKeyRetrieval`が設定されていない
**解決:** URLに`allowPublicKeyRetrieval=true`を追加

### エラー: The server time zone value 'XXX' is unrecognized
**原因:** サーバータイムゾーンが設定されていない
**解決:** URLに`serverTimezone=UTC`を追加

## 参考資料

- [HikariCP GitHub](https://github.com/brettwooldridge/HikariCP)
- [MySQL Connector/J Documentation](https://dev.mysql.com/doc/connector-j/8.0/en/)
- [MySQL JDBC URL Parameters](https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-reference-configuration-properties.html)

## まとめ

HikariCPとMySQL JDBC Driverを使用する際は:

✅ **JDBC URLパラメータ:** 接続レベルの設定（SSL、文字エンコーディング、タイムゾーン）
✅ **DataSourceプロパティ:** パフォーマンス最適化設定（キャッシュ、バッチ処理）

この区別を理解することで、接続エラーを回避できます。
