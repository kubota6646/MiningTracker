package com.kubota6646.miningtracker.commands;

import com.kubota6646.miningtracker.MiningTracker;
import com.kubota6646.miningtracker.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ImportCommand implements CommandExecutor {
    
    private final MiningTracker plugin;
    private final MessageManager messages;
    
    public ImportCommand(MiningTracker plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.getLogger().info("ImportCommand executed by " + sender.getName() + " with args: " + String.join(", ", args));
        
        if (!sender.hasPermission("miningtracker.import")) {
            plugin.getLogger().info("Permission denied for " + sender.getName());
            sender.sendMessage(messages.getPrefix() + messages.getMessage("plugin.no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            plugin.getLogger().info("No arguments provided");
            sender.sendMessage(messages.getPrefix() + messages.getMessage("import.usage"));
            return true;
        }
        
        String target = args[0];
        boolean confirmed = args.length > 1 && args[1].equalsIgnoreCase("confirm");
        
        plugin.getLogger().info("Target player: " + target + ", Confirmed: " + confirmed);
        
        if (!confirmed) {
            sender.sendMessage(messages.getPrefix() + messages.getMessage("import.confirm", "target", target));
            return true;
        }
        
        OfflinePlayer player = Bukkit.getOfflinePlayer(target);
        
        plugin.getLogger().info("Player lookup result - UUID: " + player.getUniqueId() + 
                              ", Name: " + player.getName() + 
                              ", HasPlayedBefore: " + player.hasPlayedBefore() + 
                              ", IsOnline: " + player.isOnline());
        
        if (!player.hasPlayedBefore() && !player.isOnline()) {
            plugin.getLogger().info("Player not found or never played: " + target);
            sender.sendMessage(messages.getPrefix() + 
                messages.getMessage("plugin.player-not-found", "player", target));
            return true;
        }
        
        plugin.getLogger().info("Initiating import for player: " + target);
        importPlayerAsync(sender, player);
        
        return true;
    }
    
    private void importPlayerAsync(CommandSender sender, OfflinePlayer player) {
        plugin.getLogger().info("Starting async import task for player: " + player.getName() + " (UUID: " + player.getUniqueId() + ")");
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getLogger().info("Async task started - calling importPlayerStats");
                
                // 強制上書きモードでインポート
                boolean success = plugin.getStatsImporter().importPlayerStats(
                    player.getUniqueId(), 
                    player.getName(),
                    true  // forceOverwrite = true
                );
                
                plugin.getLogger().info("Import result for " + player.getName() + ": " + (success ? "SUCCESS" : "FAILED"));
                
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getLogger().info("Sending chat message to " + sender.getName());
                    if (success) {
                        String message = messages.getPrefix() + messages.getMessage("import.success", "player", player.getName());
                        plugin.getLogger().info("Success message: " + message);
                        sender.sendMessage(message);
                    } else {
                        String message = messages.getPrefix() + messages.getMessage("import.failed", "player", player.getName());
                        plugin.getLogger().info("Failed message: " + message);
                        sender.sendMessage(message);
                    }
                    plugin.getLogger().info("Chat message sent");
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Exception during import for " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
                
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(messages.getPrefix() + 
                        "&cエラーが発生しました: " + e.getMessage());
                });
            }
        });
    }
}
