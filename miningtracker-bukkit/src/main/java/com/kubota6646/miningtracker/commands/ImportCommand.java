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
        if (!sender.hasPermission("miningtracker.import")) {
            sender.sendMessage(messages.getPrefix() + messages.getMessage("plugin.no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage(messages.getPrefix() + messages.getMessage("import.usage"));
            return true;
        }
        
        String target = args[0];
        boolean confirmed = args.length > 1 && args[1].equalsIgnoreCase("confirm");
        
        if (!confirmed) {
            sender.sendMessage(messages.getPrefix() + messages.getMessage("import.confirm", "target", target));
            return true;
        }
        
        OfflinePlayer player = Bukkit.getOfflinePlayer(target);
        
        if (!player.hasPlayedBefore() && !player.isOnline()) {
            sender.sendMessage(messages.getPrefix() + 
                messages.getMessage("plugin.player-not-found", "player", target));
            return true;
        }
        
        importPlayerAsync(sender, player);
        
        return true;
    }
    
    private void importPlayerAsync(CommandSender sender, OfflinePlayer player) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // 強制上書きモードでインポート
            boolean success = plugin.getStatsImporter().importPlayerStats(
                player.getUniqueId(), 
                player.getName(),
                true  // forceOverwrite = true
            );
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    sender.sendMessage(messages.getPrefix() + 
                        messages.getMessage("import.success", "player", player.getName()));
                } else {
                    sender.sendMessage(messages.getPrefix() + 
                        messages.getMessage("import.failed", "player", player.getName()));
                }
            });
        });
    }
}
