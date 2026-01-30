package com.kubota6646.miningtracker.database;

import com.kubota6646.miningtracker.MiningTracker;
import org.bukkit.Material;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    
    private final MiningTracker plugin;
    private Connection connection;
    
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
        
        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, database);
        
        connection = DriverManager.getConnection(url, username, password);
        createTables();
        
        plugin.getLogger().info("MySQLデータベースに接続しました。");
        return true;
    }
    
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("データベース接続を切断しました。");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("データベース切断エラー: " + e.getMessage());
        }
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
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, playerName);
            pstmt.setString(3, material.name());
            pstmt.setString(4, playerName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("採掘データの保存エラー: " + e.getMessage());
        }
    }
    
    public Map<Material, Integer> getPlayerStats(UUID playerUUID) {
        Map<Material, Integer> stats = new HashMap<>();
        String sql = "SELECT block_type, count FROM mining_data WHERE player_uuid = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
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
        
        return stats;
    }
    
    public int getTotalMined(UUID playerUUID) {
        String sql = "SELECT SUM(count) as total FROM mining_data WHERE player_uuid = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("総採掘数取得エラー: " + e.getMessage());
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
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
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
        
        return rankings;
    }
    
    public int getTotalPlayers() {
        String sql = "SELECT COUNT(DISTINCT player_uuid) as count FROM mining_data";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("プレイヤー数取得エラー: " + e.getMessage());
        }
        
        return 0;
    }
    
    public void resetPlayerStats(UUID playerUUID) {
        String sql = "DELETE FROM mining_data WHERE player_uuid = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("統計リセットエラー: " + e.getMessage());
        }
    }
    
    public void resetAllStats() {
        String sql = "DELETE FROM mining_data";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            plugin.getLogger().warning("全統計リセットエラー: " + e.getMessage());
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
