package com.kubota6646.miningtracker.managers;

import com.kubota6646.miningtracker.MiningTracker;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MessageManager {
    
    private final MiningTracker plugin;
    private FileConfiguration messages;
    private File messagesFile;
    
    public MessageManager(MiningTracker plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    
    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // デフォルト設定を読み込み
        InputStream defConfigStream = plugin.getResource("messages.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
            messages.setDefaults(defConfig);
        }
    }
    
    public String getMessage(String path) {
        String message = messages.getString(path, "");
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        
        // 置換処理
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        
        return message;
    }
    
    public String getPrefix() {
        return getMessage("plugin.prefix");
    }
    
    public void reload() {
        loadMessages();
    }
}
