package io.github.andrewwwwwwwwwwwwwww.craftle.game;

import java.util.List;

/**
 * The fixed 18-ingredient palette (matching the original web game). Order matters:
 * palette indices are what travels over the network and sits in saved guesses.
 */
public final class Palette {
    public static final List<String> ITEM_IDS = List.of(
            "minecraft:oak_planks",
            "minecraft:cobblestone",
            "minecraft:stone",
            "minecraft:glass",
            "minecraft:white_wool",
            "minecraft:stick",
            "minecraft:coal",
            "minecraft:diamond",
            "minecraft:gold_ingot",
            "minecraft:iron_ingot",
            "minecraft:redstone",
            "minecraft:quartz",
            "minecraft:oak_slab",
            "minecraft:oak_log",
            "minecraft:iron_nugget",
            "minecraft:redstone_torch",
            "minecraft:string",
            "minecraft:leather"
    );

    public static final int SIZE = ITEM_IDS.size();

    private Palette() {
    }

    public static int indexOf(String itemId) {
        return ITEM_IDS.indexOf(itemId);
    }
}
