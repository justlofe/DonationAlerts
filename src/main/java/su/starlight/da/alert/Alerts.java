package su.starlight.da.alert;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;
import su.starlight.da.CorePlugin;
import su.starlight.da.ProfanityFilter;
import su.starlight.da.alert.entity.BlackHoleEntity;
import su.starlight.da.alert.entity.BossEntity;
import su.starlight.da.alert.entity.CasinoEntity;
import su.starlight.da.alert.entity.FollowingEntity;
import su.starlight.da.util.RandomUtil;
import su.starlight.da.util.Text;

import java.time.Duration;
import java.util.*;

public final class Alerts {

    public static final NamespacedKey
            ALERT_EXPLOSION = NamespacedKey.minecraft("alert_explosion"),
            MEGA_SWORD = NamespacedKey.minecraft("mega_sword"),
            CANCEL_FALL_DAMAGE = NamespacedKey.minecraft("cancel_fall_damage");

    private static final Material[] LUCKY_BLOCKS = {
            Material.INFESTED_STONE,       // green
            Material.INFESTED_COBBLESTONE, // red
            Material.INFESTED_STONE_BRICKS // yellow
    };

    private static final Set<Material> BANNED_ITEMS = Set.of(
            Material.COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.BARRIER,
            Material.ENDER_DRAGON_SPAWN_EGG,
            Material.BEDROCK,
            Material.STRUCTURE_BLOCK,
            Material.COMMAND_BLOCK_MINECART,
            Material.JIGSAW,
            Material.WRITTEN_BOOK,

            Material.INFESTED_STONE,
            Material.INFESTED_COBBLESTONE,
            Material.INFESTED_STONE_BRICKS
    );

    private final CorePlugin corePlugin;
    private final ProfanityFilter filter;

    private final Map<String, Alert> TYPES = new HashMap<>();
    private final Map<Double, String> PRICES = new HashMap<>();
    private final AlertMonitor MONITOR = AlertMonitor.create();

    private final Map<Player, AdAlert> ADS = new HashMap<>();
    private final Map<Player, HelpersAlert> HELPERS = new HashMap<>();

    private Alerts(CorePlugin corePlugin) {
        this.corePlugin = corePlugin;
        this.filter = new ProfanityFilter(
                Set.copyOf(corePlugin.getConfig().getStringList("profanity")),
                "#"
        );
    }

    public Set<String> getTypes() {
        return TYPES.keySet();
    }

    public void setCanBeHelper(String player, boolean canBeHelper) {
        String name = player.toLowerCase();

        Set<String> helpers = possibleHelpers();
        if(canBeHelper) helpers.add(name);
        else helpers.remove(name);

        corePlugin.getConfig().set("helpers", List.copyOf(helpers));
    }

    public boolean canBeHelper(String player) {
        return corePlugin.getConfig().getStringList("helpers").contains(player.toLowerCase());
    }

    public Set<String> possibleHelpers() {
        return new HashSet<>(corePlugin.getConfig().getStringList("helpers"));
    }

    public void loadPrices() {
        PRICES.clear();
        ConfigurationSection alerts = corePlugin.getConfig().getConfigurationSection("alerts");
        if(alerts == null) return;
        for (String key : alerts.getKeys(false)) {
            PRICES.put(alerts.getDouble(key, 0), key);
        }
    }

    public void execute(Player player, String donator, String donationMessage, double summary) {
        var opt = Optional.ofNullable(PRICES.get(summary)).map(TYPES::get);
        if(opt.isEmpty()) return;
        execute(player, donator, donationMessage, opt.get());
    }

    public void execute(Player player, String donator, String donationMessage, String alertType) {
        Alert alert = TYPES.get(alertType);
        if(alert != null) execute(player, donator, donationMessage, alert);
    }

    private void execute(Player player, String donator, String donationMessage, Alert alert) {
        if(alert == null) return;
        String formattedDonator = filter.filterText(donator);
        corePlugin.advancementDisplay().show(player, "donationalerts:" + alert.key(), Text.create(String.format(
                alert.message(),
                "<yellow>" + formattedDonator + "</yellow>"
        )));
        corePlugin.taskUtil().run(() -> execute0(player, formattedDonator, donationMessage, alert), 20L);
    }

