package io.github.andrewwwwwwwwwwwwwww.craftle.game;

/**
 * Per-cell feedback for a submitted guess.
 */
public enum CellState {
    /** Cell was left empty in the guess. */
    EMPTY,
    /** The ingredient placed here is not in the recipe (or all copies are already accounted for). */
    ABSENT,
    /** The ingredient is in the recipe but belongs in a different cell. */
    PRESENT,
    /** The ingredient is correct and in the right cell. */
    CORRECT;

    public static final CellState[] VALUES = values();

    public byte id() {
        return (byte) ordinal();
    }

    public static CellState byId(int id) {
        return VALUES[id];
    }
}
