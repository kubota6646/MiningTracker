package com.kubota6646.miningtracker.managers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kubota6646.miningtracker.MiningTracker;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Minecraftの統計ファイルからデータをインポートするクラス
 */
public class MinecraftStatsImporter {
    
    private final MiningTracker plugin;
    
    public MinecraftStatsImporter(MiningTracker plugin) {
        this.plugin = plugin;
    }
    
    /**
     * プレイヤーのMinecraft統計ファイルから採掘データをインポート
     * 
     * @param playerUUID プレイヤーのUUID
     * @param playerName プレイヤー名
     * @return インポートに成功した場合true
     */
    public boolean importPlayerStats(UUID playerUUID, String playerName) {
        return importPlayerStats(playerUUID, playerName, false);
    }
    
    /**
     * プレイヤーのMinecraft統計ファイルから採掘データをインポート
     * 
     * @param playerUUID プレイヤーのUUID
     * @param playerName プレイヤー名
     * @param forceOverwrite 既存データを上書きするかどうか
     * @return インポートに成功した場合true
     */
    public boolean importPlayerStats(UUID playerUUID, String playerName, boolean forceOverwrite) {
        plugin.getLogger().info("importPlayerStats called - UUID: " + playerUUID + 
                              ", Name: " + playerName + 
                              ", forceOverwrite: " + forceOverwrite);
        
        // 設定でインポート機能が無効化されている場合はスキップ（強制モードは除く）
        if (!forceOverwrite && !plugin.getConfig().getBoolean("import.enabled", true)) {
            plugin.getLogger().info("Import disabled in config and not forced");
            return false;
        }
        
        // 統計ファイルを取得
        File statsFile = getStatsFile(playerUUID);
        plugin.getLogger().info("Stats file path: " + (statsFile != null ? statsFile.getAbsolutePath() : "null"));
        
        if (statsFile == null || !statsFile.exists()) {
            plugin.getLogger().warning("統計ファイルが見つかりません: " + playerUUID + 
                                     " (Path: " + (statsFile != null ? statsFile.getAbsolutePath() : "null") + ")");
            return false;
        }
        
        plugin.getLogger().info("Stats file found, attempting to parse: " + statsFile.getAbsolutePath());
        
        try {
            // 統計データを読み込み
            Map<Material, Integer> miningData = parseStatsFile(statsFile);
            
            plugin.getLogger().info("Parsed " + miningData.size() + " block types from stats file");
            
            if (miningData.isEmpty()) {
                plugin.getLogger().warning("統計ファイルに採掘データがありません: " + playerName);
                return false;
            }
            
            // データベースに一括インポート（バッチ処理）
            int importedBlocks = 0;
            
            if (forceOverwrite) {
                // バッチ処理を使用して一括で上書き
                importedBlocks = plugin.getDatabaseManager().setBatchMiningCount(playerUUID, playerName, miningData);
            } else {
                // 追加モードの場合は個別に処理（既存の動作を維持）
                int failedBlocks = 0;
                for (Map.Entry<Material, Integer> entry : miningData.entrySet()) {
                    try {
                        plugin.getDatabaseManager().addMiningCount(playerUUID, playerName, entry.getKey(), entry.getValue());
                        importedBlocks++;
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to import block " + entry.getKey() + ": " + e.getMessage());
                        failedBlocks++;
                    }
                }
                plugin.getLogger().info("Import summary (add mode) - Success: " + importedBlocks + ", Failed: " + failedBlocks);
            }
            
            plugin.getLogger().info("Import summary - Successfully imported: " + importedBlocks + " blocks");
            
            if (forceOverwrite) {
                plugin.getLogger().info(String.format(
                    "プレイヤー %s の統計を上書きインポートしました: %d種類のブロック",
                    playerName, importedBlocks
                ));
            } else {
                plugin.getLogger().info(String.format(
                    "プレイヤー %s の統計をインポートしました: %d種類のブロック",
                    playerName, importedBlocks
                ));
            }
            
            // データベース操作が1つも成功しなかった場合は失敗とみなす
            if (importedBlocks == 0) {
                plugin.getLogger().warning("No blocks were successfully imported for " + playerName);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, 
                "統計ファイルの読み込みエラー: " + playerName + " (File: " + statsFile.getAbsolutePath() + ")", e);
            return false;
        }
    }
    
    /**
     * プレイヤーの統計ファイルを取得
     * 
     * @param playerUUID プレイヤーのUUID
     * @return 統計ファイル、見つからない場合null
     */
    private File getStatsFile(UUID playerUUID) {
        // メインワールドの統計フォルダを取得
        // ワールドリストが空の場合に備えてstream().findFirst()を使用
        World mainWorld = Bukkit.getWorlds().stream()
            .findFirst()
            .orElse(null);
        
        if (mainWorld == null) {
            plugin.getLogger().warning("ワールドが見つかりません。統計をインポートできません。");
            return null;
        }
        
        plugin.getLogger().info("Main world found: " + mainWorld.getName());
        
        File worldFolder = mainWorld.getWorldFolder();
        File statsFolder = new File(worldFolder, "stats");
        
        plugin.getLogger().info("World folder: " + worldFolder.getAbsolutePath());
        plugin.getLogger().info("Stats folder: " + statsFolder.getAbsolutePath());
        plugin.getLogger().info("Stats folder exists: " + statsFolder.exists());
        
        // UUID.jsonファイルを取得
        File statsFile = new File(statsFolder, playerUUID.toString() + ".json");
        plugin.getLogger().info("Looking for stats file: " + statsFile.getAbsolutePath());
        plugin.getLogger().info("Stats file exists: " + statsFile.exists());
        
        return statsFile;
    }
    
    /**
     * 統計ファイルをパースして採掘データを抽出
     * 
     * @param statsFile 統計ファイル
     * @return マテリアルと採掘数のマップ
     * @throws IOException ファイル読み込みエラー
     */
    private Map<Material, Integer> parseStatsFile(File statsFile) throws IOException {
        Map<Material, Integer> miningData = new HashMap<>();
        
        try (FileReader reader = new FileReader(statsFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            
            // stats.minecraft:mined セクションを取得
            if (!root.has("stats")) {
                return miningData;
            }
            
            JsonObject stats = root.getAsJsonObject("stats");
            if (!stats.has("minecraft:mined")) {
                return miningData;
            }
            
            JsonObject minedStats = stats.getAsJsonObject("minecraft:mined");
            
            // 各ブロックの採掘数を取得
            for (Map.Entry<String, JsonElement> entry : minedStats.entrySet()) {
                String blockKey = entry.getKey();
                int count = entry.getValue().getAsInt();
                
                // "minecraft:stone" -> "STONE" に変換
                Material material = parseMaterial(blockKey);
                if (material != null && material.isBlock()) {
                    miningData.put(material, count);
                }
            }
        }
        
        return miningData;
    }
    
    /**
     * Minecraft統計のブロックキーをMaterialに変換
     * 
     * @param blockKey 例: "minecraft:stone"
     * @return Material、変換できない場合null
     */
    private Material parseMaterial(String blockKey) {
        try {
            // "minecraft:" プレフィックスを削除
            String materialName = blockKey.replace("minecraft:", "").toUpperCase();
            return Material.matchMaterial(materialName);
        } catch (Exception e) {
            return null;
        }
    }
}