    private void execute0(Player player, String donator, String donationMessage, Alert alert) {
        long startTime = System.currentTimeMillis();
        try {
            alert.executor().execute(new AlertContext(donator, donationMessage, player));
        }
        catch (Throwable ex) {
            ex.printStackTrace();
        }
        MONITOR.addCompletionTime(alert.key(), System.currentTimeMillis() - startTime);
    }

    public AlertMonitor monitor() {
        return MONITOR;
    }

    private void create(String key, String message, AlertExecutor executor) {
        Alert alert = new Alert(key, message, executor);
        TYPES.put(key, alert);
        MONITOR.register(key);
    }

    public static Alerts create(CorePlugin corePlugin) {
        return new Alerts(corePlugin);
    }

    public static Alerts createDefault(CorePlugin corePlugin) {
        return create(corePlugin).initializeDefault();
    }

    private Alerts initializeDefault() {
        create("set_night", "%s set the day", _ -> Bukkit.getWorld("world").setTime(18000));

        create("set_day", "%s set the night", _ -> Bukkit.getWorld("world").setTime(1000));

        create("give_meat", "%s gave you 16 beefs", ctx -> ctx.giveWithName("<yellow>" + ctx.donator(), new ItemStack(Material.COOKED_BEEF, 16)));

        create("netherite_tools", "%s gave you netherite tools", ctx -> ctx.giveWithName(
                ctx.donator(),
                new ItemStack(Material.NETHERITE_PICKAXE),
                new ItemStack(Material.NETHERITE_SHOVEL),
                new ItemStack(Material.NETHERITE_AXE),
                new ItemStack(Material.NETHERITE_HOE)
        ));

        create("netherite_armor", "%s gave you netherite armor", ctx -> ctx.giveWithName(
                ctx.donator(),
                new ItemStack(Material.NETHERITE_HELMET),
                new ItemStack(Material.NETHERITE_CHESTPLATE),
                new ItemStack(Material.NETHERITE_LEGGINGS),
                new ItemStack(Material.NETHERITE_BOOTS)
        ));

        create("shuffle_inventory", "%s shuffled your inventory!", ctx -> {
            var inv = ctx.player().getInventory();
            List<ItemStack> contents = new ArrayList<>();
            for (ItemStack content : ctx.player().getInventory().getContents()) {
                contents.add(Optional.ofNullable(content).orElse(ItemStack.empty()));
            }
            Collections.shuffle(contents);

            inv.setContents(contents.toArray(new ItemStack[0]));
        });

        create("totem_of_undying", "%s gifted you a second life",
                ctx -> ctx.giveWithName(ctx.donator(), new ItemStack(Material.TOTEM_OF_UNDYING))
        );

        create("dropper", "%s built a dropper", ctx -> {
            Block block = ctx.player().getLocation().getBlock();
            for (int y = 0; y < 50; y++) {
                for (int x = -1; x <= 1; x++) {
                    block.getRelative(x, -y, -1).setType(Material.AIR);
                    block.getRelative(x, -y, 0).setType(Material.AIR);
                    block.getRelative(x, -y, 1).setType(Material.AIR);
                }
            }

            block.getRelative(0, -50, 0).setType(Material.WATER);
        });

        create("heal", "%s healed you", ctx -> ctx.player().setHealth(20));

        create("explosion", "%s exploded you!", ctx -> {
            TNTPrimed primed = ctx.summon(TNTPrimed.class);
            primed.getPersistentDataContainer().set(ALERT_EXPLOSION, PersistentDataType.BOOLEAN, true);
            primed.setFuseTicks(10);
        });

        create("mega_sword", "%s gifted you a mega-sword", ctx -> ctx.give(new ItemStack(Material.DIAMOND_SWORD){{
            editMeta(meta -> {
                meta.setItemModel(NamespacedKey.minecraft("doom_sword"));
                ((Damageable) meta).setMaxDamage(3);
                meta.getPersistentDataContainer().set(MEGA_SWORD, PersistentDataType.BOOLEAN, true);
                meta.displayName(Component.text("Мега-меч").decoration(TextDecoration.ITALIC, false));
                meta.setRarity(ItemRarity.EPIC);
                meta.setEnchantmentGlintOverride(true);
            });
        }})); // #3

        create("airdrop", "%s sent an airdrop", ctx -> {
            List<ItemStack> drops = new ArrayList<>();
            Random random = new Random();
            for (int i = 0; i < 5; i++) {

                ItemStack stack = createRandomItem(random);
                int halfAmount = (int) (stack.getAmount() / 2d);
                if(halfAmount > 2 && RandomUtil.nextBoolean(0.75)) {
                    int newAmount = RandomUtil.nextInt(stack.getAmount() - halfAmount);
                    if(newAmount > 1) {
                        stack.setAmount(stack.getAmount() - newAmount);
                        ItemStack second = stack.clone();
                        second.setAmount(newAmount);
                        drops.add(second);
                    }
                }

                drops.add(stack);
            }
            
            FallingBlock block = ctx.player().getWorld().spawn(ctx.player().getLocation().add(0, 5, 0).toCenterLocation(), FallingBlock.class);
            BlockData data = Material.BARREL.createBlockData();
            Barrel barrel = (Barrel) data.createBlockState();
            Inventory inventory = barrel.getInventory();

            int size = drops.size();
            int ranging = (int) (27d / size);
            for (int i = 0; i < size; i++) {
                int index = i * ranging;
                int mod = index - 1 + RandomUtil.nextInt(3);
                if(mod < 27 && mod >= 0 && inventory.getItem(mod) == null) {
                    index = mod;
                }
                inventory.setItem(index, drops.get(i));
            }

            block.setBlockData(data);
            block.setBlockState(barrel);
        }); // #4

        create("dropper2", "%s  built a dropper <green>V2</green>!", ctx -> {
            Player player = ctx.player();
            player.teleport(player.getLocation().add(0, 100, 0));

            PlayerInventory inventory = player.getInventory();
            ItemStack stack = inventory.getItemInMainHand();
            if(stack.isEmpty()) {
                inventory.setItemInMainHand(new ItemStack(Material.WATER_BUCKET));
                return;
            }

            int held = inventory.getHeldItemSlot();
            int emptySlot = -1;
            for (int i = 0; i < 36; i++) {
                if(held == i) continue;
                if(inventory.getItem(i) == null) {
                    emptySlot = i;
                    break;
                }
            }

            if(emptySlot != -1) inventory.setItem(emptySlot, stack);
            inventory.setItem(held, new ItemStack(Material.WATER_BUCKET));
        }); // #5

        create("ad", "%s became an advertiser", ctx -> {
            AdAlert alert = ADS.computeIfAbsent(ctx.player(), player -> new AdAlert(
                    player,
                    () -> ADS.remove(player)
            ));

            alert.trigger();
        });

        create("ink", "%s blurted out on the screen!", ctx -> ctx.player().showTitle(Title.title(
                Text.create("<color:black>典</color>"),
                Component.empty(),
                Title.Times.times(
                        Duration.ofMillis(50),
                        Duration.ofSeconds(10),
                        Duration.ofSeconds(1)
                )
        ))); // #8

        create("bad_trip", "%s had a bad trip...", ctx -> {
            FollowingEntity entity = new FollowingEntity(
                    ctx.player(),
                    10 * 20L,
                    "drug"
            );
            entity.create();
        }); // #9

        create("black_hole", "%s created a black hole", ctx -> {
            BlackHoleEntity entity = new BlackHoleEntity();
            entity.create(ctx.player().getLocation().add(0, 10, 0));
        }); // #10

        create("casino", "%s started a ... stream", ctx -> {
            new CasinoEntity(ctx.player().getLocation().getBlock());
        });

//        create("rainbow", "%s created a friendship rainbow", ctx -> {
//            FollowingEntity entity = new FollowingEntity(
//                    ctx.player(),
//                    10 * 20L,
//                    "wobble"
//            );
//            entity.create();
//        }); // #12

        create("to_nether", "%s sent streamer to the nether!", ctx -> {
            World world = Bukkit.getWorld("world_nether");
            if(world == null) return;
            Location spawn = world.getSpawnLocation();
            ctx.player().teleport(findSafeNetherSpawn(world, spawn.blockX(), spawn.blockZ()));
        });

        create("kill_chefs", "%s killed the bosses around", ctx -> {
            ctx.player().getWorld()
                    .getNearbyEntitiesByType(Giant.class, ctx.player().getLocation(), 300)
                    .forEach(Giant::remove);
        });

        create("spawn_chef", "%s spawned a boss", ctx -> {
            new BossEntity(ctx.player());
        });

        create("helpers", "%s called for helpers!", ctx -> {
            HelpersAlert alert = HELPERS.computeIfAbsent(ctx.player(), player -> new HelpersAlert(
                    player,
                    () -> HELPERS.remove(player)
            ));

            alert.start();
        });

        create("lucky_blocks", "%s gave a random lucky block",
                ctx -> ctx.give(new ItemStack(LUCKY_BLOCKS[RandomUtil.nextInt(3)]))
        ); // #14

        create("random_scale", "%s set a random scale",
                ctx -> ctx.player().getAttribute(Attribute.SCALE).setBaseValue(RandomUtil.nextDouble(0.3, 2.5))
        );

        return this;
    }


