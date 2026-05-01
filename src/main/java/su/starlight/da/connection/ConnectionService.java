package su.starlight.da.connection;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.json.JSONObject;
import su.starlight.da.CorePlugin;

import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ConnectionService {

    private static final URI OAUTH_TOKEN = URI.create("https://www.donationalerts.com/oauth/token");

    private final CorePlugin corePlugin;
    private final Map<String, Connection> connections;
    private final Map<String, OfflineConnection> offlineConnections;

    private final LinkServer linkServer;

    private Player requestedLinkURL;

    public ConnectionService(CorePlugin corePlugin) {
        this.corePlugin = corePlugin;
        this.connections = new HashMap<>();
        this.offlineConnections = new HashMap<>();

        this.linkServer = new LinkServer(this, new InetSocketAddress(corePlugin.getConfig().getInt("server.app.port", 3000)));
    }

    public void start() {
        linkServer.start();
    }

    public void stop() {
        linkServer.stop();
    }

    public boolean connected(Player player) {
        return Optional.ofNullable(connections.get(player.getName().toLowerCase()))
                .map(Connection::connected)
                .orElse(false);
    }

    public void loadOfflineConnections() {
        ConfigurationSection connections = corePlugin.getConfig().getConfigurationSection("connection");
        if(connections == null) return;
        for (String name : connections.getKeys(false)) {
            ConfigurationSection connection = connections.getConfigurationSection(name);
            if(connection == null) continue;

            offlineConnections.put(
                    name.toLowerCase(),
                    new OfflineConnection(
                            connection.getString("access_token", ""),
                            connection.getBoolean("auto_connect", true)
                    )
            );
        }
    }

    public boolean tryCreateFromOffline(Player player, boolean auto) {
        String id = player.getName().toLowerCase();
        if(connections.containsKey(id)) return false;

        OfflineConnection offlineConnection = offlineConnections.get(id);
        if(offlineConnection == null || !(offlineConnection.autoConnect() && auto)) return false;

        createConnection(player, offlineConnection.accessToken(), false);
        return true;
    }

    public String createLinkURL(Player player) {
        requestedLinkURL = player;
        ConfigurationSection app = corePlugin.getConfig().getConfigurationSection("server.app");
        if(app == null) return null;
        return String.format(
                "https://www.donationalerts.com/oauth/authorize?redirect_uri=%s&client_id=%s&response_type=code&scope=%s",
                app.getString("redirect_uri"),
                app.getString("client_id"),
                URLEncoder.encode("oauth-user-show oauth-donation-subscribe", StandardCharsets.UTF_8)
        );
    }

    public void createConnectionFromCode(String code) {
        if(requestedLinkURL == null) {
            corePlugin.getLogger().severe("Got a request on web page with code, but we are not waiting for any connection.");
            return;
        }

        Player player = requestedLinkURL;
        requestedLinkURL = null;

        try {
            HttpURLConnection connection = (HttpURLConnection) OAUTH_TOKEN.toURL().openConnection();
            ConfigurationSection cfg = corePlugin.getConfig();
            byte[] content = buildQuery(Map.of(
                    "grant_type", "authorization_code",
                    "code", code,
                    "client_secret", cfg.getString("server.app.client_secret", ""),
                    "client_id", cfg.getString("server.app.client_id", ""),
                    "redirect_uri", cfg.getString("server.app.redirect_uri", "")
            )).getBytes(StandardCharsets.UTF_8);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", String.valueOf(content.length));
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()){
                os.write(content);
                os.flush();
            }

            JSONObject object = Connection.readResponse(connection);
            createConnection(player, object.getString("access_token"), true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String buildQuery(Map<String, String> query) {
        if(query.isEmpty()) return "";

        StringBuilder builder = new StringBuilder();

        var iterator = query.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();

            builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            if(iterator.hasNext()) builder.append("&");
        }

        return builder.toString();
    }

    public boolean createConnection(Player player, String accessToken, boolean saveToConfig) {
        String id = player.getName().toLowerCase();
        if(connections.containsKey(id)) return false;

        if(saveToConfig) saveAsOffline(id, accessToken, true);

        Connection connection = new Connection(corePlugin, corePlugin.getLogger(), player, id, accessToken);
        connections.put(id, connection);

        corePlugin.taskUtil().runAsync(connection::connect, 0L);

        return true;
    }

    public void dropConnection(Player player) {
        Connection connection = connections.remove(player.getName().toLowerCase());
        if(connection == null) return;
        connection.disconnect();
    }

    private void saveAsOffline(String id, String accessToken, boolean autoConnect) {
        offlineConnections.put(
                id,
                new OfflineConnection(accessToken, autoConnect)
        );

        FileConfiguration cfg = corePlugin.getConfig();
        cfg.set(
                String.format("connection.%s.access_token", id),
                accessToken
        );
        cfg.set(
                String.format("connection.%s.auto_connect", id),
                autoConnect
        );
        corePlugin.saveConfig();
    }

    private record OfflineConnection(String accessToken, boolean autoConnect) {

    }

}
