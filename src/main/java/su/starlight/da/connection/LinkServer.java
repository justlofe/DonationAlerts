package su.starlight.da.connection;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class LinkServer {

    private static final String PAGE = """
            <!DOCTYPE html>
            <html>
            <body>
            <div style="text-align: center;">
            <h2>%s</h2>
            </div>
            </body>
            </html>
            """;

    private static final String
            ERROR = "An error occurred.",
            SUCCESSFULLY_CONNECTED = "You've successfully connected account,<br>this tab can be closed.";

    private final ConnectionService service;
    private final HttpServer server;

    public LinkServer(ConnectionService service, InetSocketAddress address) {
        this.service = service;
        try {
            this.server = HttpServer.create(address, 0);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        server.createContext("/", this::handleConnection);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(1);
    }

    private void handleConnection(HttpExchange exchange) {
        String returnMessage;

        // determining response
        Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
        String code = query.get("code");
        if(code == null) returnMessage = ERROR;
        else {
            returnMessage = SUCCESSFULLY_CONNECTED;
            service.createConnectionFromCode(code);
        }

        try {
            byte[] responseBytes = String.format(PAGE, returnMessage).getBytes(StandardCharsets.UTF_8);

            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "text/html; charset=UTF-8");
            headers.set("Content-Length", String.valueOf(responseBytes.length));

            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> parseQuery(String query) {
        if(query == null || query.isEmpty()) return Map.of();
        if(query.startsWith("?")) query = query.replaceFirst("\\?", "");

        String[] pairs = query.split("&");
        Map<String, String> result = new HashMap<>();
        for (String rawPair : pairs) {
            String[] pair = rawPair.split("=", 2);
            if(pair.length != 2) continue;
            result.put(pair[0].toLowerCase(), pair[1]);
        }
        return Map.copyOf(result);
    }

}
