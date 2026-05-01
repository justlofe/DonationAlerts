package su.starlight.da.command;

import com.mojang.datafixers.util.Pair;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.CommandExecutor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import su.starlight.da.CorePlugin;
import su.starlight.da.alert.AlertMonitor;
import su.starlight.da.connection.ConnectionService;
import su.starlight.da.util.MessageUtil;
import su.starlight.da.util.Text;

import java.util.Optional;

public final class DACommand extends CommandAPICommand {

    private final CorePlugin plugin;

    public DACommand(CorePlugin plugin) {
        super("da");
        withAliases("donationalerts");
        this.plugin = plugin;

        withSubcommands(
                new CommandAPICommand("monitoring").withPermission("*").executes(this::monitoring),

                new CommandAPICommand("call").withPermission("*").withArguments(
                        new StringArgument("type").replaceSuggestions(ArgumentSuggestions.stringCollection(_ -> plugin.alerts().getTypes()
                                .stream()
                                .toList()))
                ).withOptionalArguments(new GreedyStringArgument("donation_message")).executesPlayer(this::call),

                new CommandAPICommand("connect").executes(this::connect),
                new CommandAPICommand("reload").withPermission("*").executes(this::reload),

                new CommandAPICommand("helper").withPermission("*").withSubcommands(
                        new CommandAPICommand("add").withArguments(new StringArgument("player").replaceSuggestions(
                                ArgumentSuggestions.stringCollection(_ -> Bukkit.getOnlinePlayers()
                                        .parallelStream()
                                        .map(Player::getName)
                                        .filter(player -> !plugin.alerts().canBeHelper(player))
                                        .toList()
                                )
                        )).executes((CommandExecutor) (sender, args) -> changeHelper(sender, args, true)),

                        new CommandAPICommand("remove").withArguments(new StringArgument("player").replaceSuggestions(
                                ArgumentSuggestions.stringCollection(_ -> plugin.alerts().possibleHelpers())
                        )).executes((CommandExecutor) (sender, args) -> changeHelper(sender, args, false))
                )
        );
    }

    private void changeHelper(CommandSender sender, CommandArguments args, boolean add) {
        String name = args.getUnchecked("player");
        assert name != null;
        plugin.alerts().setCanBeHelper(name, add);
        sender.sendMessage(MessageUtil.INFO.create(
                name + " " + (add ? "can be helper now." : "can't be helper anymore")
        ));
    }

    private void reload(CommandSender sender, CommandArguments args) {
        plugin.reloadConfig();
        plugin.alerts().loadPrices();
        sender.sendMessage(MessageUtil.INFO.create("Reloaded prices for alerts. For reloading server info or connections, restart server."));
    }

    private void connect(CommandSender sender, CommandArguments args) {
        if(!(sender instanceof Player player)) {
            sender.sendMessage("only player");
            return;
        }

        ConnectionService service = plugin.connectionService();
        if(service.connected(player)) {
            sender.sendMessage(MessageUtil.ERROR.create("Connection is already established."));
            return;
        }

        if(service.tryCreateFromOffline(player, false)) return;

        sender.sendMessage(MessageUtil.INFO.create(String.format(
                "Connect your DonationAlerts account: <hover:show_text:'Open in browser'><click:open_url:'%s'><green>[Connect]</hover>",
                Optional.ofNullable(service.createLinkURL(player))
                        .orElse("")
        )));
    }

    private void call(Player sender, CommandArguments args) {
        String type = args.getUnchecked("type");
        String donationMessage = args.getOrDefaultUnchecked("donation_message", "");
        plugin.alerts().execute(sender, "@", donationMessage, type);
    }

    private void monitoring(CommandSender sender, CommandArguments args) {
        StringBuilder builder = new StringBuilder("Alerts monitoring:");
        for (Pair<String, AlertMonitor.Monitored> data : plugin.alerts().monitor().getAllData()) {
            builder.append("\n").append("> ").append(buildMonitoringData(data.getFirst(), data.getSecond()));
        }
        sender.sendMessage(Text.create(builder.toString()));
    }

    private static String buildMonitoringData(String key, AlertMonitor.Monitored monitored) {
        return String.format(
                "<yellow>%s</yellow>: avg(%.2f) | max(%.2f) | min(%.2f) | me(%.2f)",
                key,
                monitored.average(),
                monitored.maximum(),
                monitored.minimum(),
                monitored.median()
        );
    }

}
