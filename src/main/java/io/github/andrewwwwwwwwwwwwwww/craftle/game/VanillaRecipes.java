package io.github.andrewwwwwwwwwwwwwww.craftle.game;

import java.util.List;

/**
 * The puzzle pool: every vanilla shaped crafting recipe that can be built entirely from
 * {@link Palette}, in a fixed order.
 *
 * <p>The list is baked in rather than discovered from whatever recipes a server happens to
 * have loaded. That is what keeps the daily global: a datapack or mod adding, removing or
 * overriding a recipe cannot shift the pool and hand that server a different puzzle from
 * everyone else. The server is still asked for each recipe's actual layout, so the answer
 * always matches what really crafts there.</p>
 *
 * <p>Generated from the Minecraft 26.2 recipe data; regenerate when the game version changes.</p>
 */
public final class VanillaRecipes {
    private VanillaRecipes() {
    }

    public static final List<String> IDS = List.of(
            "minecraft:activator_rail", "minecraft:barrel", "minecraft:bow",
            "minecraft:bowl", "minecraft:bucket", "minecraft:bundle",
            "minecraft:campfire", "minecraft:cauldron", "minecraft:chest",
            "minecraft:chiseled_bookshelf", "minecraft:clock", "minecraft:coal_block",
            "minecraft:cobblestone_slab", "minecraft:cobblestone_stairs", "minecraft:cobblestone_wall",
            "minecraft:comparator", "minecraft:compass", "minecraft:composter",
            "minecraft:crafting_table", "minecraft:daylight_detector", "minecraft:diamond_axe",
            "minecraft:diamond_block", "minecraft:diamond_boots", "minecraft:diamond_chestplate",
            "minecraft:diamond_helmet", "minecraft:diamond_hoe", "minecraft:diamond_leggings",
            "minecraft:diamond_pickaxe", "minecraft:diamond_shovel", "minecraft:diamond_spear",
            "minecraft:diamond_sword", "minecraft:diorite", "minecraft:dropper",
            "minecraft:fishing_rod", "minecraft:furnace", "minecraft:glass_bottle",
            "minecraft:glass_pane", "minecraft:gold_block", "minecraft:golden_axe",
            "minecraft:golden_boots", "minecraft:golden_chestplate", "minecraft:golden_helmet",
            "minecraft:golden_hoe", "minecraft:golden_leggings", "minecraft:golden_pickaxe",
            "minecraft:golden_shovel", "minecraft:golden_spear", "minecraft:golden_sword",
            "minecraft:heavy_weighted_pressure_plate", "minecraft:iron_axe", "minecraft:iron_bars",
            "minecraft:iron_block", "minecraft:iron_boots", "minecraft:iron_chain",
            "minecraft:iron_chestplate", "minecraft:iron_door", "minecraft:iron_helmet",
            "minecraft:iron_hoe", "minecraft:iron_ingot_from_nuggets", "minecraft:iron_leggings",
            "minecraft:iron_pickaxe", "minecraft:iron_shovel", "minecraft:iron_spear",
            "minecraft:iron_sword", "minecraft:iron_trapdoor", "minecraft:item_frame",
            "minecraft:jukebox", "minecraft:ladder", "minecraft:lead",
            "minecraft:leather_boots", "minecraft:leather_chestplate", "minecraft:leather_helmet",
            "minecraft:leather_horse_armor", "minecraft:leather_leggings", "minecraft:lever",
            "minecraft:light_weighted_pressure_plate", "minecraft:loom", "minecraft:minecart",
            "minecraft:note_block", "minecraft:oak_boat", "minecraft:oak_door",
            "minecraft:oak_fence", "minecraft:oak_fence_gate", "minecraft:oak_pressure_plate",
            "minecraft:oak_sign", "minecraft:oak_slab", "minecraft:oak_stairs",
            "minecraft:oak_trapdoor", "minecraft:oak_wood", "minecraft:observer",
            "minecraft:painting", "minecraft:piston", "minecraft:powered_rail",
            "minecraft:quartz_block", "minecraft:rail", "minecraft:redstone_block",
            "minecraft:redstone_torch", "minecraft:repeater", "minecraft:saddle",
            "minecraft:shears", "minecraft:shield", "minecraft:smithing_table",
            "minecraft:stick", "minecraft:stone_axe", "minecraft:stone_bricks",
            "minecraft:stone_hoe", "minecraft:stone_pickaxe", "minecraft:stone_pressure_plate",
            "minecraft:stone_shovel", "minecraft:stone_slab", "minecraft:stone_spear",
            "minecraft:stone_stairs", "minecraft:stone_sword", "minecraft:stonecutter",
            "minecraft:torch", "minecraft:tripwire_hook", "minecraft:white_banner",
            "minecraft:white_bed", "minecraft:white_carpet", "minecraft:white_harness",
            "minecraft:white_wool_from_string", "minecraft:wooden_axe", "minecraft:wooden_hoe",
            "minecraft:wooden_pickaxe", "minecraft:wooden_shovel", "minecraft:wooden_spear",
            "minecraft:wooden_sword");
}
