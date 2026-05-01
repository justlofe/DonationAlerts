package su.starlight.da.alert;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import su.starlight.da.util.Text;

public record AlertContext(String donator, String donationMessage, Player player) {

    public void message(String message) {
        player.sendRichMessage(message);
    }

    public <E extends Entity> E summon(Class<E> entityClazz) {
        Location loc = player.getLocation();
        return loc.getWorld().spawn(loc, entityClazz);
    }

    public void give(ItemStack... items) {
        player.getInventory().addItem(items);
    }

    public void giveWithName(String name, ItemStack... items) {
        Component display = Text.create(name).decoration(TextDecoration.ITALIC, false);
        for (ItemStack item : items) {
            item.editMeta(meta -> meta.displayName(display));
        }
        player.getInventory().addItem(items);
    }

}
