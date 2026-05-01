package su.starlight.da.alert.luckyblock;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import su.starlight.da.util.ItemContainer;
import su.starlight.da.util.RandomUtil;

import java.util.Random;

public final class GreenLuckyBlock extends LuckyBlockBehaviour {

    public GreenLuckyBlock() {
        super(0.8, 0.15, 0.05);
    }

    @Override
    public void onItem(Player player, Random random, ItemContainer container) {
        switch (random.nextInt(22)) {
            case 0 -> container.add(Material.OAK_PLANKS, 42);
            case 1 -> {
                container.add(Material.IRON_INGOT, 8);
                container.add(Material.CRAFTING_TABLE);
            }
            case 2 -> container.add(Material.ENDER_PEARL);
            case 3 -> container.add(Material.IRON_SWORD, meta -> meta.addEnchant(Enchantment.SHARPNESS, 2, false));
            case 4 -> {
                container.add(Material.TNT, 8);
                container.add(Material.FLINT_AND_STEEL);
            }
            case 5 -> container.add(Material.SHIELD);
            case 6 -> {
                container.add(Material.ARROW, 4);
                container.add(Material.BOW);
            }
            case 7 -> container.add(Material.ENCHANTED_GOLDEN_APPLE);
            case 8 -> container.add(Material.CROSSBOW, meta0 -> {
                CrossbowMeta meta = (CrossbowMeta) meta0;
                ItemStack stack = new ItemStack(Material.FIREWORK_ROCKET);
                FireworkMeta firework = (FireworkMeta) stack.getItemMeta();
                FireworkEffect effect = FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.RED)
                        .withColor(Color.RED)
                        .withColor(Color.RED)
                        .withColor(Color.RED)
                        .withColor(Color.RED)
                        .withColor(Color.RED)
                        .build();
                firework.addEffects(effect, effect, effect, effect, effect, effect, effect);
                stack.setItemMeta(firework);

                meta.addChargedProjectile(stack);
            });
            default -> container.add(RandomUtil.chooseRandomly(
                    Material.ZOMBIFIED_PIGLIN_SPAWN_EGG,
                    Material.STRAY_SPAWN_EGG,
                    Material.SHULKER_SPAWN_EGG,
                    Material.EVOKER_SPAWN_EGG,
                    Material.CREEPER_SPAWN_EGG,
                    Material.ZOMBIE_SPAWN_EGG,
                    Material.ENDERMITE_SPAWN_EGG,
                    Material.PHANTOM_SPAWN_EGG,
                    Material.ZOMBIE_VILLAGER_SPAWN_EGG,
                    Material.WITCH_SPAWN_EGG,
                    Material.SLIME_SPAWN_EGG,
                    Material.WITHER_SKELETON_SPAWN_EGG
            ));
        }
    }

    @Override
    public void onStructure(Player player, Random random, Block block) {
        placeStructure("green", player, block);
    }

    @Override
    public void onMob(Player player, Random random, Block block) {
        World world = block.getWorld();
        Location location = block.getLocation().add(.5, 0, .5);
        if(random.nextInt(19) == 18) for (int i = 0; i < 8; i++) {
            world.spawn(location, Bat.class);
        }
        else world.spawn(location, RandomUtil.<Class<? extends Entity>>chooseRandomly(
                Sniffer.class,
                Phantom.class,
                Parrot.class,
                Llama.class,
                Ocelot.class,
                Mule.class,
                Bogged.class,
                MushroomCow.class,
                Panda.class,
                Sheep.class,
                Salmon.class,
                Ravager.class,
                Rabbit.class,
                Skeleton.class,
                Stray.class,
                WanderingTrader.class,
                Strider.class,
                PolarBear.class
        ));
    }

}
