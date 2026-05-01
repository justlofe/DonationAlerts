package su.starlight.da.resourcepack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import su.starlight.da.CorePlugin;
import su.starlight.da.util.FileUtil;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.function.Supplier;

public final class ResourcePackServer {

    private final CorePlugin corePlugin;

    private HttpServer server = null;

    public ResourcePackServer(CorePlugin corePlugin) {
        this.corePlugin = corePlugin;
    }

    public void start() {
        File file = getPackFile();
        if(!file.exists()) {
            FileUtil.extractData("pack.zip", file.getParentFile());
        }

        try {
            server = HttpServer.create(new InetSocketAddress(pack().getInt("port", 25566)), 0);
            server.createContext("/pack", new ResourcePackHandler(this::getPackFile));
            server.setExecutor(null);
            server.start();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        if(server != null) {
            server.stop(1);
            server = null;
        }
    }

    public void sendPack(Player player) {
        player.setResourcePack(getPackURI().toString(), generateHash(), true);
    }

    public URI getPackURI() {
        return URI.create(String.format("http://%s/pack", pack().getString("public_ip")));
    }

    public String generateHash() {
        byte[] bytes = FileUtil.sha1(getPackFile());
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private File getPackFile() {
        return new File(corePlugin.getDataFolder(), "pack.zip");
    }

    private ConfigurationSection pack() {
        return corePlugin.getConfig().getConfigurationSection("server.pack");
    }

    private record ResourcePackHandler(Supplier<File> pack) implements HttpHandler {
        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        public void handle(HttpExchange t) throws IOException {
            File file = pack.get();
            byte[] byteArray = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(byteArray);
            fis.close();

            t.sendResponseHeaders(200, byteArray.length);
            OutputStream os = t.getResponseBody();
            os.write(byteArray, 0, byteArray.length);
            os.close();
        }
    }

}