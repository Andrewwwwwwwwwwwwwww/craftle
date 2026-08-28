package io.github.andrewwwwwwwwwwwwwww.craftle.net;

import io.github.andrewwwwwwwwwwwwwww.craftle.game.CellState;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.GameMode;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.GameStatus;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.GuessEvaluator;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.CraftleGame;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.Palette;

/**
 * Client-side sanity checks for clientbound payloads. A well-behaved server never fails
 * these; a hostile or version-skewed one must degrade to an ignored payload, not a
 * render-thread crash.
 */
public final class PayloadChecks {
    private static final int MAX_RESULT_COUNT = 99 * 64;

    private PayloadChecks() {
    }

    public static boolean validOpen(OpenGamePayload p) {
        int paletteSize = p.paletteIds().size();
        if (paletteSize < 1 || paletteSize > Palette.SIZE) {
            return false;
        }
        if (p.mode() < 0 || p.mode() >= GameMode.VALUES.length) {
            return false;
        }
        if (p.status() < 0 || p.status() >= GameStatus.VALUES.length) {
            return false;
        }
        if (p.guesses().size() != p.results().size()
                || p.guesses().size() > CraftleGame.MAX_GUESSES) {
            return false;
        }
        for (byte[] guess : p.guesses()) {
            if (!GridBytes.isValidGrid(guess, paletteSize)) {
                return false;
            }
        }
        for (byte[] result : p.results()) {
            if (!validColors(result)) {
                return false;
            }
        }
        return validAnswer(p.answer(), paletteSize)
                && p.resultCount() >= 0 && p.resultCount() <= MAX_RESULT_COUNT
                && p.epochDay() >= 0;
    }

    /** Colors of length 0 (the reject/unlock signal) are valid here; length 9 is graded. */
    public static boolean validResult(GuessResultPayload p, int paletteSize) {
        if (p.status() < 0 || p.status() >= GameStatus.VALUES.length) {
            return false;
        }
        if (p.colors().length != 0 && !validColors(p.colors())) {
            return false;
        }
        return validAnswer(p.answer(), paletteSize)
                && p.resultCount() >= 0 && p.resultCount() <= MAX_RESULT_COUNT;
    }

    /** A preview answer is usable only with a real item id and a sane stack count. */
    public static boolean validPreview(PreviewResultPayload p) {
        return !p.itemId().isEmpty()
                && p.count() > 0 && p.count() <= MAX_RESULT_COUNT
                && p.cells().length == GuessEvaluator.GRID_SIZE;
    }

    private static boolean validColors(byte[] colors) {
        if (colors.length != GuessEvaluator.GRID_SIZE) {
            return false;
        }
        for (byte color : colors) {
            if (color < 0 || color >= CellState.VALUES.length) {
                return false;
            }
        }
        return true;
    }

    private static boolean validAnswer(byte[] answer, int paletteSize) {
        return answer.length == 0 || GridBytes.isValidGrid(answer, paletteSize);
    }
}
