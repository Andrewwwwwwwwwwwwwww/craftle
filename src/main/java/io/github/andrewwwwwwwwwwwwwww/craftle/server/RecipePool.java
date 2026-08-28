package io.github.andrewwwwwwwwwwwwwww.craftle.server;

import io.github.andrewwwwwwwwwwwwwww.craftle.ItemIds;
import io.github.andrewwwwwwwwwwwwwww.craftle.Craftle;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.GuessEvaluator;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.Palette;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.PuzzleRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Scans the server's recipe registry for shaped crafting recipes whose every ingredient
 * accepts a palette item, and canonicalizes them into {@link PuzzleRecipe}s (palette
 * indices, anchored top-left). Rebuilt on server start and datapack reload.
 *
 * <p>The pool is sorted by recipe id, so vanilla servers on the same game version derive
 * an identical pool — which is what makes the daily puzzle globally shared.</p>
 */
public final class RecipePool {
    private RecipePool() {
    }

    private static List<PuzzleRecipe> pool = List.of();

    public static List<PuzzleRecipe> pool() {
        return pool;
    }

    public static PuzzleRecipe byId(String recipeId) {
        for (PuzzleRecipe recipe : pool) {
            if (recipe.recipeId().equals(recipeId)) {
                return recipe;
            }
        }
        return null;
    }

    public static void rebuild(MinecraftServer server) {
        List<Item> palette = new ArrayList<>(Palette.SIZE);
        for (String id : Palette.ITEM_IDS) {
            if (!ItemIds.exists(id)) {
                Craftle.LOGGER.warn("[Craftle] palette item {} does not exist in this game version", id);
                pool = List.of();
                return;
            }
            palette.add(ItemIds.resolve(id));
        }

        List<RecipeHolder<?>> holders = new ArrayList<>(server.getRecipeManager().getRecipes());
        holders.sort(Comparator.comparing(holder -> holder.id().identifier().toString()));

        List<PuzzleRecipe> out = new ArrayList<>();
        Set<String> seenGrids = new HashSet<>();
        for (RecipeHolder<?> holder : holders) {
            PuzzleRecipe puzzle = canonicalize(holder, palette);
            if (puzzle != null && seenGrids.add(puzzle.gridKey())) {
                out.add(puzzle);
            }
        }
        pool = List.copyOf(out);
        Craftle.LOGGER.info("[Craftle] recipe pool ready: {} puzzles", pool.size());
    }

    private static PuzzleRecipe canonicalize(RecipeHolder<?> holder, List<Item> palette) {
        if (!(holder.value() instanceof ShapedRecipe shaped)) {
            return null;
        }
        int width = shaped.getWidth();
        int height = shaped.getHeight();
        if (width > 3 || height > 3) {
            return null;
        }

        List<Optional<Ingredient>> ingredients = shaped.getIngredients();
        if (ingredients.size() != width * height) {
            return null;
        }

        int[] grid = new int[GuessEvaluator.GRID_SIZE];
        Arrays.fill(grid, GuessEvaluator.NO_ITEM);
        List<ItemStack> inputStacks = new ArrayList<>(width * height);
        int filled = 0;
        for (int i = 0; i < width * height; i++) {
            Optional<Ingredient> slot = ingredients.get(i);
            if (slot.isEmpty() || slot.get().isEmpty()) {
                inputStacks.add(ItemStack.EMPTY);
                continue;
            }
            int paletteIndex = firstMatch(slot.get(), palette);
            if (paletteIndex < 0) {
                return null;
            }
            grid[(i / width) * 3 + (i % width)] = paletteIndex;
            inputStacks.add(new ItemStack(palette.get(paletteIndex)));
            filled++;
        }
        if (filled < 2) {
            return null; // single-ingredient grids are not puzzles
        }

        ItemStack result;
        try {
            result = shaped.assemble(CraftingInput.of(width, height, inputStacks));
        } catch (Exception e) {
            return null;
        }
        if (result == null || result.isEmpty()) {
            return null;
        }
        return new PuzzleRecipe(holder.id().identifier().toString(), grid,
                ItemIds.idOf(result.getItem()), result.getCount());
    }

    private static int firstMatch(Ingredient ingredient, List<Item> palette) {
        for (int i = 0; i < palette.size(); i++) {
            if (ingredient.test(new ItemStack(palette.get(i)))) {
                return i;
            }
        }
        return -1;
    }
}
