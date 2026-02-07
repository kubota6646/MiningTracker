package com.kubota6646.miningtracker.bungee;

import com.kubota6646.miningtracker.bungee.plan.MiningTrackerBungeeExtension;
import com.kubota6646.miningtracker.common.database.CommonDatabaseManager;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

/**
 * MiningTracker Bungeecord Plugin
 * Bungeecord環境でのPlan Player Analytics統合
 * ネットワーク統計のみを表示
 */
public class MiningTrackerBungee extends Plugin {
    
    private static MiningTrackerBungee instance;
    private Configuration config;
    private CommonDatabaseManager databaseManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 設定ファイルの読み込み
        loadConfig();
        
        // データベース接続
        databaseManager = new CommonDatabaseManager(
            new BungeeConfigAdapter(this, config),
            getDataFolder(),
            getLogger()
        );
        
        if (!databaseManager.connect()) {
            getLogger().severe("データベースへの接続に失敗しました。プラグインを無効化します。");
            return;
        }
        
        // Plan連携の登録
        registerPlanHook();
        
        getLogger().info("MiningTracker (Bungee) が有効化されました。");
    }
    
    @Override
    public void onDisable() {
        // データベース切断
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        
        getLogger().info("MiningTracker (Bungee) が無効化されました。");
    }
    
    /**
     * 設定ファイルを読み込む
     */
    private void loadConfig() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
            }
            
            File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                try (InputStream in = getResourceAsStream("config.yml")) {
                    if (in != null) {
                        Files.copy(in, configFile.toPath());
                    }
                }
            }
            
            config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(configFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "設定ファイルの読み込みに失敗しました", e);
        }
    }
    
    /**
     * Plan Player Analyticsとの連携を登録
     */
    private void registerPlanHook() {
        try {
            // Planプラグインが存在するか確認
            if (getProxy().getPluginManager().getPlugin("Plan") != null) {
                // DataExtensionを作成して登録
                MiningTrackerBungeeExtension extension = new MiningTrackerBungeeExtension(this);
                extension.register();
            } else {
                getLogger().info("Plan Player Analyticsが見つかりません。");
            }
        } catch (NoClassDefFoundError e) {
            // Planのクラスが見つからない場合
            getLogger().info("Plan Player Analyticsが見つかりません。");
        } catch (Exception e) {
            // その他のエラー
            getLogger().warning("Plan連携の初期化中にエラーが発生しました: " + e.getMessage());
        }
    }
    
    public static MiningTrackerBungee getInstance() {
        return instance;
    }
    
    public Configuration getConfiguration() {
        return config;
    }
    
    public CommonDatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
