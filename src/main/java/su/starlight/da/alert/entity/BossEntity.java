package su.starlight.da.alert.entity;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import su.starlight.da.CorePlugin;
import su.starlight.da.util.task.TaskContext;
import su.starlight.da.util.task.TaskUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class BossEntity {

    public static final NamespacedKey BOSS_FIREBALL = NamespacedKey.minecraft("boss_fireball");

    private final static double BASE_DAMAGE = 5;

    private final static int TARGET_RANGE = 48;
    private final static int TICKS_BETWEEN_ATTACKS = 20;
    private final static int TICKS_BETWEEN_SHOTS =  40;
    private final static int TICKS_BEFORE_FORGETTING_FAR_TARGET = 100;

    private final Giant entity;
    private final TextDisplay nameDisplay;

    private int timesOnOnePlace = 0;
    private Player target = null;

    private int ticksTargetTooFarAway;
    private int ticksAlive;

    private boolean removed = false;

    private float yaw, pitch;
    private Location position;

    public BossEntity(Player target) {
        Location location = target.getLocation();

        Vector direction = location.getDirection();
        direction.setY(0);
        direction.normalize();

        entity = location.getWorld().spawn(
                location.clone().add(direction.clone().multiply(5)).setDirection(direction.multiply(-1)),
                Giant.class
        );
        entity.setRemoveWhenFarAway(false);
        entity.getEquipment().setItem(
                EquipmentSlot.HEAD,
                new ItemStack(Material.IRON_SWORD)
        );
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.FIRE_RESISTANCE,
                -1,
                255,
                false,
                false,
                false
        ));
        entity.setVisualFire(false);
