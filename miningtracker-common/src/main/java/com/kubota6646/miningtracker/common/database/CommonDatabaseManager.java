package com.kubota6646.miningtracker.common.database;

import com.kubota6646.miningtracker.common.config.ConfigAdapter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * 共通データベースマネージャー
 * BukkitとBungeecordの両方で使用可能
 */
public class CommonDatabaseManager {
    
    private final ConfigAdapter config;
    private final File dataFolder;
    private final Logger logger;
    private Connection connection;
    private HikariDataSource hikariDataSource;
    
    public CommonDatabaseManager(ConfigAdapter config, File dataFolder, Logger logger) {
        this.config = config;
        this.dataFolder = dataFolder;
        this.logger = logger;
    }
    
    public boolean connect() {
        try {
            String dbType = config.getString("database.type", "mysql");
            
            if (dbType.equalsIgnoreCase("sqlite")) {
                return connectSQLite();
            } else if (dbType.equalsIgnoreCase("mysql")) {
                return connectMySQL();
            } else {
                logger.severe("サポートされていないデータベースタイプ: " + dbType);
                return false;
            }
        } catch (SQLException e) {
            logger.severe("データベース接続エラー: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    private boolean connectSQLite() throws SQLException {
        String fileName = config.getString("database.sqlite.file", "mining_data.db");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        File dbFile = new File(dataFolder, fileName);
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        
        connection = DriverManager.getConnection(url);
        
        logger.info("SQLiteデータベースに接続しました。");
        return true;
    }
    
    private boolean connectMySQL() throws SQLException {
        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String database = config.getString("database.mysql.database", "minecraft");
        String username = config.getString("database.mysql.username", "root");
        String password = config.getString("database.mysql.password", "password");
        
        int maxPoolSize = config.getInt("database.mysql.pool.maximum-pool-size", 10);
        int minIdle = config.getInt("database.mysql.pool.minimum-idle", 2);
        long connectionTimeout = config.getLong("database.mysql.pool.connection-timeout", 30000);
        
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
        
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        // リアルタイム同期を確実にするための設定
        hikariConfig.setAutoCommit(true);  // 明示的にautoCommitを有効化
        
        try {
            hikariDataSource = new HikariDataSource(hikariConfig);
            logger.info("MySQLデータベース（HikariCP）に接続しました。");
            return true;
        } catch (Exception e) {
            logger.severe("MySQL接続エラー: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public void disconnect() {
        try {
            if (hikariDataSource != null && !hikariDataSource.isClosed()) {
                hikariDataSource.close();
            }
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.warning("データベース切断エラー: " + e.getMessage());
        }
    }
    
    private Connection getConnection() throws SQLException {
        if (hikariDataSource != null) {
            return hikariDataSource.getConnection();
        }
        return connection;
    }
    
    // Plan統計用メソッド
    
    public long getNetworkTotalMined() {
        String sql = "SELECT SUM(count) FROM mining_data";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                long total = rs.getLong(1);
                // rs.wasNull()をチェックしてNULLの場合は0を返す
                return rs.wasNull() ? 0 : total;
            }
        } catch (SQLException e) {
            logger.warning("ネットワーク総採掘数取得エラー: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
    
    public long getNetworkPlayerCount() {
        String sql = "SELECT COUNT(DISTINCT player_uuid) FROM mining_data";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.warning("ネットワークプレイヤー数取得エラー: " + e.getMessage());
        }
        return 0;
    }
    
    public List<Map.Entry<String, Long>> getTopPlayersAllServers(int limit) {
        String sql = "SELECT player_name, SUM(count) as total FROM mining_data " +
                     "GROUP BY player_uuid, player_name ORDER BY total DESC LIMIT ?";
        List<Map.Entry<String, Long>> result = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(Map.entry(rs.getString("player_name"), rs.getLong("total")));
                }
            }
        } catch (SQLException e) {
            logger.warning("トッププレイヤー取得エラー: " + e.getMessage());
        }
        return result;
    }
    
    public Map<String, Map<String, Long>> getAllServerStats() {
        String sql = "SELECT server_name, COUNT(DISTINCT player_uuid) as playerCount, " +
                     "SUM(count) as totalBlocks FROM mining_data " +
                     "GROUP BY server_name";
        Map<String, Map<String, Long>> result = new HashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String serverName = rs.getString("server_name");
                Map<String, Long> stats = new HashMap<>();
                stats.put("playerCount", rs.getLong("playerCount"));
                stats.put("totalBlocks", rs.getLong("totalBlocks"));
                result.put(serverName, stats);
            }
        } catch (SQLException e) {
            logger.warning("サーバー統計取得エラー: " + e.getMessage());
        }
        return result;
    }
}
