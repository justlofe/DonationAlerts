package su.starlight.da.alert;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import su.starlight.da.CorePlugin;
import su.starlight.da.util.Text;
import su.starlight.da.util.task.TaskContext;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;

public final class HelpersAlert {

    private static final int HELPERS_ACTIVE_TICKS = 2 * 60 * 20;

    public static final NamespacedKey HELPER_TAG = NamespacedKey.minecraft("helper");

    private static final Collection<PotionEffect> EFFECTS = List.of(
            new PotionEffect(PotionEffectType.HASTE, -1, 2, true, false, true),
            new PotionEffect(PotionEffectType.STRENGTH, -1, 2, true, false, true),
            new PotionEffect(PotionEffectType.SPEED, -1, 1, true, false, true),
            new PotionEffect(PotionEffectType.JUMP_BOOST, -1, 1, true, false, true),
            new PotionEffect(PotionEffectType.RESISTANCE, -1, 255, true, false, true),
            new PotionEffect(PotionEffectType.REGENERATION, -1, 255, true, false, true)
    );

    private static final ItemStack[] ARMOR = {
            new ItemStack(Material.IRON_BOOTS),
            new ItemStack(Material.IRON_LEGGINGS),
            new ItemStack(Material.IRON_CHESTPLATE),
            new ItemStack(Material.IRON_HELMET),
    };

    private static final ItemStack
            SWORD = new ItemStack(Material.NETHERITE_SWORD),
            PICKAXE = new ItemStack(Material.NETHERITE_PICKAXE){{
                addUnsafeEnchantment(Enchantment.EFFICIENCY, 255);
                addUnsafeEnchantment(Enchantment.UNBREAKING, 255);
                addUnsafeEnchantment(Enchantment.FORTUNE, 255);
            }},
            AXE = new ItemStack(Material.NETHERITE_AXE){{
                addUnsafeEnchantment(Enchantment.EFFICIENCY, 255);
                addUnsafeEnchantment(Enchantment.UNBREAKING, 255);
                addUnsafeEnchantment(Enchantment.FORTUNE, 255);
            }},
            SHOVEL = new ItemStack(Material.NETHERITE_SHOVEL){{
                addUnsafeEnchantment(Enchantment.EFFICIENCY, 255);
                addUnsafeEnchantment(Enchantment.UNBREAKING, 255);
                addUnsafeEnchantment(Enchantment.FORTUNE, 255);
            }};

    private final Player streamer;
    private final Runnable endCallback;

    private boolean active;
    private BossBar bossbar;
    private int lifetime;
    private List<Player> helpers;

    public HelpersAlert(Player streamer, Runnable endCallback) {
        this.streamer = streamer;
        this.endCallback = endCallback;
    }

    public void start() {
        if(active) {
            lifetime = 0;
            return;
        }

        active = true;
        bossbar = BossBar.bossBar(
                Component.text("Helpers - time left 03:00"),
                1.0f,
                BossBar.Color.WHITE,
                BossBar.Overlay.PROGRESS
        );

        streamer.showBossBar(bossbar);

        helpers = Bukkit.getOnlinePlayers().parallelStream()
                .filter(player -> player != streamer)
                .filter(player -> player.getGameMode() == GameMode.SPECTATOR)
                .filter(player -> CorePlugin.instance().alerts().canBeHelper(player.getName()))
                .limit(2)
                .map(player -> (Player) player)
                .toList();

        helpers.forEach(helper -> {
            helper.getPersistentDataContainer().set(HELPER_TAG, PersistentDataType.BOOLEAN, true);
            helper.setGameMode(GameMode.SURVIVAL);
            helper.setHealth(helper.getAttribute(Attribute.MAX_HEALTH).getBaseValue());
            helper.teleport(streamer);

            PlayerInventory inventory = helper.getInventory();
            inventory.clear();

            inventory.setItem(0, SWORD);
            inventory.setItem(1, PICKAXE);
            inventory.setItem(2, AXE);
            inventory.setItem(3, SHOVEL);

            inventory.setArmorContents(ARMOR);

            helper.addPotionEffects(EFFECTS);
            helper.setAllowFlight(true);
        });

        CorePlugin.instance().taskUtil().startRepeating(
                this::tick,
                0L,
                1L
        );
    }

    private void tick(TaskContext context) {
        if(lifetime >= HELPERS_ACTIVE_TICKS) {
            ItemStack[] boxes = new ItemStack[helpers.size()];
            int i = 0;
            for (Player helper : helpers) {
                boxes[i++] = createBoxWithItems(helper.getInventory());
                helper.getInventory().clear();
                helper.clearActivePotionEffects();
                helper.setGameMode(GameMode.SPECTATOR);
                helper.getPersistentDataContainer().set(HELPER_TAG, PersistentDataType.BOOLEAN, false);
            }

            streamer.getInventory().addItem(boxes);

            Bukkit.getOnlinePlayers().forEach(player -> player.hideBossBar(bossbar));

            context.cancel();
            active = false;
            endCallback.run();
            return;
        }

        float progress = 1 - (float) (lifetime) / (HELPERS_ACTIVE_TICKS);
        bossbar.progress(progress);

        SimpleDateFormat format = new SimpleDateFormat("mm:ss");
        bossbar.name(Text.create(String.format(
                "Helpers - time left %s",
                format.format((HELPERS_ACTIVE_TICKS - lifetime) * 50L)
        )));

        lifetime++;
    }

    private static ItemStack createBoxWithItems(PlayerInventory playerInventory) {
        ShulkerBox box = (ShulkerBox) Material.SHULKER_BOX.createBlockData().createBlockState();
        Inventory inventory = box.getInventory();

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, playerInventory.getItem(i + 9));
        }

        ItemStack stack = new ItemStack(Material.WHITE_SHULKER_BOX);
        stack.editMeta(meta -> {
            BlockStateMeta stateMeta = (BlockStateMeta) meta;
            stateMeta.setBlockState(box);
            HumanEntity humanEntity = playerInventory.getHolder();
            if(humanEntity != null) meta.displayName(humanEntity.name());
        });
        return stack;
    }

}
