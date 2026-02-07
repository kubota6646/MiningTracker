package com.kubota6646.miningtracker.managers;

import com.kubota6646.miningtracker.MiningTracker;
import org.bukkit.Material;

import java.util.UUID;

public class DataManager {
    
    private final MiningTracker plugin;
    
    public DataManager(MiningTracker plugin) {
        this.plugin = plugin;
    }
    
    public void incrementBlockBreak(UUID playerUUID, String playerName, Material material) {
        // 非同期でデータベースに保存
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().addMiningCount(playerUUID, playerName, material);
            
            // Planのキャッシュを無効化してリアルタイム更新
            if (plugin.getPlanExtension() != null) {
                plugin.getPlanExtension().invalidatePlayerCache(playerUUID);
                plugin.getPlanExtension().invalidateServerCache();
            }
        });
    }
    
    public boolean isTrackingEnabled() {
        return plugin.getConfig().getBoolean("tracking.enabled", true);
    }
    
    public boolean shouldCountCreative() {
        return plugin.getConfig().getBoolean("tracking.count-creative", false);
    }
    
    public boolean shouldCountSilkTouch() {
        return plugin.getConfig().getBoolean("tracking.count-silk-touch", true);
    }
}
