package com.kubota6646.miningtracker.listeners;

import com.kubota6646.miningtracker.MiningTracker;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class BlockBreakListener implements Listener {
    
    private final MiningTracker plugin;
    
    public BlockBreakListener(MiningTracker plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();
        
        // トラッキングが無効な場合は処理しない
        if (!plugin.getDataManager().isTrackingEnabled()) {
            return;
        }
        
        // クリエイティブモードのチェック
        if (player.getGameMode() == GameMode.CREATIVE && !plugin.getDataManager().shouldCountCreative()) {
            return;
        }
        
        // シルクタッチのチェック
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            if (!plugin.getDataManager().shouldCountSilkTouch()) {
                return;
            }
        }
        
        // 空気ブロックは無視
        if (blockType == Material.AIR) {
            return;
        }
        
        // 採掘カウントを増やす
        plugin.getDataManager().incrementBlockBreak(
            player.getUniqueId(),
            player.getName(),
            blockType
        );
    }
}
