package com.kubota6646.miningtracker.commands;

import com.kubota6646.miningtracker.MiningTracker;
import com.kubota6646.miningtracker.database.DatabaseManager.PlayerRanking;
import com.kubota6646.miningtracker.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class RankingCommand implements CommandExecutor {
    
    private final MiningTracker plugin;
    private final MessageManager messages;
    
    public RankingCommand(MiningTracker plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int page = 1;
        
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(messages.getPrefix() + messages.getMessage("ranking.invalid-page"));
                return true;
            }
        }
        
        if (page < 1) {
            page = 1;
        }
        
        showRankingAsync(sender, page);
        return true;
    }
    
    private void showRankingAsync(CommandSender sender, int page) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            int perPage = plugin.getConfig().getInt("ranking.per-page", 10);
            int offset = (page - 1) * perPage;
            
            List<PlayerRanking> rankings = plugin.getDatabaseManager().getTopPlayers(perPage, offset);
            int totalPlayers = plugin.getDatabaseManager().getTotalPlayers();
            int totalPages = (int) Math.ceil((double) totalPlayers / perPage);
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (rankings.isEmpty()) {
                    sender.sendMessage(messages.getPrefix() + messages.getMessage("ranking.empty"));
                    return;
                }
                
                if (page > totalPages) {
                    sender.sendMessage(messages.getPrefix() + messages.getMessage("ranking.invalid-page"));
                    return;
                }
                
                sender.sendMessage(messages.getMessage("ranking.header",
                    "page", String.valueOf(page),
                    "total_pages", String.valueOf(totalPages)));
                
                int startRank = offset + 1;
                for (int i = 0; i < rankings.size(); i++) {
                    PlayerRanking ranking = rankings.get(i);
                    sender.sendMessage(messages.getMessage("ranking.entry",
                        "rank", String.valueOf(startRank + i),
                        "player", ranking.getPlayerName(),
                        "count", String.valueOf(ranking.getTotalMined())));
                }
                
                sender.sendMessage(messages.getMessage("ranking.footer"));
            });
        });
    }
}
