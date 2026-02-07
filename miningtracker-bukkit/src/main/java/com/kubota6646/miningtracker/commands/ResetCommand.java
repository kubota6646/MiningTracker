package com.kubota6646.miningtracker.commands;

import com.kubota6646.miningtracker.MiningTracker;
import com.kubota6646.miningtracker.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ResetCommand implements CommandExecutor {
    
    private final MiningTracker plugin;
    private final MessageManager messages;
    
    public ResetCommand(MiningTracker plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("miningtracker.reset")) {
            sender.sendMessage(messages.getPrefix() + messages.getMessage("plugin.no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage(messages.getPrefix() + messages.getMessage("reset.usage"));
            return true;
        }
        
        String target = args[0];
        boolean confirmed = args.length > 1 && args[1].equalsIgnoreCase("confirm");
        
        if (!confirmed) {
            sender.sendMessage(messages.getPrefix() + messages.getMessage("reset.confirm", "target", target));
            return true;
        }
        
        if (target.equalsIgnoreCase("all")) {
            resetAllAsync(sender);
        } else {
            OfflinePlayer player = Bukkit.getOfflinePlayer(target);
            
            if (!player.hasPlayedBefore() && !player.isOnline()) {
                sender.sendMessage(messages.getPrefix() + 
                    messages.getMessage("plugin.player-not-found", "player", target));
                return true;
            }
            
            resetPlayerAsync(sender, player);
        }
        
        return true;
    }
    
    private void resetPlayerAsync(CommandSender sender, OfflinePlayer player) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().resetPlayerStats(player.getUniqueId());
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(messages.getPrefix() + 
                    messages.getMessage("reset.success-player", "player", player.getName()));
            });
        });
    }
    
    private void resetAllAsync(CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().resetAllStats();
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(messages.getPrefix() + messages.getMessage("reset.success-all"));
            });
        });
    }
}
