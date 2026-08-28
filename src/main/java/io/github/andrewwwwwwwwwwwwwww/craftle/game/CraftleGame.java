package io.github.andrewwwwwwwwwwwwwww.craftle.game;

import java.util.ArrayList;
import java.util.List;

/**
 * One puzzle in progress (or finished) for one player. Pure state machine — no
 * Minecraft classes — so it is unit-testable and shared conceptually with the client.
 */
public final class CraftleGame {
    public static final int MAX_GUESSES = 10;

    private final GameMode mode;
    private final String recipeId;
    private final String resultItemId;
    private final int resultCount;
    private final int[] answer;
    private final int paletteSize;
    private final List<int[]> guesses = new ArrayList<>();
    private final List<CellState[]> results = new ArrayList<>();
    private GameStatus status = GameStatus.IN_PROGRESS;

    public CraftleGame(GameMode mode, String recipeId, String resultItemId, int resultCount,
                           int[] answer, int paletteSize) {
        if (answer.length != GuessEvaluator.GRID_SIZE) {
            throw new IllegalArgumentException("answer grid must have 9 cells");
        }
        this.mode = mode;
        this.recipeId = recipeId;
        this.resultItemId = resultItemId;
        this.resultCount = resultCount;
        this.answer = answer.clone();
        this.paletteSize = paletteSize;
    }

    /**
     * Validates and applies a guess. Returns the per-cell feedback, or null when the
     * guess is rejected (game over, malformed grid, empty grid, or out-of-range item).
     */
    public CellState[] submit(int[] guess) {
        if (status.finished() || guesses.size() >= MAX_GUESSES) {
            return null;
        }
        if (guess == null || guess.length != GuessEvaluator.GRID_SIZE) {
            return null;
        }
        boolean any = false;
        for (int cell : guess) {
            if (cell != GuessEvaluator.NO_ITEM) {
                if (cell < 0 || cell >= paletteSize) {
                    return null;
                }
                any = true;
            }
        }
        if (!any) {
            return null;
        }

        int[] copy = guess.clone();
        CellState[] result = GuessEvaluator.evaluate(copy, answer, paletteSize);
        guesses.add(copy);
        results.add(result);
        if (GuessEvaluator.isWin(copy, answer)) {
            status = GameStatus.WON;
        } else if (guesses.size() >= MAX_GUESSES) {
            status = GameStatus.LOST;
        }
        return result;
    }

    /** Restores a finished/partial guess history without re-validating (used when loading saves). */
    public void restoreGuess(int[] guess) {
        if (status.finished() || guesses.size() >= MAX_GUESSES) {
            return; // corrupt saves must not replay a finished game into a different status
        }
        int[] copy = guess.clone();
        guesses.add(copy);
        results.add(GuessEvaluator.evaluate(copy, answer, paletteSize));
        if (GuessEvaluator.isWin(copy, answer)) {
            status = GameStatus.WON;
        } else if (guesses.size() >= MAX_GUESSES) {
            status = GameStatus.LOST;
        }
    }

    public GameMode mode() {
        return mode;
    }

    public String recipeId() {
        return recipeId;
    }

    public String resultItemId() {
        return resultItemId;
    }

    public int resultCount() {
        return resultCount;
    }

    public int[] answer() {
        return answer.clone();
    }

    public int paletteSize() {
        return paletteSize;
    }

    public GameStatus status() {
        return status;
    }

    public int guessCount() {
        return guesses.size();
    }

    public List<int[]> guesses() {
        return guesses;
    }

    public List<CellState[]> results() {
        return results;
    }
}