//        entity.setAI(false);
        AttributeInstance instance = entity.getAttribute(Attribute.SAFE_FALL_DISTANCE);
        instance.setBaseValue(instance.getBaseValue() * 4);

        nameDisplay = location.getWorld().spawn(entity.getLocation(), TextDisplay.class);
        nameDisplay.text(Component.text("Boss"));
        nameDisplay.setTransformation(new Transformation(
                new Vector3f(0, 2, 0),
                new Quaternionf(),
                new Vector3f(5),
                new Quaternionf()
        ));
        nameDisplay.setBillboard(Display.Billboard.VERTICAL);

        entity.addPassenger(nameDisplay);

        position = entity.getLocation();

        TaskUtil taskUtil = CorePlugin.instance().taskUtil();
        taskUtil.startRepeating(this::baseTick, 1L, 2L);
        taskUtil.startRepeating(this::tick, 1L, 1L);
    }

    public boolean isRemoved() {
        return removed || entity.isDead();
    }

    private Optional<Player> findNewTarget() {
        World world = entity.getWorld();
        List<Player> candidates = new ArrayList<>(world.getNearbyEntitiesByType(
                Player.class,
                entity.getLocation(),
                TARGET_RANGE
        ));

        Location ourEyes = entity.getEyeLocation();
        candidates.removeIf(candidate -> {
            if(!canBeTarget(candidate)) return true;

            Location body = candidate.getLocation();
            Location eyes = candidate.getEyeLocation();

            double distance = ourEyes.distance(eyes) + 1;

            double yDiff = eyes.getY() - body.getY();
            body.setY(body.getY() + yDiff * 0.5);

            // we'll do two ray traces - between eyes and eyes & between eyes and player body
            RayTraceResult eyesToEyes = world.rayTraceBlocks(
                    ourEyes,
                    vecBetweenTwoLocations(ourEyes, eyes),
                    distance,
                    FluidCollisionMode.NEVER,
                    true
            );

            if(eyesToEyes == null) return false;

            return world.rayTraceBlocks(
                    ourEyes,
                    vecBetweenTwoLocations(ourEyes, body),
                    distance,
                    FluidCollisionMode.NEVER,
                    true
            ) != null;
        });

        candidates.sort(Comparator.comparingDouble(candidate -> candidate.getLocation().distanceSquared(ourEyes)));

        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.getFirst());
    }

    private static Vector vecBetweenTwoLocations(Location first, Location second) {
        return second.toVector().subtract(first.toVector());
    }

    private boolean canBeTarget(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE && player.hasPotionEffect(PotionEffectType.INVISIBILITY);
    }

    private void tick(TaskContext context) {
        if(isRemoved()) {
            context.cancel();
            return;
        }

        entity.setRotation(yaw, pitch);
        entity.setFireTicks(0);
    }

    private void baseTick(TaskContext context) {
        if(isRemoved() || !position.getChunk().isLoaded()) {
            context.cancel();
            if(!removed) removed = true;
            if(!entity.isDead()) entity.remove();
            if(!nameDisplay.isDead()) nameDisplay.remove();
            ticksAlive = 0;
            return;
        }

        ticksAlive += 2;

        Location eyes = entity.getEyeLocation();
        Player target = this.target;
        if(target == null || !canBeTarget(target)) {
            Optional<Player> newTarget = findNewTarget();
            if(newTarget.isPresent()) target = newTarget.get();
            else {
                boolean inWater = eyes.clone().add(0, 2, 0).getBlock().getType() == Material.WATER;
                entity.setVelocity(entity.getLocation().getDirection().normalize().setY(
                        inWater ? 3 : -4.5
                ));
                return;
            }

            this.target = target;
        }

        Location eyesDiff = target.getEyeLocation().subtract(eyes);
        double distanceXZ = Math.sqrt(eyesDiff.x() * eyesDiff.x() + eyesDiff.z() * eyesDiff.z());
        double distance = Math.sqrt(distanceXZ * distanceXZ + eyesDiff.y() * eyesDiff.y());

        yaw = (float) Math.toDegrees(Math.atan2(eyesDiff.z(), eyesDiff.x())) - 90;
        pitch = (float) Math.toDegrees(Math.acos(eyesDiff.y() / distance)) - 90;
        position = entity.getLocation();

        entity.setRotation(yaw, pitch);

        Location entityLoc = entity.getLocation();
        Location entityLocWithoutY = entityLoc.clone();
        entityLocWithoutY.setY(0);

        Location targetLoc = target.getLocation();
        Location targetLocWithoutY = targetLoc.clone();
        targetLocWithoutY.setY(0);

        double entityY = entityLoc.getY();
        double targetY = targetLoc.getY();
        double horizontalDistance = entityLocWithoutY.distance(targetLocWithoutY);

        if(horizontalDistance <= 3 && (targetY - entityY <= 20 || entityY >= targetY)) {
            if(ticksAlive % TICKS_BETWEEN_ATTACKS == 0) {
                target.damage(
                        BASE_DAMAGE,
                        DamageSource.builder(DamageType.MOB_ATTACK)
                                .withCausingEntity(entity)
                                .withDirectEntity(entity)
                                .build()
                );
                entity.swingMainHand();
            }

            Vector velocity = entityLoc.getDirection().normalize().multiply(0.3).setY(-0.5);
            entity.setVelocity(velocity);
            return;
        }
        else if(horizontalDistance >= TARGET_RANGE) {
            if(ticksTargetTooFarAway % TICKS_BEFORE_FORGETTING_FAR_TARGET == 0) {
                this.target = null;
                return;
            }

            ticksTargetTooFarAway += 2;
        }
        else if (ticksAlive % TICKS_BETWEEN_SHOTS == 0 && horizontalDistance >= 5) longRangeAttack();

        double oldX = entityLoc.x();
        double oldZ = entityLoc.z();

        if(eyes.add(0, 1, 0).getBlock().getType() == Material.WATER) {
            entity.setVelocity(entityLoc.getDirection().normalize().setY(0.28));
            return;
        }

        entity.setRemainingAir(entity.getRemainingAir());

        Vector movement = entityLoc.getDirection().normalize().multiply(entity.getLocation().getBlock().getType() == Material.WATER ? 0.1 : 0.2);
        if (horizontalDistance < 5) movement.setY(Math.min(movement.getY(), -0.2));
        else movement.setY(-0.9);

        entity.setVelocity(movement);

        CorePlugin.instance().taskUtil().run(() -> {
            if(isRemoved()) return;

            Location currentLocation = entity.getLocation();
            if(Math.abs(currentLocation.x() - oldX) >= 1 || Math.abs(currentLocation.z() - oldZ) >= 1 || (entityY >= targetY && entityLocWithoutY.distance(targetLocWithoutY) <= 2.5)) {
                timesOnOnePlace = 0;
                return;
            }

            timesOnOnePlace++;
            Vector velocity = entityLoc.getDirection().normalize();

            switch (timesOnOnePlace) {
                case 3 -> {
                    double jumpHeight = horizontalDistance < 5 ? 2 : 4;
                    velocity.setY(jumpHeight);
                }
                case 6, 13 -> {
                    velocity.rotateAroundY(0.9).multiply(0.5).setY(1);
                    if(timesOnOnePlace == 13) timesOnOnePlace = 0;
                }
                case 9, 10 -> velocity.rotateAroundY(-0.9).setY(1);
                default -> {
                    if(timesOnOnePlace <= 2) velocity.multiply(0.5).setY(0.2);
                }
            }

            entity.setVelocity(velocity);
        }, 1L);
    }

    private void longRangeAttack() {
        Location eyes = entity.getEyeLocation();
        Fireball fireball = eyes.getWorld().spawn(
                eyes.add(eyes.getDirection().normalize().multiply(2))
                        .subtract(0, 2, 0),
                Fireball.class
        );
        fireball.setDirection(vecBetweenTwoLocations(fireball.getLocation(), target.getLocation()).multiply(2));
        fireball.getPersistentDataContainer().set(BOSS_FIREBALL, PersistentDataType.BOOLEAN, true);
    }

    public void remove() {
        removed = true;
    }

}