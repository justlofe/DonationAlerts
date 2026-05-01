package su.starlight.da.alert.luckyblock;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.structure.Structure;
import su.starlight.da.CorePlugin;
import su.starlight.da.util.ItemContainer;
import su.starlight.da.util.RandomUtil;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public abstract class LuckyBlockBehaviour {

    private final double itemChance, mobChance;

    public LuckyBlockBehaviour(double itemChance, double mobChance, double structureChance) {
        if(itemChance + mobChance + structureChance != 1) throw new RuntimeException("Chances should be a summary of 1");
        this.itemChance = itemChance;
        this.mobChance = mobChance;
    }

    public void onActivate(Player player, Random random, Block block) {
        double a = random.nextDouble();
        if (a < itemChance) {
            ItemContainer container = new ItemContainer();
            onItem(player, random, container);

            Location location = block.getLocation().add(.5, .5, .5);
            World world = location.getWorld();
            for (ItemStack item : container.items()) {
                world.dropItemNaturally(location, item);
            }
        }
        else if (a < itemChance + mobChance) onMob(player, random, block);
        else onStructure(player, random, block);
    }

    public abstract void onItem(Player player, Random random, ItemContainer container);
    public abstract void onStructure(Player player, Random random, Block block);
    public abstract void onMob(Player player, Random random, Block block);

    public static void placeStructure(String id, Player player, Block block) {
        placeStructure(RandomUtil.chooseRandomly(CorePlugin.instance().getStructures(id)), player, block);
    }

    public static void placeStructure(Structure structure, Player player, Block block) {
        if(structure == null) return;

        Location origin = block.getLocation().add(-2, 0, -2);
        AtomicReference<Location> loc = new AtomicReference<>(null);
        structure.place(
                origin,
                true,
                StructureRotation.NONE,
                Mirror.NONE,
                0,
                1,
                new Random(),
                Set.of(new StructureTransformer(pos -> loc.set(
                        new Location(origin.getWorld(), pos.getX() + .5, pos.getY(), pos.getZ() + .5, player.getYaw(), player.getPitch())
                ))),
                Set.of()
        );

        Location location1 = loc.get();
        if(location1 != null) player.teleport(location1);
    }

}
