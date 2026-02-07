package com.kubota6646.miningtracker;

import com.kubota6646.miningtracker.commands.StatsCommand;
import com.kubota6646.miningtracker.commands.RankingCommand;
import com.kubota6646.miningtracker.commands.ResetCommand;
import com.kubota6646.miningtracker.commands.ImportCommand;
import com.kubota6646.miningtracker.database.DatabaseManager;
import com.kubota6646.miningtracker.listeners.BlockBreakListener;
import com.kubota6646.miningtracker.managers.MessageManager;
import com.kubota6646.miningtracker.managers.DataManager;
import com.kubota6646.miningtracker.managers.MinecraftStatsImporter;
import com.kubota6646.miningtracker.plan.MiningTrackerExtension;
import org.bukkit.plugin.java.JavaPlugin;

public class MiningTracker extends JavaPlugin {
    
    private static MiningTracker instance;
    private DatabaseManager databaseManager;
    private MessageManager messageManager;
    private DataManager dataManager;
    private MinecraftStatsImporter statsImporter;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 設定ファイルの保存
        saveDefaultConfig();
        
        // マネージャーの初期化
        messageManager = new MessageManager(this);
        databaseManager = new DatabaseManager(this);
        dataManager = new DataManager(this);
        statsImporter = new MinecraftStatsImporter(this);
        
        // データベース接続
        if (!databaseManager.connect()) {
            getLogger().severe("データベースへの接続に失敗しました。プラグインを無効化します。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // リスナー登録
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        
        // コマンド登録
        getCommand("mtstats").setExecutor(new StatsCommand(this));
        getCommand("mtranking").setExecutor(new RankingCommand(this));
        getCommand("mtreset").setExecutor(new ResetCommand(this));
        getCommand("mtimport").setExecutor(new ImportCommand(this));
        
        // Plan連携の登録
        registerPlanHook();
        
        getLogger().info("MiningTracker が有効化されました。");
    }
    
    @Override
    public void onDisable() {
        // データベース切断
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        
        getLogger().info("MiningTracker が無効化されました。");
    }
    
    /**
     * Plan Player Analyticsとの連携を登録
     */
    private void registerPlanHook() {
        try {
            // Planプラグインが存在するか確認
            if (getServer().getPluginManager().getPlugin("Plan") != null) {
                // DataExtensionを作成して登録
                MiningTrackerExtension extension = new MiningTrackerExtension(this);
                extension.register();
            } else {
                getLogger().info("Plan Player Analyticsが見つかりません。通常モードで動作します。");
            }
        } catch (NoClassDefFoundError e) {
            // Planのクラスが見つからない場合（Planがインストールされていない）
            getLogger().info("Plan Player Analyticsが見つかりません。通常モードで動作します。");
        } catch (Exception e) {
            // その他のエラー
            getLogger().warning("Plan連携の初期化中にエラーが発生しました: " + e.getMessage());
        }
    }
    
    public static MiningTracker getInstance() {
        return instance;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public MinecraftStatsImporter getStatsImporter() {
        return statsImporter;
    }
}
