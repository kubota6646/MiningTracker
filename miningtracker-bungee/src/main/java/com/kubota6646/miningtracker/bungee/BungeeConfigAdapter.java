package com.kubota6646.miningtracker.bungee;

import com.kubota6646.miningtracker.common.config.ConfigAdapter;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;

/**
 * Bungeecord用の設定アダプター
 */
public class BungeeConfigAdapter implements ConfigAdapter {
    
    private final Plugin plugin;
    private final Configuration config;
    
    public BungeeConfigAdapter(Plugin plugin, Configuration config) {
        this.plugin = plugin;
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
