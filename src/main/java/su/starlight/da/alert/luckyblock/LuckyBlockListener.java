package su.starlight.da.alert.luckyblock;

import org.bukkit.block.Block;
import org.bukkit.entity.Silverfish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import su.starlight.da.CorePlugin;

import java.util.Random;

public final class LuckyBlockListener implements Listener {

    private static final RedLuckyBlock RED = new RedLuckyBlock();
    private static final GreenLuckyBlock GREEN = new GreenLuckyBlock();
    private static final YellowLuckyBlock YELLOW = new YellowLuckyBlock();

    private final CorePlugin plugin;

    public LuckyBlockListener(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if(event.getEntity() instanceof Silverfish) event.setCancelled(true);
    }

    @EventHandler public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Random random = new Random();
        LuckyBlockBehaviour behaviour = switch (block.getType()) {
            case INFESTED_STONE -> GREEN;
            case INFESTED_COBBLESTONE -> RED;
            case INFESTED_STONE_BRICKS -> YELLOW;
            default -> null;
        };
        plugin.taskUtil().run(() -> {
            if(behaviour != null) behaviour.onActivate(event.getPlayer(), random, block);
        }, 0L);
    }

}
