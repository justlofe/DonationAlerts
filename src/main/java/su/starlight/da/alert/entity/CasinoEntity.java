package su.starlight.da.alert.entity;

import com.destroystokyo.paper.ParticleBuilder;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Transformation;
import su.starlight.da.CorePlugin;
import su.starlight.da.util.RandomUtil;
import su.starlight.da.util.Text;
import su.starlight.da.util.task.TaskContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CasinoEntity {

    private static final Map<UUID, CasinoEntity> instancesByHitboxUid = new HashMap<>();

    private static final int
            ROLL_DURATION = 5;

    private static final int
            MIN_ROLL = 3,
            MAX_ROLL = 16;

    private static final int
            SIX_INDEX = 1,
            LEMON_INDEX = 2,
            APPLE_INDEX = 3,
            BLUEBERRY_INDEX = 4;

    private static final double WIN_CHANCE = 0.4;
    private static final double JACKPOT_CHANCE = 0.05; // within wins

    private final Block base;
    private final ItemDisplay baseEntity;
    private final ItemDisplay[] slots;
    private final Interaction hitbox;
    private final int[] rolls, indices;

    private Player gambler;
    private boolean freeSpin = true;
    private int endOfLife = -1;
    private long lifespan;

    public CasinoEntity(Block base) {
        this.base = base;
        base.setType(Material.BARRIER);
        base.getRelative(0, 1, 0).setType(Material.BARRIER);

        Location loc = base.getLocation().toCenterLocation();
        World world = loc.getWorld();

        baseEntity = world.spawn(loc, ItemDisplay.class);
        baseEntity.setItemStack(new ItemStack(Material.PAPER){{
            editMeta(meta -> meta.setItemModel(NamespacedKey.minecraft("casino")));
        }});

        hitbox = world.spawn(loc.clone().subtract(0, 0.5, 0), Interaction.class);
        hitbox.setInteractionWidth(1.1f);
        hitbox.setInteractionHeight(2);
        instancesByHitboxUid.put(hitbox.getUniqueId(), this);

        world.playSound(
                loc,
                "block.anvil.place",
                0.5f,
                1f
        );

        loc.add(0, 0.66, -0.0625);

        slots = new ItemDisplay[3];
        for (int i = 0; i < slots.length; i++) {
            ItemDisplay slot = slots[i] = world.spawn(
                    loc.clone().add(-.25 + 0.25 * i, 0, 0),
                    ItemDisplay.class
            );
            slot.setItemStack(new ItemStack(Material.GLASS){{
                editMeta(meta -> meta.setItemModel(NamespacedKey.minecraft("casino_slot")));
            }});

            slot.setInterpolationDuration(ROLL_DURATION);
            slot.setInterpolationDelay(0);
        }

        this.rolls = new int[3];
        this.indices = new int[3];
    }

    public void spin(Player gambler) {
        if(!freeSpin || endOfLife > 0) return;
        this.gambler = gambler;
        freeSpin = false;

        boolean isWinner = RandomUtil.nextDouble() < WIN_CHANCE;

        if (isWinner) {
            if (RandomUtil.nextDouble() < JACKPOT_CHANCE)
                setAllIndices(SIX_INDEX);
            else {
                int winningIndex = pickRandomIndex();
                setAllIndices(winningIndex);
            }
        }
        else generateLosingCombination();

        for (int i = 0; i < rolls.length; i++) {
            int targetRemainder = indices[i] - 1;
            int baseRoll = MIN_ROLL + RandomUtil.nextInt(MAX_ROLL - MIN_ROLL);

            while (baseRoll % 4 != targetRemainder) {
                baseRoll++;
                if (baseRoll > MAX_ROLL) {
                    baseRoll = MIN_ROLL + (targetRemainder - MIN_ROLL % 4 + 4) % 4;
                }
            }

            rolls[i] = Math.min(baseRoll, MAX_ROLL);
        }

        CorePlugin.instance().taskUtil().startRepeating(this::tick, 0L, 1L);
    }

    private void setAllIndices(int index) {
        Arrays.fill(indices, index);
    }

    private int pickRandomIndex() {
        int[] validIndices = {LEMON_INDEX, APPLE_INDEX, BLUEBERRY_INDEX};
        return validIndices[RandomUtil.nextInt(validIndices.length)];
    }

    private void generateLosingCombination() {
        int first, second, third;
        do {
            first = 1 + RandomUtil.nextInt(4);
            second = 1 + RandomUtil.nextInt(4);
            third = 1 + RandomUtil.nextInt(4);
        } while (first == second && second == third);

        indices[0] = first;
        indices[1] = second;
        indices[2] = third;
    }

    private void tick(TaskContext context) {
        if(endOfLife == 0) {

            int first = indices[0];
            boolean same = true;
            for (int i = 1; i < indices.length; i++) {
                if(indices[i] != first) {
                    same = false;
                    break;
                }
            }

            String message;
            if(!same) message = "Better luck next time!";
            else {
                if(first == SIX_INDEX) {
                    gambler.getInventory().clear();
                    message = "Today is not your day!";
                }
                else {
                    PlayerInventory inventory = gambler.getInventory();
                    ItemStack[] contents = inventory.getContents();
                    for (ItemStack stack : contents) {
                        if (stack == null) continue;
                        stack.setAmount(Math.min(stack.getMaxStackSize(), stack.getAmount() * 3));
                    }
                    inventory.setContents(contents);

                    message = "You're in luck today! 3x for each item!";
                }
            }

            gambler.sendMessage(Text.create("[Slots] " + message));

            CorePlugin.instance().taskUtil().run(() -> {
                base.setType(Material.AIR);
                base.getRelative(0, 1, 0).setType(Material.AIR);

                baseEntity.getWorld().playSound(
                        baseEntity.getLocation(),
                        "entity.generic.explode",
                        1,
                        1
                );
                new ParticleBuilder(Particle.EXPLOSION_EMITTER)
                        .location(hitbox.getLocation())
                        .count(2)
                        .extra(2)
                        .offset(1, 1, 1)
                        .allPlayers()
                        .spawn();

                baseEntity.remove();
                for (ItemDisplay slot : slots) {
                    slot.remove();
                }
                hitbox.remove();
            }, 100L);
            context.cancel();
            return;
        }

        if(endOfLife > 0) --endOfLife;

        else if(lifespan % ROLL_DURATION == 0) {
            boolean anyChanged = false;
            for (int i = 0; i < rolls.length; i++) {
                int remaining = rolls[i];
                if(remaining <= 0) continue;

                anyChanged = true;
                --rolls[i];

                if(rolls[i] == 0) CorePlugin.instance().taskUtil().run(() -> baseEntity.getWorld().playSound(
                        baseEntity.getLocation(),
                        "block.note_block.bell",
                        1,
                        1
                ), ROLL_DURATION);

                baseEntity.getWorld().playSound(
                        baseEntity.getLocation(),
                        "block.comparator.click",
                        1,
                        2
                );

                ItemDisplay slot = slots[i];
                Transformation transformation = slot.getTransformation();

                slot.setTransformation(new Transformation(
                        transformation.getTranslation(),
                        transformation.getLeftRotation().rotateX((float) Math.toRadians(90)),
                        transformation.getScale(),
                        transformation.getRightRotation()
                ));
                slot.setInterpolationDuration(ROLL_DURATION);
                slot.setInterpolationDelay(0);
            }

            if(anyChanged) {
                boolean cancel = false;
                for (int roll : rolls) {
                    if(roll > 0) {
                        cancel = true;
                        break;
                    }
                }

                if(!cancel) endOfLife = ROLL_DURATION;
            }
        }

        ++lifespan;
    }

    public static void callSpin(UUID interaction, Player player) {
        CasinoEntity casinoEntity = instancesByHitboxUid.get(interaction);
        if(casinoEntity != null) {
            player.swingMainHand();
            casinoEntity.spin(player);
            instancesByHitboxUid.remove(interaction);
        }
    }

}
