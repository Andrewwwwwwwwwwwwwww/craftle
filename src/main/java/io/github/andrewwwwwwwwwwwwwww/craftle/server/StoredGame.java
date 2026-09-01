package io.github.andrewwwwwwwwwwwwwww.craftle.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.GameMode;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.GuessEvaluator;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.CraftleGame;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.Palette;

import java.util.ArrayList;
import java.util.List;

/**
 * One game on disk. The answer grid and result are stored outright so a saved game can
 * always be rehydrated — even if the recipe pool changes under it (datapacks, mod
 * updates) mid-game or after the fact.
 *
 * @param day      puzzle day the daily belongs to (0 for random games)
 * @param recipeId source recipe id (informational)
 * @param answer   9 palette indices, -1 = empty
 * @param guesses  each guess as 9 palette indices
 */
public record StoredGame(long day, String recipeId, List<Integer> answer, String resultItemId,
                         int resultCount, List<List<Integer>> guesses) {

    public static final Codec<StoredGame> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("day").forGetter(StoredGame::day),
            Codec.STRING.fieldOf("recipe").forGetter(StoredGame::recipeId),
            Codec.INT.listOf().fieldOf("answer").forGetter(StoredGame::answer),
            Codec.STRING.fieldOf("resultItem").forGetter(StoredGame::resultItemId),
            Codec.INT.fieldOf("resultCount").forGetter(StoredGame::resultCount),
            Codec.INT.listOf().listOf().fieldOf("guesses").forGetter(StoredGame::guesses)
    ).apply(instance, StoredGame::new));

    public static StoredGame fresh(long day, String recipeId, int[] answer, String resultItemId, int resultCount) {
        return new StoredGame(day, recipeId, boxGrid(answer), resultItemId, resultCount, List.of());
    }

    public StoredGame withGuess(int[] guess) {
        List<List<Integer>> next = new ArrayList<>(guesses);
        next.add(boxGrid(guess));
        return new StoredGame(day, recipeId, answer, resultItemId, resultCount, List.copyOf(next));
    }

    /** Rebuilds the live game, replaying every stored guess. Returns null if the data is malformed. */
    public CraftleGame rehydrate(GameMode mode) {
        int[] answerGrid = unboxGrid(answer);
        if (answerGrid == null) {
            return null;
        }
        CraftleGame game = new CraftleGame(mode, recipeId, resultItemId, resultCount, answerGrid, Palette.SIZE);
        for (List<Integer> guess : guesses) {
            int[] grid = unboxGrid(guess);
            if (grid == null) {
                return null;
            }
            game.restoreGuess(grid);
        }
        return game;
    }

    private static List<Integer> boxGrid(int[] grid) {
        List<Integer> out = new ArrayList<>(grid.length);
        for (int cell : grid) {
            out.add(cell);
        }
        return List.copyOf(out);
    }

    private static int[] unboxGrid(List<Integer> cells) {
        if (cells.size() != 9) {
            return null;
        }
        int[] out = new int[9];
        for (int i = 0; i < 9; i++) {
            int cell = cells.get(i);
            if (cell != GuessEvaluator.NO_ITEM && (cell < 0 || cell >= Palette.SIZE)) {
                return null; // corrupt/foreign cell value — let the caller replace the save
            }
            out[i] = cell;
        }
        return out;
    }
}
