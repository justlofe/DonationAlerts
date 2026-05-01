package su.starlight.da.util.task;

@FunctionalInterface
public interface RepeatingTask {

    void tick(TaskContext ctx);

}