    private static ItemStack createRandomItem(Random random) {
        Material[] materials = Material.values();
        Material material;

        // we will choose new material until it something we like
        do {
            material = materials[random.nextInt(materials.length)];
        } while (material.isLegacy() || !material.isItem() || material.isAir() || BANNED_ITEMS.contains(material));

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        switch (meta) {
            case PotionMeta potion -> {
                PotionType[] types = PotionType.values();
                potion.setBasePotionType(types[random.nextInt(types.length)]);
            }
            case EnchantmentStorageMeta enchantmentStorage -> {
                Enchantment[] enchantments = Enchantment.values();
                Enchantment enchantment = enchantments[random.nextInt(enchantments.length)];
                enchantmentStorage.addStoredEnchant(
                        enchantment,
                        random.nextInt(enchantment.getMaxLevel() + 1),
                        true
                );
            }
            case MusicInstrumentMeta music -> music.setInstrument(RandomUtil.chooseRandomly(MusicInstrument.values()));
            default -> {}
        }

        stack.setItemMeta(meta);

        stack.setAmount(1 + random.nextInt(stack.getMaxStackSize()));

        return stack;
    }

    private static Location findSafeNetherSpawn(World world, int centerX, int centerZ) {
        int[] heightLevels = {120, 100, 80, 64, 32};

        for (int y : heightLevels) {
            Location safeLoc = findSafePositionAtHeight(world, centerX, centerZ, 32, y);
            if (safeLoc != null) {
                return safeLoc;
            }
        }

        return findSafePositionInRange(world, centerX, centerZ, 32, 32, 120);
    }

