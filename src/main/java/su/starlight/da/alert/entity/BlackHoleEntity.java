package su.starlight.da.alert.entity;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import su.starlight.da.CorePlugin;
import su.starlight.da.util.RandomUtil;
import su.starlight.da.util.task.TaskContext;
import su.starlight.da.util.task.TaskUtil;

import java.util.ArrayList;
import java.util.List;

public final class BlackHoleEntity {

    public static final long DEFAULT_LIFESPAN = 60 * 20L; // one minute
    public static final int ITEM_TAKE_COOLDOWN = 3 * 20; // 3 seconds
    public static final int ACTIVATION_RADIUS = 32;
    public static final int ITEM_TRAVEL_TIME = 3 * 20 - 1; // 3 seconds

    private final long lifespan;

    private ItemDisplay entity;
    private long lifetime;

    public BlackHoleEntity() {
        this(DEFAULT_LIFESPAN);
    }

    public BlackHoleEntity(long lifespan) {
        this.lifespan = lifespan;
    }

    @SuppressWarnings("UnstableApiUsage")
    public void create(Location location) {
        if(entity != null) return;
        entity = location.getWorld().spawn(location, ItemDisplay.class);
        entity.setItemStack(new ItemStack(Material.WOODEN_HOE){{
            editMeta(meta -> {
                CustomModelDataComponent component = meta.getCustomModelDataComponent();
                component.setStrings(List.of("black_hole"));
                meta.setCustomModelDataComponent(component);
            });
        }});
        entity.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf(),
                new Vector3f(10f),
                new Quaternionf()
        ));
        entity.setBillboard(Display.Billboard.CENTER);

        CorePlugin.instance().taskUtil().startRepeating(this::tick, 0L, 1L);
    }

    private void tick(TaskContext ctx) {
        if(lifetime >= lifespan) {
            entity.remove();
            ctx.cancel();
            return;
        }

        if((lifetime + 1) % ITEM_TAKE_COOLDOWN == 0) attemptToTakeItems();

        lifetime++;
    }

    private void attemptToTakeItems() {
        Location location = entity.getLocation();

        location.getWorld().getEntitiesByClass(Item.class)
                .parallelStream()
                .filter(item -> item.getLocation().distanceSquared(location) <= ACTIVATION_RADIUS * ACTIVATION_RADIUS)
                .forEach(item -> {
                    takeItem(item.getItemStack(), item.getLocation());
                    item.remove();
                });

        Bukkit.getOnlinePlayers().parallelStream()
                .filter(player -> player.getWorld().equals(location.getWorld()))
                .filter(player -> player.getLocation().distanceSquared(location) <= ACTIVATION_RADIUS * ACTIVATION_RADIUS)
                .forEach(player -> {
                    ItemStack[] contents = player.getInventory().getContents();

                    List<Integer> candidates = new ArrayList<>();
                    for (int i = 0; i < contents.length; i++) {
                        ItemStack stack = contents[i];
                        if(stack != null) candidates.add(i);
                    }

                    int chosen = RandomUtil.chooseRandomly(candidates);
                    ItemStack stack = contents[chosen];
                    takeItem(stack, player.getLocation());
                    player.getInventory().setItem(chosen, null);
                });
    }

    private void takeItem(ItemStack stack, Location from) {
        ItemDisplay display = from.getWorld().spawn(from, ItemDisplay.class);
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(ITEM_TRAVEL_TIME);
        display.setTeleportDuration(ITEM_TRAVEL_TIME);
        display.setItemStack(stack);
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
        display.setBillboard(Display.Billboard.CENTER);

        TaskUtil taskUtil = CorePlugin.instance().taskUtil();
        taskUtil.run(() -> display.teleport(entity.getLocation()), 1);
        taskUtil.run(display::remove, 1 + ITEM_TRAVEL_TIME);
    }

}
