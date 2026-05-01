package su.starlight.da.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class MessageUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    public static final Type
            INFO = Type.INFO,
            ERROR = Type.ERROR;

    private MessageUtil() {
    }

    public static Component any(Type type, Component component) {
        return Component.empty()
                .colorIfAbsent(TextColor.color(0xFFFFFF))
                .append(type.create())
                .append(component);
    }

    public enum Type {

        INFO("info", 0x69A2FF),
        ERROR("error", 0xFF5555);

        public final String name;
        public final int color;

        Type(String name, int color) {
            this.name = name;
            this.color = color;
        }

        public Component create(String message) {
            return any(this, MM.deserialize(message));
        }

        public Component create() {
            return Component.text("[")
                    .append(Component.text(name).color(TextColor.color(this.color)))
                    .append(Component.text("] » "))
                    .color(TextColor.color(this.color));
        }

    }

}
