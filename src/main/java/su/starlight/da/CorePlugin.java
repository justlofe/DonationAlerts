package su.starlight.da;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.structure.Structure;
import su.starlight.da.alert.Alerts;
import su.starlight.da.alert.AlertsListener;
import su.starlight.da.alert.luckyblock.LuckyBlockListener;
import su.starlight.da.command.DACommand;
import su.starlight.da.connection.ConnectionListener;
import su.starlight.da.connection.ConnectionService;
import su.starlight.da.resourcepack.ResourcePackListener;
import su.starlight.da.resourcepack.ResourcePackServer;
import su.starlight.da.util.FileUtil;
import su.starlight.da.util.task.TaskUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class CorePlugin extends JavaPlugin {

    private static CorePlugin instance;

    private final TaskUtil taskUtil = new TaskUtil(this);
    private final ConnectionService connectionService = new ConnectionService(this);
    private final Alerts alerts = Alerts.createDefault(this);
    private final ResourcePackServer packServer = new ResourcePackServer(this);
    private final AdvancementDisplay advancementDisplay = new AdvancementDisplay(this);

    private final Map<NamespacedKey, Structure> STRUCTURES = new HashMap<>();

    public CorePlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        connectionService.loadOfflineConnections();
        connectionService.start();

        loadStructures();
        alerts.loadPrices();

        packServer.start();

        registerListeners(
                this,
                new ConnectionListener(this),
                new AlertsListener(),
                new LuckyBlockListener(this),
                new ResourcePackListener(this)
        );

        new DACommand(this).register(this);
    }

    @Override
    public void onDisable() {
        connectionService.stop();
        packServer.stop();
    }

    private void loadStructures() {
        FileUtil.extractData("structures/", getDataFolder());

        STRUCTURES.clear();
        File structuresFolder = new File(getDataFolder(), "structures/");
        for (String id: new String[]{"yellow", "red", "green"}) {
            File luckyBlockFolder = new File(structuresFolder, id + "/");

            var files = luckyBlockFolder.listFiles();
            if(files == null) continue;

            for (File file : files) {
                var name = FileUtil.getNameAndExtension(file);
                if(!name.getSecond().equals("nbt")) continue;

                Structure structure;
                try {
                    structure = Bukkit.getStructureManager().loadStructure(file);
                }
                catch (Exception e) {
                    continue;
                }

                STRUCTURES.put(new NamespacedKey(id, name.getFirst()), structure);
            }
        }
    }

    private static void registerListeners(CorePlugin plugin, Listener... listeners) {
        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }
    }

    public TaskUtil taskUtil() {
        return taskUtil;
    }

    public ConnectionService connectionService() {
        return connectionService;
    }

    public Alerts alerts() {
        return alerts;
    }

    public ResourcePackServer packServer() {
        return packServer;
    }

    public AdvancementDisplay advancementDisplay() {
        return advancementDisplay;
    }

    public Set<Structure> getStructures(String id) {
        return STRUCTURES.entrySet().stream()
                .filter(entry -> entry.getKey().namespace().equals(id))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }

    public static CorePlugin instance() {
        return instance;
    }

}
