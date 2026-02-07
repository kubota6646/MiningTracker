package com.kubota6646.miningtracker.config;

import com.kubota6646.miningtracker.common.config.ConfigAdapter;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Bukkit用の設定アダプター
 */
public class BukkitConfigAdapter implements ConfigAdapter {
    
    private final FileConfiguration config;
    
    public BukkitConfigAdapter(FileConfiguration config) {
        this.config = config;
    }
    
    @Override
    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }
    
    @Override
    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }
    
    @Override
    public long getLong(String path, long defaultValue) {
        return config.getLong(path, defaultValue);
    }
}
