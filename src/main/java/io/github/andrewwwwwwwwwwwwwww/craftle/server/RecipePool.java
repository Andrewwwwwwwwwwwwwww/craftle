package io.github.andrewwwwwwwwwwwwwww.craftle.server;

import io.github.andrewwwwwwwwwwwwwww.craftle.ItemIds;
import io.github.andrewwwwwwwwwwwwwww.craftle.Craftle;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.DailyPicker;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.GuessEvaluator;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.Palette;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.PuzzleRecipe;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.VanillaRecipes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Resolves the fixed {@link VanillaRecipes} list against the running server, turning each
 * recipe into a {@link PuzzleRecipe} of palette indices anchored to the grid's top-left.
 *
 * <p>Selection is indexed by position in that fixed list, never by what the server happens
 * to have loaded, so every server picks the same puzzle for a given day. A server that has
 * removed or altered one of these recipes simply falls through to the next entry, and only
 * differs on the days that recipe would have come up.</p>
 */
public final class RecipePool {
    private RecipePool() {
    }

    /** Aligned 1:1 with {@link VanillaRecipes#IDS}; null where this server can't offer it. */
    private static PuzzleRecipe[] canonical = new PuzzleRecipe[0];
    private static List<PuzzleRecipe> available = List.of();

    public static boolean isEmpty() {
        return available.isEmpty();
    }

    public static int size() {
        return available.size();
    }

    /** Today's puzzle: the same one on every server that still has the recipe. */
    public static PuzzleRecipe daily(long epochDay) {
        if (canonical.length == 0) {
            return null;
        }
        int start = DailyPicker.pickIndex(epochDay, canonical.length);
        for (int i = 0; i < canonical.length; i++) {
            PuzzleRecipe puzzle = canonical[(start + i) % canonical.length];
            if (puzzle != null) {
                return puzzle;
            }
        }
        return null;
    }

    /** A practice puzzle, never today's daily. */
    public static PuzzleRecipe practice(Random rng, long today) {
        if (available.isEmpty()) {
            return null;
        }
        PuzzleRecipe todays = daily(today);
        for (int attempt = 0; attempt < 32; attempt++) {
            PuzzleRecipe pick = available.get(rng.nextInt(available.size()));
            if (todays == null || !pick.recipeId().equals(todays.recipeId())) {
                return pick;
            }
        }
        return available.get(rng.nextInt(available.size()));
    }

    public static void rebuild(MinecraftServer server) {
        List<Item> palette = new ArrayList<>(Palette.SIZE);
        for (String id : Palette.ITEM_IDS) {
            if (!ItemIds.exists(id)) {
                Craftle.LOGGER.warn("[Craftle] palette item {} does not exist in this game version", id);
                canonical = new PuzzleRecipe[0];
                available = List.of();
                return;
            }
            palette.add(ItemIds.resolve(id));
        }

        PuzzleRecipe[] resolved = new PuzzleRecipe[VanillaRecipes.IDS.size()];
        List<PuzzleRecipe> live = new ArrayList<>();
        Set<String> seenGrids = new HashSet<>();
        int missing = 0;
        for (int i = 0; i < VanillaRecipes.IDS.size(); i++) {
            String id = VanillaRecipes.IDS.get(i);
            PuzzleRecipe puzzle = lookUp(server, id, palette);
            if (puzzle == null) {
                missing++;
            } else if (seenGrids.add(puzzle.gridKey())) {
                resolved[i] = puzzle;
                live.add(puzzle);
            }
        }
        canonical = resolved;
        available = List.copyOf(live);
        if (missing > 0) {
            Craftle.LOGGER.info("[Craftle] {} of {} puzzles unavailable here (recipes changed or removed) —"
                    + " those days will differ from a vanilla server", missing, VanillaRecipes.IDS.size());
        }
        Craftle.LOGGER.info("[Craftle] recipe pool ready: {} puzzles", available.size());
    }

    private static PuzzleRecipe lookUp(MinecraftServer server, String id, List<Item> palette) {
        Identifier parsed = Identifier.tryParse(id);
        if (parsed == null) {
            return null;
        }
        Optional<RecipeHolder<?>> holder =
                server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, parsed));
        return holder.map(h -> canonicalize(h, id, palette)).orElse(null);
    }

    private static PuzzleRecipe canonicalize(RecipeHolder<? extends Recipe<?>> holder, String id, List<Item> palette) {
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
            return null;
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
        return new PuzzleRecipe(id, grid, ItemIds.idOf(result.getItem()), result.getCount());
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
