package com.kubota6646.miningtracker.database;

import com.kubota6646.miningtracker.MiningTracker;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Material;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    
    private final MiningTracker plugin;
    private Connection connection;
    private HikariDataSource hikariDataSource;
    
    public DatabaseManager(MiningTracker plugin) {
        this.plugin = plugin;
    }
    
    public boolean connect() {
        try {
            String dbType = plugin.getConfig().getString("database.type", "sqlite");
            
            if (dbType.equalsIgnoreCase("sqlite")) {
                return connectSQLite();
            } else if (dbType.equalsIgnoreCase("mysql")) {
                return connectMySQL();
            } else {
                plugin.getLogger().severe("サポートされていないデータベースタイプ: " + dbType);
                return false;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("データベース接続エラー: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    private boolean connectSQLite() throws SQLException {
        String fileName = plugin.getConfig().getString("database.sqlite.file", "mining_data.db");
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        File dbFile = new File(dataFolder, fileName);
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        
        connection = DriverManager.getConnection(url);
        createTables();
        
        plugin.getLogger().info("SQLiteデータベースに接続しました。");
        return true;
    }
    
    private boolean connectMySQL() throws SQLException {
        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = plugin.getConfig().getString("database.mysql.database", "minecraft");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "password");
        
        // HikariCP接続プール設定
        int maxPoolSize = plugin.getConfig().getInt("database.mysql.pool.maximum-pool-size", 10);
        int minIdle = plugin.getConfig().getInt("database.mysql.pool.minimum-idle", 2);
        long connectionTimeout = plugin.getConfig().getLong("database.mysql.pool.connection-timeout", 30000);
        
        // JDBC URLパラメータを構築（MySQL Connector/J 8.x用）
        // 注: MySQL Connector/J 8.xではUTF-8がデフォルトなので、characterEncodingは不要
        String jdbcUrl = String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectionCollation=utf8mb4_unicode_ci",
            host, port, database
        );
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(maxPoolSize);
        hikariConfig.setMinimumIdle(minIdle);
        hikariConfig.setConnectionTimeout(connectionTimeout);
        hikariConfig.setLeakDetectionThreshold(60000);
        
        // MySQL最適化設定（DataSourceプロパティとして設定）
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        // elideSetAutoCommits を削除 - リアルタイム同期のため
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
        
        // リアルタイム同期を確実にするための設定
        hikariConfig.setAutoCommit(true);  // 明示的にautoCommitを有効化
        
        try {
            hikariDataSource = new HikariDataSource(hikariConfig);
            
            // テーブルを作成（接続を一時的に取得し、すぐに返却）
            try (Connection conn = hikariDataSource.getConnection()) {
                createTablesForMySQL(conn);
            }
            
            plugin.getLogger().info("MySQLデータベースに接続しました（HikariCP使用）。");
            plugin.getLogger().info("接続プール設定: 最大=" + maxPoolSize + ", 最小=" + minIdle);
            return true;
        } catch (SQLException e) {
            if (hikariDataSource != null) {
                hikariDataSource.close();
            }
            throw e;
        }
    }
    
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            if (hikariDataSource != null && !hikariDataSource.isClosed()) {
                hikariDataSource.close();
                plugin.getLogger().info("データベース接続プールを閉じました。");
            }
            plugin.getLogger().info("データベース接続を切断しました。");
        } catch (SQLException e) {
            plugin.getLogger().severe("データベース切断エラー: " + e.getMessage());
        }
    }
    
    private Connection getConnection() throws SQLException {
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        
        if (dbType.equalsIgnoreCase("mysql") && hikariDataSource != null) {
            // MySQLの場合は接続プールから新しい接続を取得
            return hikariDataSource.getConnection();
        }
        
        // SQLiteの場合は既存の接続を確認し、閉じている場合は再接続
        if (dbType.equalsIgnoreCase("sqlite")) {
            if (connection == null || connection.isClosed()) {
                // 再接続を試みる
                plugin.getLogger().info("SQLite接続を再確立しています...");
                connectSQLite();
            }
            return connection;
        }
        
        throw new SQLException("データベース接続が利用できません");
    }
    
    /**
     * SQLite用の接続を取得（try-with-resourcesで閉じてはいけない）
     */
    private Connection getSQLiteConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            plugin.getLogger().info("SQLite接続を再確立しています...");
            connectSQLite();
        }
        return connection;
    }
    
    /**
     * MySQL用の接続を取得（HikariCPプールから取得、try-with-resourcesで返却される）
     */
    private Connection getMySQLConnection() throws SQLException {
        if (hikariDataSource == null) {
            throw new SQLException("MySQL接続プールが初期化されていません");
        }
        return hikariDataSource.getConnection();
    }
    
    private void createTables() throws SQLException {
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        String createTableSQL;
        
        if (dbType.equalsIgnoreCase("mysql")) {
            // MySQL用のテーブル作成SQL（server_nameカラムを含む）
            createTableSQL = "CREATE TABLE IF NOT EXISTS mining_data (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "player_name VARCHAR(16) NOT NULL," +
                    "block_type VARCHAR(64) NOT NULL," +
                    "server_name VARCHAR(64) NOT NULL DEFAULT 'default'," +
                    "count INT NOT NULL DEFAULT 0," +
                    "UNIQUE KEY unique_player_block_server (player_uuid, block_type, server_name)," +
                    "INDEX idx_player_uuid (player_uuid)," +
                    "INDEX idx_block_type (block_type)," +
                    "INDEX idx_server_name (server_name)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        } else {
            // SQLite用のテーブル作成SQL（server_nameカラムを含む）
            createTableSQL = "CREATE TABLE IF NOT EXISTS mining_data (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_uuid TEXT NOT NULL," +
                    "player_name TEXT NOT NULL," +
                    "block_type TEXT NOT NULL," +
                    "server_name TEXT NOT NULL DEFAULT 'default'," +
                    "count INTEGER NOT NULL DEFAULT 0," +
                    "UNIQUE(player_uuid, block_type, server_name)" +
                    ")";
        }
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            
            // SQLiteの場合のみ個別のインデックス作成（MySQLはCREATE TABLEで作成済み）
            if (dbType.equalsIgnoreCase("sqlite")) {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_uuid ON mining_data(player_uuid)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_block_type ON mining_data(block_type)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_server_name ON mining_data(server_name)");
            }
            
            // 既存テーブルをマイグレーション
            migrateOldSchema();
        }
    }
    
    /**
     * 既存のテーブルスキーマをserver_name対応に移行
     */
    private void migrateOldSchema() throws SQLException {
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        
        try (Statement stmt = connection.createStatement()) {
            // server_nameカラムが存在するかチェック
            if (dbType.equalsIgnoreCase("mysql")) {
                try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = 'mining_data' " +
                    "AND COLUMN_NAME = 'server_name'")) {
                    
                    if (rs.next() && rs.getInt(1) == 0) {
                        // カラムが存在しない場合は追加
                        plugin.getLogger().info("データベースを移行中: server_nameカラムを追加...");
                        stmt.execute("ALTER TABLE mining_data ADD COLUMN server_name VARCHAR(64) NOT NULL DEFAULT 'default'");
                        stmt.execute("CREATE INDEX idx_server_name ON mining_data(server_name)");
                        
                        // UNIQUE KEYを更新
                        stmt.execute("ALTER TABLE mining_data DROP INDEX unique_player_block");
                        stmt.execute("ALTER TABLE mining_data ADD UNIQUE KEY unique_player_block_server (player_uuid, block_type, server_name)");
                        plugin.getLogger().info("データベースの移行が完了しました。");
                    }
                }
            } else {
                // SQLiteの場合はカラム存在チェック
                try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(mining_data)")) {
                    boolean hasServerName = false;
                    while (rs.next()) {
                        if (rs.getString("name").equals("server_name")) {
                            hasServerName = true;
                            break;
                        }
                    }
                    
                    if (!hasServerName) {
                        // SQLiteはALTER TABLEでUNIQUE制約を変更できないので、テーブルを再作成
                        plugin.getLogger().info("データベースを移行中: 新しいスキーマに移行...");
                        
                        // 一時テーブルに既存データをバックアップ
                        stmt.execute("CREATE TABLE mining_data_backup AS SELECT * FROM mining_data");
                        stmt.execute("DROP TABLE mining_data");
                        
                        // 新しいスキーマでテーブルを再作成
                        createTables();
                        
                        // データを復元（server_name = 'default'として）
                        stmt.execute(
                            "INSERT OR IGNORE INTO mining_data (player_uuid, player_name, block_type, server_name, count) " +
                            "SELECT player_uuid, player_name, block_type, 'default', count FROM mining_data_backup"
                        );
                        
                        // バックアップを削除
                        stmt.execute("DROP TABLE mining_data_backup");
                        plugin.getLogger().info("データベースの移行が完了しました。");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("データベース移行中にエラーが発生しました: " + e.getMessage());
            // 移行に失敗しても続行（新規インストールの可能性）
        }
    }
    
    /**
     * MySQL用のテーブル作成（指定された接続を使用）
     */
    private void createTablesForMySQL(Connection conn) throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS mining_data (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "player_name VARCHAR(16) NOT NULL," +
                "block_type VARCHAR(64) NOT NULL," +
                "server_name VARCHAR(64) NOT NULL DEFAULT 'default'," +
                "count INT NOT NULL DEFAULT 0," +
                "UNIQUE KEY unique_player_block_server (player_uuid, block_type, server_name)," +
                "INDEX idx_player_uuid (player_uuid)," +
                "INDEX idx_block_type (block_type)," +
                "INDEX idx_server_name (server_name)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        }
        
        // 既存テーブルのマイグレーション（MySQL専用）
        migrateMySQLSchema(conn);
    }
    
    /**
     * MySQL既存テーブルのマイグレーション
     */
    private void migrateMySQLSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // server_nameカラムが存在するかチェック
            try (ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "AND TABLE_NAME = 'mining_data' " +
                "AND COLUMN_NAME = 'server_name'")) {
                
                if (rs.next() && rs.getInt(1) == 0) {
                    // カラムが存在しない場合は追加
                    plugin.getLogger().info("MySQLデータベースを移行中: server_nameカラムを追加...");
                    stmt.execute("ALTER TABLE mining_data ADD COLUMN server_name VARCHAR(64) NOT NULL DEFAULT 'default'");
                    stmt.execute("CREATE INDEX idx_server_name ON mining_data(server_name)");
                    
                    // UNIQUE KEYを更新
                    stmt.execute("ALTER TABLE mining_data DROP INDEX unique_player_block");
                    stmt.execute("ALTER TABLE mining_data ADD UNIQUE KEY unique_player_block_server (player_uuid, block_type, server_name)");
                    plugin.getLogger().info("MySQLデータベースの移行が完了しました。");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQLデータベース移行中にエラーが発生しました: " + e.getMessage());
        }
    }
    
    public void addMiningCount(UUID playerUUID, String playerName, Material material) {
        addMiningCount(playerUUID, playerName, material, 1);
    }
    
    /**
     * 指定した採掘数を追加（インポート用）
     * @param playerUUID プレイヤーUUID
     * @param playerName プレイヤー名
     * @param material ブロックタイプ
     * @param count 追加する採掘数
     */
    public void addMiningCount(UUID playerUUID, String playerName, Material material, int count) {
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        String serverName = plugin.getConfig().getString("server-name", "default");
        String sql;
        
        if (dbType.equalsIgnoreCase("mysql")) {
            // MySQL用のUPSERT構文（server_name含む）
            sql = "INSERT INTO mining_data (player_uuid, player_name, block_type, server_name, count) " +
                  "VALUES (?, ?, ?, ?, ?) " +
                  "ON DUPLICATE KEY UPDATE count = count + ?, player_name = ?";
        } else {
            // SQLite用のUPSERT構文（server_name含む）
            sql = "INSERT INTO mining_data (player_uuid, player_name, block_type, server_name, count) " +
                  "VALUES (?, ?, ?, ?, ?) " +
                  "ON CONFLICT(player_uuid, block_type, server_name) " +
                  "DO UPDATE SET count = count + ?, player_name = ?";
        }
        
        if (dbType.equalsIgnoreCase("mysql")) {
            // MySQLの場合: 接続プールから取得、try-with-resourcesで返却
            // autoCommitがtrueの場合、executeUpdate()後に自動的にコミットされる
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                // autoCommitを明示的に有効化（リアルタイム同期のため）
                if (!conn.getAutoCommit()) {
                    conn.setAutoCommit(true);
                }
                pstmt.setString(1, playerUUID.toString());
                pstmt.setString(2, playerName);
                pstmt.setString(3, material.name());
                pstmt.setString(4, serverName);
                pstmt.setInt(5, count);  // INSERT時の初期count値
                pstmt.setInt(6, count);  // UPDATE時に既存countに加算する値
                pstmt.setString(7, playerName);
                pstmt.executeUpdate();
                // autoCommit=trueなので、ここで自動的にコミットされている
            } catch (SQLException e) {
                plugin.getLogger().warning("採掘データの保存エラー: " + e.getMessage());
            }
        } else {
            // SQLiteの場合: 永続的な接続を使用、閉じない
            try {
                Connection conn = getSQLiteConnection();
                // SQLiteもautoCommitを確実に有効化
                if (!conn.getAutoCommit()) {
                    conn.setAutoCommit(true);
                }
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, playerUUID.toString());
                    pstmt.setString(2, playerName);
                    pstmt.setString(3, material.name());
                    pstmt.setString(4, serverName);
                    pstmt.setInt(5, count);  // INSERT時の初期count値
                    pstmt.setInt(6, count);  // UPDATE時に既存countに加算する値
                    pstmt.setString(7, playerName);
                    pstmt.executeUpdate();
                    // autoCommit=trueなので、ここで自動的にコミットされている
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("採掘データの保存エラー: " + e.getMessage());
            }
        }
    }
    
    /**
     * 指定した採掘数を設定（上書き）する（強制インポート用）
     * @param playerUUID プレイヤーUUID
     * @param playerName プレイヤー名
     * @param material ブロックタイプ
     * @param count 設定する採掘数
     */
    public void setMiningCount(UUID playerUUID, String playerName, Material material, int count) {
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        String serverName = plugin.getConfig().getString("server-name", "default");
        String sql;
        
        plugin.getLogger().fine("setMiningCount called - Player: " + playerName + ", Material: " + material + ", Count: " + count);
        
        if (dbType.equalsIgnoreCase("mysql")) {
            // MySQL用のUPSERT構文（countを指定値で上書き）
            sql = "INSERT INTO mining_data (player_uuid, player_name, block_type, server_name, count) " +
                  "VALUES (?, ?, ?, ?, ?) " +
                  "ON DUPLICATE KEY UPDATE count = ?, player_name = ?";
        } else {
            // SQLite用のUPSERT構文（countを指定値で上書き）
            sql = "INSERT INTO mining_data (player_uuid, player_name, block_type, server_name, count) " +
                  "VALUES (?, ?, ?, ?, ?) " +
                  "ON CONFLICT(player_uuid, block_type, server_name) " +
                  "DO UPDATE SET count = ?, player_name = ?";
        }
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                if (!conn.getAutoCommit()) {
                    conn.setAutoCommit(true);
                }
                pstmt.setString(1, playerUUID.toString());
                pstmt.setString(2, playerName);
                pstmt.setString(3, material.name());
                pstmt.setString(4, serverName);
                pstmt.setInt(5, count);  // INSERT時の初期count値
                pstmt.setInt(6, count);  // UPDATE時に既存countを上書きする値
                pstmt.setString(7, playerName);
                int rowsAffected = pstmt.executeUpdate();
                plugin.getLogger().fine("setMiningCount - MySQL rows affected: " + rowsAffected);
            } catch (SQLException e) {
                plugin.getLogger().warning("採掘データの設定エラー (MySQL): " + e.getMessage() + 
                                         " - Player: " + playerName + ", Material: " + material);
                e.printStackTrace();
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                if (!conn.getAutoCommit()) {
                    conn.setAutoCommit(true);
                }
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, playerUUID.toString());
                    pstmt.setString(2, playerName);
                    pstmt.setString(3, material.name());
                    pstmt.setString(4, serverName);
                    pstmt.setInt(5, count);  // INSERT時の初期count値
                    pstmt.setInt(6, count);  // UPDATE時に既存countを上書きする値
                    pstmt.setString(7, playerName);
                    int rowsAffected = pstmt.executeUpdate();
                    plugin.getLogger().fine("setMiningCount - SQLite rows affected: " + rowsAffected + 
                                          " for " + material);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("採掘データの設定エラー (SQLite): " + e.getMessage() + 
                                         " - Player: " + playerName + ", Material: " + material);
                e.printStackTrace();
            }
        }
    }
    
    public Map<Material, Integer> getPlayerStats(UUID playerUUID) {
        Map<Material, Integer> stats = new HashMap<>();
        String sql = "SELECT block_type, count FROM mining_data WHERE player_uuid = ?";
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String blockType = rs.getString("block_type");
                        int count = rs.getInt("count");
                        try {
                            Material material = Material.valueOf(blockType);
                            stats.put(material, count);
                        } catch (IllegalArgumentException e) {
                            // 無効なマテリアルは無視
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("統計取得エラー: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, playerUUID.toString());
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            String blockType = rs.getString("block_type");
                            int count = rs.getInt("count");
                            try {
                                Material material = Material.valueOf(blockType);
                                stats.put(material, count);
                            } catch (IllegalArgumentException e) {
                                // 無効なマテリアルは無視
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("統計取得エラー: " + e.getMessage());
            }
        }
        
        return stats;
    }
    
    public int getTotalMined(UUID playerUUID) {
        String sql = "SELECT SUM(count) as total FROM mining_data WHERE player_uuid = ?";
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("total");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("総採掘数取得エラー: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, playerUUID.toString());
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt("total");
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("総採掘数取得エラー: " + e.getMessage());
            }
        }
        
        return 0;
    }
    
    public List<PlayerRanking> getTopPlayers(int limit, int offset) {
        List<PlayerRanking> rankings = new ArrayList<>();
        String sql = "SELECT player_uuid, player_name, SUM(count) as total " +
                     "FROM mining_data " +
                     "GROUP BY player_uuid, player_name " +
                     "ORDER BY total DESC " +
                     "LIMIT ? OFFSET ?";
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, limit);
                pstmt.setInt(2, offset);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String playerName = rs.getString("player_name");
                        int total = rs.getInt("total");
                        rankings.add(new PlayerRanking(playerName, total));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("ランキング取得エラー: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, limit);
                    pstmt.setInt(2, offset);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            String playerName = rs.getString("player_name");
                            int total = rs.getInt("total");
                            rankings.add(new PlayerRanking(playerName, total));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("ランキング取得エラー: " + e.getMessage());
            }
        }
        
        return rankings;
    }
    
    // ===== Plan連携用の新しいメソッド =====
    
    /**
     * プレイヤーの全サーバー合計採掘数を取得
     */
    public long getTotalMinedAllServers(UUID playerUUID) {
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        String sql = "SELECT SUM(count) as total FROM mining_data WHERE player_uuid = ?";
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("total");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("統計取得エラー: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, playerUUID.toString());
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong("total");
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("統計取得エラー: " + e.getMessage());
            }
        }
        return 0;
    }
    
    /**
     * プレイヤーの特定サーバーでの採掘数を取得
     */
    public long getTotalMinedByServer(UUID playerUUID, String serverName) {
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        String sql = "SELECT SUM(count) as total FROM mining_data WHERE player_uuid = ? AND server_name = ?";
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                pstmt.setString(2, serverName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("total");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("統計取得エラー: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, playerUUID.toString());
                    pstmt.setString(2, serverName);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong("total");
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("統計取得エラー: " + e.getMessage());
            }
        }
        return 0;
    }
    
    /**
     * プレイヤーのランキング順位を取得（全サーバー合計）
     */
    public long getPlayerRank(UUID playerUUID) {
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        // rankはMySQLの予約語なので、player_rankに変更
        String sql = "SELECT COUNT(DISTINCT player_uuid) + 1 as player_rank " +
                     "FROM mining_data " +
                     "WHERE player_uuid != ? " +
                     "GROUP BY player_uuid " +
                     "HAVING SUM(count) > (SELECT SUM(count) FROM mining_data WHERE player_uuid = ?)";
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                String uuid = playerUUID.toString();
                pstmt.setString(1, uuid);
                pstmt.setString(2, uuid);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("player_rank");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("ランク取得エラー: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    String uuid = playerUUID.toString();
                    pstmt.setString(1, uuid);
                    pstmt.setString(2, uuid);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong("player_rank");
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("ランク取得エラー: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return 1; // デフォルトは1位
    }
    
    /**
     * プレイヤーの特定サーバーでのブロック別統計を取得
     */
    public Map<String, Long> getPlayerStatsByServer(UUID playerUUID, String serverName) {
        Map<String, Long> stats = new HashMap<>();
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        String sql = "SELECT block_type, count FROM mining_data WHERE player_uuid = ? AND server_name = ?";
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                pstmt.setString(2, serverName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        stats.put(rs.getString("block_type"), rs.getLong("count"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("統計取得エラー: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, playerUUID.toString());
                    pstmt.setString(2, serverName);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            stats.put(rs.getString("block_type"), rs.getLong("count"));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("統計取得エラー: " + e.getMessage());
            }
        }
        return stats;
    }
    
    /**
     * プレイヤーの全サーバーでのブロック別統計を取得（集計）
     */
    public Map<String, Long> getPlayerStatsAllServers(UUID playerUUID) {
        Map<String, Long> stats = new HashMap<>();
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        String sql = "SELECT block_type, SUM(count) as total FROM mining_data WHERE player_uuid = ? GROUP BY block_type";
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        stats.put(rs.getString("block_type"), rs.getLong("total"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("統計取得エラー: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, playerUUID.toString());
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            stats.put(rs.getString("block_type"), rs.getLong("total"));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("統計取得エラー: " + e.getMessage());
            }
        }
        return stats;
    }
    
    /**
     * 特定サーバーの総採掘数を取得
     */
    public long getServerTotalMined(String serverName) {
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        String sql = "SELECT SUM(count) as total FROM mining_data WHERE server_name = ?";
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, serverName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        long total = rs.getLong("total");
                        // rs.wasNull()をチェックしてNULLの場合は0を返す
                        return rs.wasNull() ? 0 : total;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("サーバー総採掘数取得エラー: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, serverName);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            long total = rs.getLong("total");
                            // rs.wasNull()をチェックしてNULLの場合は0を返す
                            return rs.wasNull() ? 0 : total;
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("サーバー総採掘数取得エラー: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return 0;
    }
    
    /**
     * 特定サーバーでのプレイヤー数を取得
     */
    public long getServerPlayerCount(String serverName) {
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        String sql = "SELECT COUNT(DISTINCT player_uuid) as count FROM mining_data WHERE server_name = ?";
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, serverName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("count");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("統計取得エラー: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, serverName);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong("count");
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("統計取得エラー: " + e.getMessage());
            }
        }
        return 0;
    }
    
    /**
     * 特定サーバーのトップランキングを取得
     */
    public List<Map.Entry<String, Long>> getTopPlayersByServer(String serverName, int limit) {
        List<Map.Entry<String, Long>> topPlayers = new ArrayList<>();
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        String sql = "SELECT player_name, SUM(count) as total " +
                     "FROM mining_data WHERE server_name = ? " +
                     "GROUP BY player_uuid, player_name " +
                     "ORDER BY total DESC LIMIT ?";
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, serverName);
                pstmt.setInt(2, limit);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        topPlayers.add(Map.entry(
                            rs.getString("player_name"),
                            rs.getLong("total")
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("ランキング取得エラー: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, serverName);
                    pstmt.setInt(2, limit);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            topPlayers.add(Map.entry(
                                rs.getString("player_name"),
                                rs.getLong("total")
                            ));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("ランキング取得エラー: " + e.getMessage());
            }
        }
        return topPlayers;
    }
    
    /**
     * ネットワーク全体の総採掘数を取得
     */
    public long getNetworkTotalMined() {
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        String sql = "SELECT SUM(count) as total FROM mining_data";
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        long total = rs.getLong("total");
                        // rs.wasNull()をチェックしてNULLの場合は0を返す
                        return rs.wasNull() ? 0 : total;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("ネットワーク総採掘数取得エラー: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            long total = rs.getLong("total");
                            // rs.wasNull()をチェックしてNULLの場合は0を返す
                            return rs.wasNull() ? 0 : total;
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("ネットワーク総採掘数取得エラー: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return 0;
    }
    
    /**
     * ネットワーク全体のプレイヤー数を取得
     */
    public long getNetworkPlayerCount() {
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        String sql = "SELECT COUNT(DISTINCT player_uuid) as count FROM mining_data";
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("count");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("統計取得エラー: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong("count");
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("統計取得エラー: " + e.getMessage());
            }
        }
        return 0;
    }
    
    /**
     * 全サーバーのトップランキングを取得（全サーバー合計）
     */
    public List<Map.Entry<String, Long>> getTopPlayersAllServers(int limit) {
        List<Map.Entry<String, Long>> topPlayers = new ArrayList<>();
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        String sql = "SELECT player_name, SUM(count) as total " +
                     "FROM mining_data " +
                     "GROUP BY player_uuid, player_name " +
                     "ORDER BY total DESC LIMIT ?";
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, limit);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        topPlayers.add(Map.entry(
                            rs.getString("player_name"),
                            rs.getLong("total")
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("ランキング取得エラー: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, limit);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            topPlayers.add(Map.entry(
                                rs.getString("player_name"),
                                rs.getLong("total")
                            ));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("ランキング取得エラー: " + e.getMessage());
            }
        }
        return topPlayers;
    }
    
    /**
     * すべてのサーバーの統計を取得
     */
    public Map<String, Map<String, Long>> getAllServerStats() {
        Map<String, Map<String, Long>> serverStats = new HashMap<>();
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        String sql = "SELECT server_name, " +
                     "SUM(count) as totalBlocks, " +
                     "COUNT(DISTINCT player_uuid) as playerCount " +
                     "FROM mining_data " +
                     "GROUP BY server_name";
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String serverName = rs.getString("server_name");
                        Map<String, Long> stats = new HashMap<>();
                        stats.put("totalBlocks", rs.getLong("totalBlocks"));
                        stats.put("playerCount", rs.getLong("playerCount"));
                        serverStats.put(serverName, stats);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("サーバー統計取得エラー: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            String serverName = rs.getString("server_name");
                            Map<String, Long> stats = new HashMap<>();
                            stats.put("totalBlocks", rs.getLong("totalBlocks"));
                            stats.put("playerCount", rs.getLong("playerCount"));
                            serverStats.put(serverName, stats);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("サーバー統計取得エラー: " + e.getMessage());
            }
        }
        return serverStats;
    }
    
    // ===== 既存メソッドを全サーバー対応に更新 =====
    
    /**
     * プレイヤーの統計を全サーバー合計で取得（既存メソッドの互換性維持）
     */
    
    public int getTotalPlayers() {
        String sql = "SELECT COUNT(DISTINCT player_uuid) as count FROM mining_data";
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                if (rs.next()) {
                    return rs.getInt("count");
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("プレイヤー数取得エラー: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    if (rs.next()) {
                        return rs.getInt("count");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("プレイヤー数取得エラー: " + e.getMessage());
            }
        }
        
        return 0;
    }
    
    public void resetPlayerStats(UUID playerUUID) {
        String sql = "DELETE FROM mining_data WHERE player_uuid = ?";
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("統計リセットエラー: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, playerUUID.toString());
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("統計リセットエラー: " + e.getMessage());
            }
        }
    }
    
    public void resetAllStats() {
        String sql = "DELETE FROM mining_data";
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        
        if (dbType.equalsIgnoreCase("mysql")) {
            try (Connection conn = getMySQLConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                plugin.getLogger().warning("全統計リセットエラー: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(sql);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("全統計リセットエラー: " + e.getMessage());
            }
        }
    }
    
    public static class PlayerRanking {
        private final String playerName;
        private final int totalMined;
        
        public PlayerRanking(String playerName, int totalMined) {
            this.playerName = playerName;
            this.totalMined = totalMined;
        }
        
        public String getPlayerName() {
            return playerName;
        }
        
        public int getTotalMined() {
            return totalMined;
        }
    }
}
