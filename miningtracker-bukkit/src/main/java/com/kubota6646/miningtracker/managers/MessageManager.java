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
        
        plugin.getLogger().info("Loading messages from: " + messagesFile.getAbsolutePath());
        plugin.getLogger().info("Messages file exists: " + messagesFile.exists());
        
        if (!messagesFile.exists()) {
            plugin.getLogger().info("Messages file doesn't exist, saving default from resources");
            plugin.saveResource("messages.yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // デフォルト設定を読み込み
        InputStream defConfigStream = plugin.getResource("messages.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
            messages.setDefaults(defConfig);
            plugin.getLogger().info("Default messages loaded from resources");
        }
        
        // import.successメッセージが存在するか確認
        String importSuccess = messages.getString("import.success", "NOT_FOUND");
        plugin.getLogger().info("Loaded import.success message: '" + importSuccess + "'");
    }
    
    public String getMessage(String path) {
        String message = messages.getString(path, "");
        plugin.getLogger().info("getMessage called - path: " + path + ", raw value: '" + message + "'");
        
        // メッセージが空の場合、デフォルト設定から取得を試みる
        if (message == null || message.trim().isEmpty()) {
            plugin.getLogger().warning("Message '" + path + "' is empty, trying to get from defaults");
            if (messages.getDefaults() != null) {
                message = messages.getDefaults().getString(path, "");
                plugin.getLogger().info("Got from defaults: '" + message + "'");
            }
            if (message == null || message.trim().isEmpty()) {
                plugin.getLogger().severe("Message '" + path + "' not found in config or defaults!");
                return "[Message not found: " + path + "]";
            }
        }
        
        String translated = ChatColor.translateAlternateColorCodes('&', message);
        plugin.getLogger().info("getMessage - after color translation: '" + translated + "'");
        return translated;
    }
    
    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        plugin.getLogger().info("getMessage with replacements - path: " + path + ", before replace: '" + message + "'");
        
        // 置換処理
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                plugin.getLogger().info("Replacing {" + replacements[i] + "} with '" + replacements[i + 1] + "'");
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        
        plugin.getLogger().info("getMessage - after replacements: '" + message + "'");
        return message;
    }
    
    public String getPrefix() {
        return getMessage("plugin.prefix");
    }
    
    public void reload() {
        loadMessages();
    }
}
