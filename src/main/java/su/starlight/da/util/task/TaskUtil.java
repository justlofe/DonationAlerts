package su.starlight.da.util.task;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.atomic.AtomicReference;

public final class TaskUtil {

    private final JavaPlugin plugin;

    public TaskUtil(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void run(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }

    public void runAsync(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    public void startRepeating(RepeatingTask task, long delay, long period) {
        AtomicReference<BukkitTask> reference = new AtomicReference<>();
        StaticContext context = new StaticContext(() -> reference.get().cancel());
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> task.tick(context), delay, period);
        reference.set(bukkitTask);
    }

    private record StaticContext(Runnable onCancel) implements TaskContext {
        @Override
        public void cancel() {
            onCancel.run();
        }
    }

}
