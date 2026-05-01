package su.starlight.da.connection;

import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;
import su.starlight.da.CorePlugin;
import su.starlight.da.util.MessageUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class Connection {

    private static final URI WEBSOCKET_ENDPOINT = URI.create("wss://centrifugo.donationalerts.com/connection/websocket");
    private static final URI SUBSCRIBE = URI.create("https://www.donationalerts.com/api/v1/centrifuge/subscribe");
    private static final URI OAUTH = URI.create("https://www.donationalerts.com/api/v1/user/oauth");

    private final CorePlugin corePlugin;
    private final Logger logger;

    private final Player player;
    private final String id;
    private final String accessToken;

    private final AtomicInteger MESSAGE_ID = new AtomicInteger(1);
    private final Map<Integer, Consumer<JSONObject>> callback = new HashMap<>();

    private long connectionStart;
    private int userId;
    private String socketConnectionToken;
    private String connectionToken;
    private Client client;
    private String clientId;
    private boolean connected;

    public Connection(CorePlugin corePlugin, Logger logger, Player player, String id, String accessToken) {
        this.corePlugin = corePlugin;
        this.logger = Logger.getLogger(logger.getName() + "/" + id);

        this.player = player;
        this.id = id;
        this.accessToken = accessToken;
    }

    public boolean connected() {
        return connected;
    }

    public void connect() {
        if(connected) return;
        logger.info("Connecting to DA...");
        connectionStart = System.currentTimeMillis();
        try {
            HttpURLConnection connection = open(OAUTH);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);

            JSONObject response = readResponse(connection).getJSONObject("data");
            userId = response.getInt("id");
            socketConnectionToken = response.getString("socket_connection_token");

            connection.disconnect();
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        logger.info("Retrieved user data, connecting with WebSocket");

        client = new Client(WEBSOCKET_ENDPOINT, this::onMessage, () -> connected = false);
        try {
            client.connectBlocking();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        send(
                request -> request.put("params", new JSONObject().put("token", socketConnectionToken)),
                response -> {
                    this.clientId = response.getJSONObject("result").getString("client");
                    logger.info("WS successfully authorized, subscribing to donation channel");
                    subscribe();
                }
        );
    }

    private void subscribe() {
        try {
            HttpURLConnection connection = open(SUBSCRIBE);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setDoOutput(true);
            writeRequest(
                    connection,
                    new JSONObject()
                            .put(
                                    "channels",
                                    new JSONArray()
                                            .put("$alerts:donation_" + userId)
                            )
                            .put("client", clientId)
            );


            connectionToken = readResponse(connection).getJSONArray("channels").getJSONObject(0).getString("token");
            connection.disconnect();
            logger.info("Subscription approved, creating channel");
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        send(
                request -> request
                        .put(
                                "params",
                                new JSONObject()
                                        .put("channel", "$alerts:donation_" + userId)
                                        .put("token", connectionToken)
                        )
                        .put("method", 1),
                (ignored) -> {
                    logger.info("Channel created. Connection fully initiated and ready to work.");
                    connected = true;
                    player.sendMessage(MessageUtil.INFO.create(String.format(
                            "Подключение успешно установлено за <aqua>%.2f</aqua>с!",
                            (System.currentTimeMillis() - connectionStart) * 0.001f
                    )));
                    player.playSound(player, "entity.player.levelup", 1, 2);
                }
        );
    }

    public void onMessage(String message) {
        try {
            JSONObject object = new JSONObject(message);

            if(object.isNull("id")) {
                if(object.isNull("result")) return;

                JSONObject result = object.getJSONObject("result");
                if(result.isNull("data") || result.isNull("channel") || !result.getString("channel").startsWith("$alerts:donation_")) return;

                JSONObject data = result.getJSONObject("data");
                if(data.isNull("data")) return;

                data = data.getJSONObject("data");

                onDonation(data.isNull("username") ? "Anonymous" : data.getString("username"), data.isNull("message") ? "" : data.getString("message"), data.isNull("amount") ? 0 : data.getInt("amount"));
                return;
            }

            Consumer<JSONObject> callback = this.callback.remove(object.getInt("id"));
            if(callback != null) callback.accept(object);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if(!connected) return;
        connected = false;
        client.close();
    }

    private void send(Consumer<JSONObject> request, Consumer<JSONObject> response) {
        JSONObject object = new JSONObject();
        int id = MESSAGE_ID.getAndIncrement();

        object.put("id", id);
        request.accept(object);

        callback.put(id, response);
        client.send(object.toString());
    }

    private HttpURLConnection open(URI uri) throws Exception{
        return (HttpURLConnection) uri.toURL().openConnection();
    }

    private void writeRequest(HttpURLConnection connection, JSONObject request) throws Exception {
        connection.setRequestProperty("Content-Type", "application/json");

        OutputStream os = connection.getOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(os)) {
            writer.write(request.toString());
            writer.flush();
        }
    }

    public static JSONObject readResponse(HttpURLConnection connection) throws Exception {
        int responseCode = connection.getResponseCode();
        if(responseCode != 200) {
            throw new RuntimeException("An error occurred on connection initiation: " + responseCode);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = reader.readLine()) != null) {
            content.append(inputLine);
        }
        reader.close();

        return new JSONObject(content.toString());
    }

    private void onDonation(String username, String message, int summary) {
        corePlugin.taskUtil().run(() -> corePlugin.alerts().execute(player, username, message, summary), 0L);
    }

}
