package su.starlight.da.alert;

import net.minecraft.server.level.ServerLevel;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import su.starlight.da.alert.entity.CasinoEntity;

@SuppressWarnings("UnstableApiUsage")
public final class AlertsListener implements Listener {

    @EventHandler
    private void onPlayerInteraction(PlayerInteractAtEntityEvent event) {
        if(event.getRightClicked() instanceof Interaction ent) {
            CasinoEntity.callSpin(ent.getUniqueId(), event.getPlayer());
        }
    }

    @EventHandler
    private void onExplosionPrime(ExplosionPrimeEvent event) {
        if(event.getEntity().getPersistentDataContainer().has(Alerts.ALERT_EXPLOSION)) {
            event.setRadius(20);
        }
    }

    @EventHandler
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        if(damager instanceof Player player && player.getInventory().getItemInMainHand().getPersistentDataContainer().has(Alerts.MEGA_SWORD)) {
            event.setDamage(EntityDamageEvent.DamageModifier.BASE, 100_000);
        }

        if(damager instanceof TNTPrimed tnt && tnt.getPersistentDataContainer().has(Alerts.ALERT_EXPLOSION)) {
            CraftEntity entity = (CraftEntity) event.getEntity();
            entity.getHandle().kill((ServerLevel) entity.getHandle().level());
        }
    }

    @EventHandler
    private void onEntityDamage(EntityDamageEvent event) {
        if(event.getDamageSource().getDamageType() == DamageType.FALL && event.getEntity() instanceof Player player && player.getPersistentDataContainer().has(Alerts.CANCEL_FALL_DAMAGE)) {
            event.setCancelled(true);
            player.getPersistentDataContainer().remove(Alerts.CANCEL_FALL_DAMAGE);
        }
    }

}
