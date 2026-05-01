package su.starlight.da.alert;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import su.starlight.da.CorePlugin;
import su.starlight.da.util.RandomUtil;
import su.starlight.da.util.Text;
import su.starlight.da.util.task.TaskContext;

import java.time.Duration;

public final class AdAlert {

    private static final int ADS_SIZE = 15;
    private static final int AD_LIFETIME = 10 * 20; // 10 seconds

    private final Player player;
    private final Runnable deleteCallback;
    private final boolean[] active;
    private final int[] lifetime;
    private int activeCount;

    private boolean dirty;
    private boolean ticking;

    public AdAlert(Player player, Runnable deleteCallback) {
        this.player = player;
        this.deleteCallback = deleteCallback;
        this.active = new boolean[ADS_SIZE];
        this.lifetime = new int[ADS_SIZE];
    }

    public void trigger() {
        int activeSize = activeCount;
        if(activeSize == ADS_SIZE) return;

        int[] candidates = new int[ADS_SIZE - activeSize];
        int index = 0;
        for (int i = 0; i < ADS_SIZE; i++) {
            if(!active[i]) candidates[index++] = i;
        }

        int newIndex = candidates[RandomUtil.nextInt(candidates.length)];
        lifetime[newIndex] = AD_LIFETIME;
        active[newIndex] = true;
        activeCount++;
        dirty = true;

        if(!ticking) startTicking();
    }

    private void startTicking() {
        ticking = true;
        CorePlugin.instance().taskUtil().startRepeating(this::tick, 0L, 1L);
    }

    private void tick(TaskContext context) {
        if(activeCount == 0) {
            context.cancel();
            ticking = false;
            dirty = false;
            if(deleteCallback != null) deleteCallback.run();
            player.clearTitle();
            return;
        }

        for (int i = 0; i < ADS_SIZE; i++) {
            if(lifetime[i] > 0 && --lifetime[i] <= 0) {
                active[i] = false;
                activeCount--;
                dirty = true;
            }
        }

        if(!dirty) return;
        dirty = false;

        StringBuilder builder = new StringBuilder();
        String baseSymbols = "1234567890qwert";

        boolean lastActive = false;
        boolean first = true;
        for (int i = 0; i < ADS_SIZE; i++) {
            boolean active = this.active[i];

            if((active != lastActive) || first) {
                lastActive = active;
                if(first) first = false;
                else builder.append("</font>");
                builder.append("<font:").append(active ? "donation" : "filler").append(">");
            }

            Color color = Color.fromRGB(4, active ? 4 : 8,(i + 1) * 4);
            builder.append("<color:")
                    .append(String.format(
                            "#%02x%02x%02x",
                            color.getRed(),
                            color.getGreen(),
                            color.getBlue()
                    ))
                    .append(">")
                    .append(baseSymbols.charAt(i))
                    .append("</color>");
        }

        builder.append("</font>");

        player.showTitle(Title.title(
                Text.create(builder.toString()),
                Component.empty(),
                Title.Times.times(
                        Duration.ZERO,
                        Duration.ofSeconds(10),
                        Duration.ZERO
                )
        ));

    }

}
