package io.github.andrewwwwwwwwwwwwwww.craftle.game;

/**
 * One eligible secret recipe: a shaped crafting recipe whose every ingredient maps to a
 * palette item, canonicalized to palette indices and anchored to the top-left of the
 * 3x3 grid.
 *
 * @param recipeId     the recipe's registry id, e.g. "minecraft:torch"
 * @param grid         row-major 3x3 grid of palette indices ({@link GuessEvaluator#NO_ITEM} = empty)
 * @param resultItemId registry id of the crafted item, for the reveal at game end
 * @param resultCount  how many items the craft yields (display only)
 */
public record PuzzleRecipe(String recipeId, int[] grid, String resultItemId, int resultCount) {

    public PuzzleRecipe {
        if (grid.length != GuessEvaluator.GRID_SIZE) {
            throw new IllegalArgumentException("grid must have 9 cells");
        }
    }

    public int ingredientCount() {
        int n = 0;
        for (int cell : grid) {
            if (cell != GuessEvaluator.NO_ITEM) {
                n++;
            }
        }
        return n;
    }

    /** Stable content key used to de-duplicate recipes that canonicalize to the same grid. */
    public String gridKey() {
        StringBuilder sb = new StringBuilder(GuessEvaluator.GRID_SIZE * 3);
        for (int cell : grid) {
            sb.append(cell).append(',');
        }
        return sb.toString();
    }
}
