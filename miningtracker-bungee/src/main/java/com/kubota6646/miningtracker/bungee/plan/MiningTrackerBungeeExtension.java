package com.kubota6646.miningtracker.bungee.plan;

import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.ElementOrder;
import com.djrapitops.plan.extension.ExtensionService;
import com.djrapitops.plan.extension.annotation.*;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.table.Table;
import com.kubota6646.miningtracker.bungee.MiningTrackerBungee;
import com.kubota6646.miningtracker.common.database.CommonDatabaseManager;

import java.util.Map;

/**
 * Bungeecord用のPlan Player Analytics連携拡張機能
 * ネットワーク統計のみを表示（サーバー統計は非表示）
 */
@PluginInfo(
    name = "MiningTracker",
    iconName = "pickaxe",
    iconFamily = Family.SOLID,
    color = Color.BROWN
)
@TabInfo(
    tab = "ネットワーク統計",
    iconName = "chart-bar",
    elementOrder = {ElementOrder.VALUES, ElementOrder.TABLE}
)
public class MiningTrackerBungeeExtension implements DataExtension {
    
    private final MiningTrackerBungee plugin;
    private final CommonDatabaseManager database;
    
    public MiningTrackerBungeeExtension(MiningTrackerBungee plugin) {
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
            plugin.getLogger().info("Planがインストールされていないため、Plan連携機能は無効です");
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("Planが有効化されていません: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Plan拡張機能の実装に問題があります: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
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
                entry.getKey(),
                entry.getValue(),
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
