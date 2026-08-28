package io.github.andrewwwwwwwwwwwwwww.craftle.net;

import io.github.andrewwwwwwwwwwwwwww.craftle.game.CellState;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.GuessEvaluator;

/**
 * Grid/feedback arrays travel as byte[9] (palette index or -1 = empty cell;
 * CellState ordinal for feedback). These helpers convert to the int/enum forms
 * the game logic uses.
 */
public final class GridBytes {
    private GridBytes() {
    }

    public static byte[] fromGrid(int[] grid) {
        byte[] out = new byte[grid.length];
        for (int i = 0; i < grid.length; i++) {
            out[i] = (byte) grid[i];
        }
        return out;
    }

    public static int[] toGrid(byte[] bytes) {
        int[] out = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            out[i] = bytes[i];
        }
        return out;
    }

    public static byte[] fromResults(CellState[] results) {
        byte[] out = new byte[results.length];
        for (int i = 0; i < results.length; i++) {
            out[i] = results[i].id();
        }
        return out;
    }

    public static CellState[] toResults(byte[] bytes) {
        CellState[] out = new CellState[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            out[i] = CellState.byId(bytes[i]);
        }
        return out;
    }

    public static boolean isValidGrid(byte[] bytes, int paletteSize) {
        if (bytes.length != GuessEvaluator.GRID_SIZE) {
            return false;
        }
        for (byte cell : bytes) {
            if (cell != GuessEvaluator.NO_ITEM && (cell < 0 || cell >= paletteSize)) {
                return false;
            }
        }
        return true;
    }
}
