package su.starlight.da.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class Text {

    static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private Text() {}

    public static Component create(String text) {
        return MINI_MESSAGE.deserialize(text);
    }

    public static Component format(String format, Object... args) {
        return create(String.format(format, args));
    }

    public static String serialize(Component text) {
        return MINI_MESSAGE.serialize(text);
    }

}
