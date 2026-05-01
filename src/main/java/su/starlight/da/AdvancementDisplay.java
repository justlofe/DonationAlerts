package su.starlight.da;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.util.HashMap;

public final class AdvancementDisplay {

    private final CorePlugin corePlugin;

    public AdvancementDisplay(CorePlugin corePlugin) {
        this.corePlugin = corePlugin;
    }

    public void show(Player player, String id, Component title) {
        Advancement advancement;
        try {
            JSONComponentSerializer serializer = JSONComponentSerializer.json();
            advancement = Bukkit.getUnsafe().loadAdvancement(
                    NamespacedKey.fromString(id),
                    asJson(new JSONObject(serializer.serialize(title)))
            );
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
            return;
        }

        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        if (!progress.isDone())	{
            for (String criteria : progress.getRemainingCriteria())	{
                progress.awardCriteria(criteria);
            }
        }

        corePlugin.taskUtil().run(() -> {
            AdvancementProgress progress0 = player.getAdvancementProgress(advancement);
            if (progress0.isDone())	{
                for (String criteria : progress0.getAwardedCriteria()) {
                    progress0.revokeCriteria(criteria);
                }
            }

            ServerAdvancementManager advancementManager = MinecraftServer.getServer().getAdvancements();
            var map = new HashMap<>(advancementManager.advancements);
            map.remove(CraftNamespacedKey.toMinecraft(advancement.getKey()));
            advancementManager.advancements = map;

            ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
            PlayerAdvancements advancements = serverPlayer.getAdvancements();
            advancements.reload(MinecraftServer.getServer().getAdvancements());
            advancements.flushDirty(serverPlayer);

            Bukkit.getUnsafe().removeAdvancement(advancement.getKey());
        }, 20L);
    }

    public static String asJson(JSONObject title) {
        return new JSONObject()
                .put(
                        "display",
                        new JSONObject()
                                .put(
                                        "icon",
                                        new JSONObject()
                                                .put("id", Material.DIRT.getKey().asString())
                                )
                                .put("title", title)
                                .put("description", "1")
                                .put("background", "minecraft:textures/gui/advancements/backgrounds/adventure.png")
                                .put("frame", "task")
                                .put("show_toast", true)
                                .put("announce_to_chat", false)
                                .put("hidden", false)
                )
                .put(
                        "criteria",
                        new JSONObject()
                                .put(
                                        "impossible",
                                        new JSONObject()
                                                .put("trigger", "minecraft:impossible")
                                )
                ).toString();
    }

}