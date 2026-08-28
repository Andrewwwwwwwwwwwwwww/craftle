package io.github.andrewwwwwwwwwwwwwww.craftle;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/** String item id &lt;-&gt; Item lookups (registry ids are the stable currency of this mod). */
public final class ItemIds {
    private ItemIds() {
    }

    /** Resolves an item id; returns air for unknown/unparseable ids (DefaultedRegistry behavior). */
    public static Item resolve(String id) {
        Identifier parsed = Identifier.tryParse(id);
        if (parsed == null) {
            parsed = Identifier.fromNamespaceAndPath(Craftle.MOD_ID, "unknown");
        }
        return BuiltInRegistries.ITEM.getValue(parsed);
    }

    public static String idOf(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    /** True when {@code id} names a real registered item (round-trips through the registry). */
    public static boolean exists(String id) {
        Identifier parsed = Identifier.tryParse(id);
        return parsed != null && BuiltInRegistries.ITEM.containsKey(parsed);
    }
}
