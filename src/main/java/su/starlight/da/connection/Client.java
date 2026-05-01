package su.starlight.da.connection;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.function.Consumer;

final class Client extends WebSocketClient {

    private final Consumer<String> onMessage;
    private final Runnable onDisconnect;

    public Client(URI serverUri, Consumer<String> onMessage, Runnable onDisconnect) {
        super(serverUri);
        this.onMessage = onMessage;
        this.onDisconnect = onDisconnect;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
    }

    @Override
    public void onMessage(String message) {
        this.onMessage.accept(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        onDisconnect.run();
        System.out.println("Disconnected. IsRemote: " + remote);
    }

    @Override
    public void onError(Exception exception) {
        exception.printStackTrace();
    }

}
