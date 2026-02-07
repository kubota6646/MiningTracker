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
        // 設定でインポート機能が無効化されている場合はスキップ
        if (!plugin.getConfig().getBoolean("import.enabled", true)) {
            return false;
        }
        
        // 統計ファイルを取得
        File statsFile = getStatsFile(playerUUID);
        if (statsFile == null || !statsFile.exists()) {
            plugin.getLogger().fine("統計ファイルが見つかりません: " + playerUUID);
            return false;
        }
        
        try {
            // 統計データを読み込み
            Map<Material, Integer> miningData = parseStatsFile(statsFile);
            
            if (miningData.isEmpty()) {
                plugin.getLogger().fine("統計ファイルに採掘データがありません: " + playerName);
                return false;
            }
            
            // データベースにインポート
            int importedBlocks = 0;
            for (Map.Entry<Material, Integer> entry : miningData.entrySet()) {
                plugin.getDatabaseManager().addMiningCount(playerUUID, playerName, entry.getKey(), entry.getValue());
                importedBlocks++;
            }
            
            plugin.getLogger().info(String.format(
                "プレイヤー %s の統計をインポートしました: %d種類のブロック",
                playerName, importedBlocks
            ));
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, 
                "統計ファイルの読み込みエラー: " + playerName, e);
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
        World mainWorld = Bukkit.getWorlds().get(0);
        if (mainWorld == null) {
            return null;
        }
        
        File worldFolder = mainWorld.getWorldFolder();
        File statsFolder = new File(worldFolder, "stats");
        
        // UUID.jsonファイルを取得
        return new File(statsFolder, playerUUID.toString() + ".json");
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
