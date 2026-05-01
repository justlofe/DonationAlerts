package su.starlight.da.alert;

public record Alert(String key, String message, AlertExecutor executor) {

}
