package su.starlight.da.alert.entity;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import su.starlight.da.CorePlugin;
import su.starlight.da.util.task.TaskContext;

import java.util.List;

public final class FollowingEntity {

    private final Player player;
    private final long lifespan;
    private final String type;

    private long lifetime;
    private long currentChunk;
    private Entity entity;

    public FollowingEntity(Player player, long lifespan, String type) {
        this.player = player;
        this.lifespan = lifespan;
        this.type = type;
    }

    @SuppressWarnings("UnstableApiUsage")
    public void create() {
        if(entity != null) return;
        Location loc = player.getLocation();
        ItemDisplay entity = loc.getWorld().spawn(loc, ItemDisplay.class);
        entity.setItemStack(new ItemStack(Material.WOODEN_HOE){{
            editMeta(meta -> {
                CustomModelDataComponent component = meta.getCustomModelDataComponent();
                component.setStrings(List.of(type));
                meta.setCustomModelDataComponent(component);
            });
        }});
        this.entity = entity;

        CorePlugin.instance().taskUtil().startRepeating(this::tick, 0L, 1L);
    }

    private void tick(TaskContext ctx) {
        if(lifetime >= lifespan) {
            entity.remove();
            ctx.cancel();
            return;
        }

        long chunk = player.getChunk().getChunkKey();
        if(chunk != currentChunk) {
            entity.teleportAsync(player.getLocation());
            currentChunk = chunk;
        }

        lifetime++;
    }

}
