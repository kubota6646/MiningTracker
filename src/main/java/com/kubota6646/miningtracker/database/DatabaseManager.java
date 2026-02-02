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
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
        
        try {
            hikariDataSource = new HikariDataSource(hikariConfig);
            connection = hikariDataSource.getConnection();
            createTables();
            
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
            // MySQL用のテーブル作成SQL
            createTableSQL = "CREATE TABLE IF NOT EXISTS mining_data (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "player_name VARCHAR(16) NOT NULL," +
                    "block_type VARCHAR(64) NOT NULL," +
                    "count INT NOT NULL DEFAULT 0," +
                    "UNIQUE KEY unique_player_block (player_uuid, block_type)," +
                    "INDEX idx_player_uuid (player_uuid)," +
                    "INDEX idx_block_type (block_type)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        } else {
            // SQLite用のテーブル作成SQL
            createTableSQL = "CREATE TABLE IF NOT EXISTS mining_data (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_uuid TEXT NOT NULL," +
                    "player_name TEXT NOT NULL," +
                    "block_type TEXT NOT NULL," +
                    "count INTEGER NOT NULL DEFAULT 0," +
                    "UNIQUE(player_uuid, block_type)" +
                    ")";
        }
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            
            // SQLiteの場合のみ個別のインデックス作成（MySQLはCREATE TABLEで作成済み）
            if (dbType.equalsIgnoreCase("sqlite")) {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_uuid ON mining_data(player_uuid)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_block_type ON mining_data(block_type)");
            }
        }
    }
    
    public void addMiningCount(UUID playerUUID, String playerName, Material material) {
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        String sql;
        
        if (dbType.equalsIgnoreCase("mysql")) {
            // MySQL用のUPSERT構文
            sql = "INSERT INTO mining_data (player_uuid, player_name, block_type, count) " +
                  "VALUES (?, ?, ?, 1) " +
                  "ON DUPLICATE KEY UPDATE count = count + 1, player_name = ?";
        } else {
            // SQLite用のUPSERT構文
            sql = "INSERT INTO mining_data (player_uuid, player_name, block_type, count) " +
                  "VALUES (?, ?, ?, 1) " +
                  "ON CONFLICT(player_uuid, block_type) " +
                  "DO UPDATE SET count = count + 1, player_name = ?";
        }
        
        if (dbType.equalsIgnoreCase("mysql")) {
            // MySQLの場合: 接続プールから取得、try-with-resourcesで返却
            try (Connection conn = getMySQLConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                pstmt.setString(2, playerName);
                pstmt.setString(3, material.name());
                pstmt.setString(4, playerName);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("採掘データの保存エラー: " + e.getMessage());
            }
        } else {
            // SQLiteの場合: 永続的な接続を使用、閉じない
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, playerUUID.toString());
                    pstmt.setString(2, playerName);
                    pstmt.setString(3, material.name());
                    pstmt.setString(4, playerName);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("採掘データの保存エラー: " + e.getMessage());
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
                ResultSet rs = pstmt.executeQuery();
                
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
            } catch (SQLException e) {
                plugin.getLogger().warning("統計取得エラー: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, playerUUID.toString());
                    ResultSet rs = pstmt.executeQuery();
                    
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
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    return rs.getInt("total");
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("総採掘数取得エラー: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = getSQLiteConnection();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, playerUUID.toString());
                    ResultSet rs = pstmt.executeQuery();
                    
                    if (rs.next()) {
                        return rs.getInt("total");
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
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    String playerName = rs.getString("player_name");
                    int total = rs.getInt("total");
                    rankings.add(new PlayerRanking(playerName, total));
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
                    ResultSet rs = pstmt.executeQuery();
                    
                    while (rs.next()) {
                        String playerName = rs.getString("player_name");
                        int total = rs.getInt("total");
                        rankings.add(new PlayerRanking(playerName, total));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("ランキング取得エラー: " + e.getMessage());
            }
        }
        
        return rankings;
    }
    
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
