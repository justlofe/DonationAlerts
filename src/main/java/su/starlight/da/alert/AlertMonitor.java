package su.starlight.da.alert;

import com.mojang.datafixers.util.Pair;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class AlertMonitor {

    private final Map<String, Monitored> DATA = new HashMap<>();

    private AlertMonitor() {}

    public List<Pair<String, Monitored>> getAllData() {
        return DATA.entrySet().stream().map(entry -> Pair.of(entry.getKey(), entry.getValue())).toList();
    }

    public void register(String key) {
        if(DATA.containsKey(key)) return;
        DATA.put(key, new Monitored(key));
    }

    public void addCompletionTime(String to, long completionTime) {
        Monitored data = DATA.get(to);
        if(data == null) return;
        data.add(completionTime);
    }

    public Optional<Monitored> getData(Key alertType) {
        return Optional.ofNullable(DATA.get(alertType));
    }

    public static AlertMonitor create() {
        return new AlertMonitor();
    }

    public static final class Monitored {

        private final String key;
        private final List<Long> completionTimes;

        private Monitored(String key) {
            this.key = key;
            this.completionTimes = new ArrayList<>();
        }

        private void add(long completionTime) {
            this.completionTimes.add(completionTime);
        }

        public double average() {
            int size = completionTimes.size();
            if(size == 0) return 0;
            long summary = 0;
            for (Long completionTime : completionTimes) {
                summary += completionTime;
            }

            return ((double) summary / size) * 0.001d;
        }

        public double maximum() {
            long maximum = 0;
            for (Long completionTime : completionTimes) {
                if(completionTime > maximum) maximum = completionTime;
            }
            return maximum * 0.001d;
        }

        public double minimum() {
            long maximum = 0;
            for (Long completionTime : completionTimes) {
                if(completionTime < maximum) maximum = completionTime;
            }
            return maximum * 0.001d;
        }

        public double median() {
            List<Long> completionTimes = new ArrayList<>(this.completionTimes);
            Collections.sort(completionTimes);

            int size = completionTimes.size();
            if(size <= 1) return 0;
            else if(size % 2 != 0) return completionTimes.get((int) ((size - 1) * 0.5) - 1) * 0.001d;

            int firstIndex = ((int) (size * 0.5)) - 1;
            return (completionTimes.get(firstIndex) + completionTimes.get(firstIndex + 1)) / 2d * 0.001d;
        }

        public @NotNull String key() {
            return key;
        }

    }

}
