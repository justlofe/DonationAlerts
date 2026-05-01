package su.starlight.da.connection;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import su.starlight.da.CorePlugin;

public final class ConnectionListener implements Listener {

    private final CorePlugin corePlugin;

    public ConnectionListener(CorePlugin corePlugin) {
        this.corePlugin = corePlugin;
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        corePlugin.connectionService().tryCreateFromOffline(event.getPlayer(), true);
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        corePlugin.connectionService().dropConnection(event.getPlayer());
    }

}
