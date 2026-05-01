package su.starlight.da.alert.luckyblock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import su.starlight.da.util.ItemContainer;

import java.util.Random;

public final class RedLuckyBlock extends LuckyBlockBehaviour {

    public RedLuckyBlock() {
        super(0.6, 0.25, 0.15);
    }

    @Override
    public void onItem(Player player, Random random, ItemContainer container) {
        switch (random.nextInt(8)) {
            case 0 -> container.add(Material.STONE_SWORD, meta -> meta.addEnchant(Enchantment.SHARPNESS, 1, false));
            case 1 -> container.add(Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS);
            case 2 -> container.add(Material.POISONOUS_POTATO, 8);
            case 3 -> container.add(Material.NETHERRACK, 10);
            case 4 -> container.add(
                    new ItemStack(Material.SAND, 4),
                    new ItemStack(Material.CACTUS, 4)
            );
            case 5 -> container.add(
                    Material.STONE_SWORD,
                    Material.STONE_PICKAXE,
                    Material.STONE_AXE,
                    Material.STONE_SHOVEL,
                    Material.STONE_HOE
            );
            case 6 -> container.add(
                    Material.IRON_HELMET,
                    Material.IRON_BOOTS
            );
            case 7 -> container.add(Material.WOODEN_PICKAXE);
        }
    }

    @Override
    public void onStructure(Player player, Random random, Block block) {
        placeStructure("red", player, block);
    }

    @Override
    public void onMob(Player player, Random random, Block block) {
        int roll = random.nextInt(10) + 1;
        World world = block.getWorld();
        Location location = block.getLocation().add(.5, 0, .5);
        if(roll <= 1) {
            Zombie zombie = world.spawn(location, Zombie.class);
            var equipment = zombie.getEquipment();
            equipment.setHelmet(new ItemStack(Material.LEATHER_HELMET));
            equipment.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
            equipment.setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
            equipment.setBoots(new ItemStack(Material.LEATHER_BOOTS));
        }
        else if (roll <= 3) for (int i = 0; i < 10; i++) {
            world.spawn(location, Chicken.class);
        }
        else if (roll <= 6) for (int i = 0; i < 5; i++) {
            world.spawn(location, Bat.class);
        }
        else for (int i = 0; i < 3; i++) {
            world.spawn(location, Silverfish.class);
        }
    }

}
