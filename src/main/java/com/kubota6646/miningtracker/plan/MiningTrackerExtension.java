package com.kubota6646.miningtracker.plan;

import com.djrapitops.plan.extension.ExtensionService;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.ElementOrder;
import com.djrapitops.plan.extension.annotation.*;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.table.Table;
import com.kubota6646.miningtracker.MiningTracker;
import com.kubota6646.miningtracker.database.DatabaseManager;

import java.util.Map;
import java.util.UUID;

/**
 * Plan Player Analytics との連携拡張機能
 * プレイヤーの採掘統計をPlanに表示します
 */
@PluginInfo(
    name = "MiningTracker",
    iconName = "pickaxe",
    iconFamily = Family.SOLID,
    color = Color.BROWN
)
@TabInfo(
    tab = "採掘統計",
    iconName = "chart-bar",
    elementOrder = {ElementOrder.VALUES, ElementOrder.TABLE, ElementOrder.GRAPH}
)
public class MiningTrackerExtension implements DataExtension {
    
    private final MiningTracker plugin;
    private final DatabaseManager database;
    
    public MiningTrackerExtension(MiningTracker plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabaseManager();
    }
    
    /**
     * Planに拡張機能を登録
     */
    public void register() {
        try {
            ExtensionService.getInstance().register(this);
            plugin.getLogger().info("Plan拡張機能を正常に登録しました");
        } catch (NoClassDefFoundError e) {
            // Planがインストールされていない場合
            plugin.getLogger().info("Planがインストールされていないため、Plan連携機能は無効です");
        } catch (IllegalStateException e) {
            // Planが有効化されていない場合
            plugin.getLogger().warning("Planが有効化されていません: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // DataExtension実装に問題がある場合
            plugin.getLogger().warning("Plan拡張機能の実装に問題があります: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // プレイヤー別データ
    
    @NumberProvider(
        text = "総採掘ブロック数",
        description = "このプレイヤーの総採掘ブロック数（全サーバー合計）",
        priority = 100,
        iconName = "cubes",
        iconColor = Color.BROWN,
        showInPlayerTable = true
    )
    public long totalBlocksMined(UUID playerUUID) {
        return database.getTotalMinedAllServers(playerUUID);
    }
    
    @NumberProvider(
        text = "総採掘ブロック数（このサーバー）",
        description = "このサーバーでの総採掘ブロック数",
        priority = 90,
        iconName = "cube",
        iconColor = Color.AMBER
    )
    public long totalBlocksMinedThisServer(UUID playerUUID) {
        String serverName = plugin.getConfig().getString("server-name", "default");
        return database.getTotalMinedByServer(playerUUID, serverName);
    }
    
    @NumberProvider(
        text = "採掘ランキング順位",
        description = "採掘ランキング順位（全サーバー合計）",
        priority = 80,
        iconName = "trophy",
        iconColor = Color.AMBER
    )
    public long miningRank(UUID playerUUID) {
        return database.getPlayerRank(playerUUID);
    }
    
    @TableProvider(tableColor = Color.BROWN)
    @Tab("採掘統計")
    public Table burokku_shubetsu_naiwake(UUID playerUUID) {
        Table.Factory table = Table.builder()
            .columnOne("ブロック種類", Icon.called("cube").build())
            .columnTwo("採掘数（このサーバー）", Icon.called("hashtag").build())
            .columnThree("採掘数（全サーバー）", Icon.called("cubes").build());
        
        String serverName = plugin.getConfig().getString("server-name", "default");
        Map<String, Long> thisServerStats = database.getPlayerStatsByServer(playerUUID, serverName);
        Map<String, Long> allServerStats = database.getPlayerStatsAllServers(playerUUID);
        
        // すべてのブロックタイプを集計
        allServerStats.forEach((blockType, totalCount) -> {
            long thisServerCount = thisServerStats.getOrDefault(blockType, 0L);
            table.addRow(blockType, thisServerCount, totalCount);
        });
        
        return table.build();
    }
    
    // サーバー別データ
    
    @NumberProvider(
        text = "サーバー総採掘数",
        description = "このサーバーでの総採掘ブロック数（全プレイヤー合計）",
        priority = 100,
        iconName = "server",
        iconColor = Color.BROWN,
        showInPlayerTable = false
    )
    @Tab("サーバー統計")
    public long serverTotalBlocksMined() {
        String serverName = plugin.getConfig().getString("server-name", "default");
        return database.getServerTotalMined(serverName);
    }
    
    @NumberProvider(
        text = "アクティブマイナー数",
        description = "このサーバーで採掘したプレイヤー数",
        priority = 90,
        iconName = "users",
        iconColor = Color.LIGHT_BLUE
    )
    @Tab("サーバー統計")
    public long serverActiveMiners() {
        String serverName = plugin.getConfig().getString("server-name", "default");
        return database.getServerPlayerCount(serverName);
    }
    
    @TableProvider(tableColor = Color.BROWN)
    @Tab("サーバー統計")
    public Table toppu_maina() {
        Table.Factory table = Table.builder()
            .columnOne("プレイヤー", Icon.called("user").build())
            .columnTwo("採掘数", Icon.called("cubes").build())
            .columnThree("順位", Icon.called("trophy").build());
        
        String serverName = plugin.getConfig().getString("server-name", "default");
        var topPlayers = database.getTopPlayersByServer(serverName, 10);
        
        int rank = 1;
        for (var entry : topPlayers) {
            table.addRow(
                entry.getKey(), // player name
                entry.getValue(), // block count
                rank++
            );
        }
        
        return table.build();
    }
    
    // ネットワーク全体データ
    
    @NumberProvider(
        text = "ネットワーク総採掘数",
        description = "全サーバー合計の総採掘ブロック数",
        priority = 100,
        iconName = "globe",
        iconColor = Color.GREEN,
        showInPlayerTable = false
    )
    @Tab("ネットワーク統計")
    public long networkTotalBlocksMined() {
        return database.getNetworkTotalMined();
    }
    
    @NumberProvider(
        text = "総アクティブマイナー数",
        description = "全サーバーで採掘したプレイヤーの総数",
        priority = 90,
        iconName = "users",
        iconColor = Color.LIGHT_GREEN
    )
    @Tab("ネットワーク統計")
    public long networkActiveMiners() {
        return database.getNetworkPlayerCount();
    }
    
    @TableProvider(tableColor = Color.GREEN)
    @Tab("ネットワーク統計")
    public Table nettowaku_toppu_maina() {
        Table.Factory table = Table.builder()
            .columnOne("プレイヤー", Icon.called("user").build())
            .columnTwo("総採掘数", Icon.called("cubes").build())
            .columnThree("順位", Icon.called("trophy").build());
        
        var topPlayers = database.getTopPlayersAllServers(10);
        
        int rank = 1;
        for (var entry : topPlayers) {
            table.addRow(
                entry.getKey(), // player name
                entry.getValue(), // block count
                rank++
            );
        }
        
        return table.build();
    }
    
    @TableProvider(tableColor = Color.BLUE)
    @Tab("ネットワーク統計")
    public Table saba_hikaku() {
        Table.Factory table = Table.builder()
            .columnOne("サーバー", Icon.called("server").build())
            .columnTwo("総採掘数", Icon.called("cubes").build())
            .columnThree("プレイヤー数", Icon.called("users").build());
        
        var serverStats = database.getAllServerStats();
        
        serverStats.forEach((serverName, stats) -> {
            table.addRow(
                serverName,
                stats.get("totalBlocks"),
                stats.get("playerCount")
            );
        });
        
        return table.build();
    }
}
