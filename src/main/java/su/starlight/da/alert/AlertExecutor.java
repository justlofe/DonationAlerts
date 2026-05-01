package su.starlight.da.alert;

@FunctionalInterface
public interface AlertExecutor {

    void execute(AlertContext context);

}
