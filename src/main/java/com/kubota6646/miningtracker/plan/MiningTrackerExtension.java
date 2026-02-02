package com.kubota6646.miningtracker.plan;

import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
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
    tab = "Mining Stats",
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
            CapabilityService.getInstance().registerExtension(this);
        } catch (Exception e) {
            plugin.getLogger().warning("Plan拡張機能の登録に失敗しました: " + e.getMessage());
        }
    }
    
    public CallEvents[] callExtensionMethodsOn() {
        return new CallEvents[]{
            CallEvents.PLAYER_JOIN,
            CallEvents.PLAYER_LEAVE,
            CallEvents.SERVER_PERIODICAL
        };
    }
    
    // プレイヤー別データ
    
    @NumberProvider(
        text = "Total Blocks Mined",
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
        text = "Total Blocks Mined (This Server)",
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
        text = "Mining Rank",
        description = "採掘ランキング順位（全サーバー合計）",
        priority = 80,
        iconName = "trophy",
        iconColor = Color.AMBER
    )
    public long miningRank(UUID playerUUID) {
        return database.getPlayerRank(playerUUID);
    }
    
    @TableProvider(tableColor = Color.BROWN)
    @Tab("Mining Stats")
    public Table blockTypeBreakdown(UUID playerUUID) {
        Table.Factory table = Table.builder()
            .columnOne("Block Type", Icon.called("cube").build())
            .columnTwo("Count (This Server)", Icon.called("hashtag").build())
            .columnThree("Count (All Servers)", Icon.called("cubes").build());
        
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
        text = "Server Total Blocks Mined",
        description = "このサーバーでの総採掘ブロック数（全プレイヤー合計）",
        priority = 100,
        iconName = "server",
        iconColor = Color.BROWN,
        showInPlayerTable = false
    )
    @Tab("Server Stats")
    public long serverTotalBlocksMined() {
        String serverName = plugin.getConfig().getString("server-name", "default");
        return database.getServerTotalMined(serverName);
    }
    
    @NumberProvider(
        text = "Active Miners",
        description = "このサーバーで採掘したプレイヤー数",
        priority = 90,
        iconName = "users",
        iconColor = Color.LIGHT_BLUE
    )
    @Tab("Server Stats")
    public long serverActiveMiners() {
        String serverName = plugin.getConfig().getString("server-name", "default");
        return database.getServerPlayerCount(serverName);
    }
    
    @TableProvider(tableColor = Color.BROWN)
    @Tab("Server Stats")
    public Table serverTopMiners() {
        Table.Factory table = Table.builder()
            .columnOne("Player", Icon.called("user").build())
            .columnTwo("Blocks Mined", Icon.called("cubes").build())
            .columnThree("Rank", Icon.called("trophy").build());
        
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
        text = "Network Total Blocks Mined",
        description = "全サーバー合計の総採掘ブロック数",
        priority = 100,
        iconName = "globe",
        iconColor = Color.GREEN,
        showInPlayerTable = false
    )
    @Tab("Network Stats")
    public long networkTotalBlocksMined() {
        return database.getNetworkTotalMined();
    }
    
    @NumberProvider(
        text = "Total Active Miners",
        description = "全サーバーで採掘したプレイヤーの総数",
        priority = 90,
        iconName = "users",
        iconColor = Color.LIGHT_GREEN
    )
    @Tab("Network Stats")
    public long networkActiveMiners() {
        return database.getNetworkPlayerCount();
    }
    
    @TableProvider(tableColor = Color.GREEN)
    @Tab("Network Stats")
    public Table networkTopMiners() {
        Table.Factory table = Table.builder()
            .columnOne("Player", Icon.called("user").build())
            .columnTwo("Total Blocks", Icon.called("cubes").build())
            .columnThree("Rank", Icon.called("trophy").build());
        
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
    @Tab("Network Stats")
    public Table serverComparison() {
        Table.Factory table = Table.builder()
            .columnOne("Server", Icon.called("server").build())
            .columnTwo("Total Blocks", Icon.called("cubes").build())
            .columnThree("Players", Icon.called("users").build());
        
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
