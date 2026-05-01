package su.starlight.da.resourcepack;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import su.starlight.da.CorePlugin;
import su.starlight.da.util.Text;

public final class ResourcePackListener implements Listener {

    private final CorePlugin corePlugin;

    public ResourcePackListener(CorePlugin corePlugin) {
        this.corePlugin = corePlugin;
    }

    @EventHandler public void onResourcePack(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();

        switch (event.getStatus()) {
            case DECLINED, DISCARDED -> player.kick(Text.create("Allow resourcepack installation to play on the server."));
            case FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD  -> player.kick(Text.create("An error occurred while installing the resourcepack."));
            default -> {}
        }
    }

    @EventHandler public void onPlayerJoin(PlayerJoinEvent event) {
        corePlugin.packServer().sendPack(event.getPlayer());
    }

}
