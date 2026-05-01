package su.starlight.da.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Macro to create items
 */
public final class ItemContainer {

    private final List<ItemStack> ITEMS = new ArrayList<>();

    public ItemContainer add(ItemStack... items) {
        for (ItemStack item : items) {
            add(item);
        }
        return this;
    }

    public ItemContainer add(Material... types) {
        for (Material type : types) {
            add(type);
        }
        return this;
    }

    public ItemContainer add(Material type, Consumer<ItemMeta> editor) {
        return add(type, 1, editor);
    }

    public ItemContainer add(Material type) {
        return add(type, 1);
    }

    public ItemContainer add(Material type, int count) {
        return add(type, count, (_) -> {});
    }

    public ItemContainer add(Material type, int count, Consumer<ItemMeta> editor) {
        return add(new ItemStack(type, count), editor);
    }

    public ItemContainer add(ItemStack stack) {
        return add(stack, (_) -> {});
    }

    public ItemContainer add(ItemStack stack, Consumer<ItemMeta> editor) {
        stack.editMeta(editor);
        ITEMS.add(stack);
        return this;
    }

    public List<ItemStack> items() {
        return ITEMS;
    }
}