    private static Location findSafePositionAtHeight(World world, int centerX, int centerZ, int radius, int y) {
        for (int r = 0; r <= radius; r++) {
            for (int x = centerX - r; x <= centerX + r; x++) {
                for (int z = centerZ - r; z <= centerZ + r; z++) {
                    if (Math.abs(x - centerX) == r || Math.abs(z - centerZ) == r) {
                        Location candidate = new Location(world, x + 0.5, y, z + 0.5);
                        if (isSafeSpawnPosition(candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static Location findSafePositionInRange(World world, int centerX, int centerZ, int radius, int minY, int maxY) {
        for (int y = maxY; y >= minY; y--) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    Location candidate = new Location(world, x + 0.5, y, z + 0.5);
                    if (isSafeSpawnPosition(candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafeSpawnPosition(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        Block feetBlock = world.getBlockAt(x, y, z);
        Block headBlock = world.getBlockAt(x, y + 1, z);
        Block floorBlock = world.getBlockAt(x, y - 1, z);

        if (!isSafeMaterial(feetBlock.getType()) || !isSafeMaterial(headBlock.getType())) return false;
        if (!isSolidAndSafe(floorBlock.getType()) || hasHazardousBlocksNearby(world, x, y, z)) return false;

        return !isInLava(world, x, y, z);
    }

    private static boolean isSafeMaterial(Material material) {
        return !material.isSolid() && material != Material.LAVA && material != Material.FIRE;
    }

    private static boolean isSolidAndSafe(Material material) {
        return material.isSolid() &&
                material != Material.LAVA &&
                material != Material.FIRE &&
                material != Material.MAGMA_BLOCK &&
                material != Material.SOUL_FIRE &&
                !material.name().contains("BED") &&
                material != Material.CAMPFIRE;
    }

    private static boolean hasHazardousBlocksNearby(World world, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    Block block = world.getBlockAt(x + dx, y + dy, z + dz);
                    Material type = block.getType();

                    if (type == Material.LAVA ||
                            type == Material.FIRE ||
                            type == Material.SOUL_FIRE ||
                            type == Material.MAGMA_BLOCK) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isInLava(World world, int x, int y, int z) {
        Block current = world.getBlockAt(x, y, z);
        if (current.getType() == Material.LAVA) return true;

        for (int i = 1; i <= 2; i++) {
            if (world.getBlockAt(x, y + i, z).getType() == Material.LAVA) {
                return true;
            }
        }
        return false;
    }

}
