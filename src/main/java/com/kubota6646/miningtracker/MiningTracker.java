package com.kubota6646.miningtracker;

import com.kubota6646.miningtracker.commands.StatsCommand;
import com.kubota6646.miningtracker.commands.RankingCommand;
import com.kubota6646.miningtracker.commands.ResetCommand;
import com.kubota6646.miningtracker.database.DatabaseManager;
import com.kubota6646.miningtracker.listeners.BlockBreakListener;
import com.kubota6646.miningtracker.managers.MessageManager;
import com.kubota6646.miningtracker.managers.DataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MiningTracker extends JavaPlugin {
    
    private static MiningTracker instance;
    private DatabaseManager databaseManager;
    private MessageManager messageManager;
    private DataManager dataManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 設定ファイルの保存
        saveDefaultConfig();
        
        // マネージャーの初期化
        messageManager = new MessageManager(this);
        databaseManager = new DatabaseManager(this);
        dataManager = new DataManager(this);
        
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
}
