package io.github.andrewwwwwwwwwwwwwww.craftle.game;

import java.util.Arrays;

/**
 * Wordle-style evaluation of a 3x3 crafting-grid guess against the secret recipe grid.
 *
 * <p>Grids are int[9] in row-major order; each cell holds a palette index, or
 * {@link #NO_ITEM} for an empty cell. Duplicate handling matches Wordle: greens are
 * claimed first, then oranges are awarded in reading order only while unclaimed copies
 * of that ingredient remain in the recipe.</p>
 */
public final class GuessEvaluator {
    public static final int GRID_SIZE = 9;
    public static final int NO_ITEM = -1;

    private GuessEvaluator() {
    }

    public static CellState[] evaluate(int[] guess, int[] answer, int paletteSize) {
        if (guess.length != GRID_SIZE || answer.length != GRID_SIZE) {
            throw new IllegalArgumentException("grids must have exactly " + GRID_SIZE + " cells");
        }
        CellState[] out = new CellState[GRID_SIZE];
        int[] unclaimed = new int[paletteSize];

        // First pass: exact matches, and count the recipe's remaining (non-green) ingredients.
        // Out-of-range cells (defense against corrupt data) simply never match anything.
        for (int i = 0; i < GRID_SIZE; i++) {
            if (guess[i] != NO_ITEM && guess[i] == answer[i]) {
                out[i] = CellState.CORRECT;
            } else if (answer[i] >= 0 && answer[i] < paletteSize) {
                unclaimed[answer[i]]++;
            }
        }

        // Second pass: present/absent for the rest.
        for (int i = 0; i < GRID_SIZE; i++) {
            if (out[i] != null) {
                continue;
            }
            if (guess[i] == NO_ITEM) {
                out[i] = CellState.EMPTY;
            } else if (guess[i] >= 0 && guess[i] < paletteSize && unclaimed[guess[i]] > 0) {
                unclaimed[guess[i]]--;
                out[i] = CellState.PRESENT;
            } else {
                out[i] = CellState.ABSENT;
            }
        }
        return out;
    }

    /** A guess wins when it reproduces the recipe grid exactly, empty cells included. */
    public static boolean isWin(int[] guess, int[] answer) {
        return Arrays.equals(guess, answer);
    }
}
