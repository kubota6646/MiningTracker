package com.kubota6646.miningtracker.commands;

import com.kubota6646.miningtracker.MiningTracker;
import com.kubota6646.miningtracker.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class StatsCommand implements CommandExecutor {
    
    private final MiningTracker plugin;
    private final MessageManager messages;
    
    public StatsCommand(MiningTracker plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // 自分の統計を表示
            if (!(sender instanceof Player)) {
                sender.sendMessage(messages.getPrefix() + "§cこのコマンドはプレイヤーのみ実行できます。");
                return true;
            }
            
            Player player = (Player) sender;
            showStats(sender, player);
        } else {
            // 他のプレイヤーの統計を表示
            if (!sender.hasPermission("miningtracker.other")) {
                sender.sendMessage(messages.getPrefix() + messages.getMessage("plugin.no-permission"));
                return true;
            }
            
            String targetName = args[0];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(messages.getPrefix() + 
                    messages.getMessage("plugin.player-not-found", "player", targetName));
                return true;
            }
            
            showStatsAsync(sender, target);
        }
        
        return true;
    }
    
    private void showStats(CommandSender sender, OfflinePlayer target) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<Material, Integer> stats = plugin.getDatabaseManager().getPlayerStats(target.getUniqueId());
            int total = plugin.getDatabaseManager().getTotalMined(target.getUniqueId());
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (stats.isEmpty()) {
                    sender.sendMessage(messages.getPrefix() + 
                        messages.getMessage("plugin.player-no-data", "player", target.getName()));
                    return;
                }
                
                sender.sendMessage(messages.getMessage("stats.header", "player", target.getName()));
                sender.sendMessage(messages.getMessage("stats.total", "total", String.valueOf(total)));
                sender.sendMessage(messages.getMessage("stats.top-blocks-header"));
                
                // トップ5ブロックを表示
                List<Map.Entry<Material, Integer>> topBlocks = stats.entrySet().stream()
                    .sorted(Map.Entry.<Material, Integer>comparingByValue().reversed())
                    .limit(5)
                    .collect(Collectors.toList());
                
                int rank = 1;
                for (Map.Entry<Material, Integer> entry : topBlocks) {
                    String blockName = formatMaterialName(entry.getKey());
                    sender.sendMessage(messages.getMessage("stats.block-entry",
                        "rank", String.valueOf(rank),
                        "block", blockName,
                        "count", String.valueOf(entry.getValue())));
                    rank++;
                }
                
                sender.sendMessage(messages.getMessage("stats.footer"));
            });
        });
    }
    
    private void showStatsAsync(CommandSender sender, OfflinePlayer target) {
        showStats(sender, target);
    }
    
    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
}
