package io.github.andrewwwwwwwwwwwwwww.craftle.game;

import org.junit.jupiter.api.Test;

import static io.github.andrewwwwwwwwwwwwwww.craftle.game.CellState.ABSENT;
import static io.github.andrewwwwwwwwwwwwwww.craftle.game.CellState.CORRECT;
import static io.github.andrewwwwwwwwwwwwwww.craftle.game.CellState.EMPTY;
import static io.github.andrewwwwwwwwwwwwwww.craftle.game.CellState.PRESENT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuessEvaluatorTest {
    private static final int P = 18;
    private static final int E = GuessEvaluator.NO_ITEM;

    // Torch-like answer anchored top-left: coal(6) above stick(5) in column 0.
    private static final int[] TORCH = {6, E, E, 5, E, E, E, E, E};

    @Test
    void exactMatchIsAllGreenAndWin() {
        CellState[] r = GuessEvaluator.evaluate(TORCH.clone(), TORCH, P);
        assertArrayEquals(new CellState[]{CORRECT, EMPTY, EMPTY, CORRECT, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY}, r);
        assertTrue(GuessEvaluator.isWin(TORCH.clone(), TORCH));
    }

    @Test
    void rightItemsWrongCellsAreOrange() {
        int[] guess = {E, 6, E, E, 5, E, E, E, E};
        CellState[] r = GuessEvaluator.evaluate(guess, TORCH, P);
        assertArrayEquals(new CellState[]{EMPTY, PRESENT, EMPTY, EMPTY, PRESENT, EMPTY, EMPTY, EMPTY, EMPTY}, r);
        assertFalse(GuessEvaluator.isWin(guess, TORCH));
    }

    @Test
    void itemNotInRecipeIsGrey() {
        int[] guess = {7, E, E, E, E, E, E, E, E}; // diamond
        CellState[] r = GuessEvaluator.evaluate(guess, TORCH, P);
        assertEquals(ABSENT, r[0]);
    }

    @Test
    void duplicatesOnlyGetOrangeWhileCopiesRemain() {
        // Answer has ONE coal; guess places coal in two wrong cells -> first orange, second grey.
        int[] guess = {E, 6, 6, E, E, E, E, E, E};
        CellState[] r = GuessEvaluator.evaluate(guess, TORCH, P);
        assertEquals(PRESENT, r[1]);
        assertEquals(ABSENT, r[2]);
    }

    @Test
    void greenConsumesTheCopyBeforeOrangeIsAwarded() {
        // Answer: two sticks at cells 3 and 6 (sword-ish). Guess: stick correct at 3, stick wrong at 0, stick wrong at 1.
        int[] answer = {8, E, E, 5, E, E, 5, E, E};
        int[] guess = {5, 5, E, 5, E, E, E, E, E};
        CellState[] r = GuessEvaluator.evaluate(guess, answer, P);
        assertEquals(CORRECT, r[3]);
        // one stick copy (cell 6) unclaimed -> exactly one orange among the wrong-placed sticks, reading order first
        assertEquals(PRESENT, r[0]);
        assertEquals(ABSENT, r[1]);
    }

    @Test
    void emptyGuessCellsGiveNoFeedback() {
        int[] guess = new int[]{E, E, E, E, E, E, E, E, 6};
        CellState[] r = GuessEvaluator.evaluate(guess, TORCH, P);
        for (int i = 0; i < 8; i++) {
            assertEquals(EMPTY, r[i]);
        }
        assertEquals(PRESENT, r[8]);
    }

    @Test
    void itemOnEmptyAnswerCellCanStillBeOrange() {
        // Coal placed where the answer is empty, but coal exists elsewhere -> orange.
        int[] guess = {E, E, E, E, E, E, 6, E, E};
        CellState[] r = GuessEvaluator.evaluate(guess, TORCH, P);
        assertEquals(PRESENT, r[6]);
    }

    @Test
    void fullGameFlowWinAndLoss() {
        CraftleGame win = new CraftleGame(GameMode.DAILY, "minecraft:torch", "minecraft:torch", 4, TORCH, P);
        assertNotNull(win.submit(new int[]{7, E, E, E, E, E, E, E, E}));
        assertNotNull(win.submit(TORCH.clone()));
        assertEquals(GameStatus.WON, win.status());
        assertNull(win.submit(TORCH.clone()), "no guesses after game over");

        CraftleGame lose = new CraftleGame(GameMode.RANDOM, "minecraft:torch", "minecraft:torch", 4, TORCH, P);
        for (int i = 0; i < CraftleGame.MAX_GUESSES; i++) {
            assertNotNull(lose.submit(new int[]{7, E, E, E, E, E, E, E, E}));
        }
        assertEquals(GameStatus.LOST, lose.status());
        assertNull(lose.submit(TORCH.clone()));
    }

    @Test
    void malformedGuessesAreRejected() {
        CraftleGame g = new CraftleGame(GameMode.DAILY, "id", "id", 1, TORCH, P);
        assertNull(g.submit(new int[]{E, E, E, E, E, E, E, E, E}), "all-empty grid rejected");
        assertNull(g.submit(new int[]{99, E, E, E, E, E, E, E, E}), "out-of-palette index rejected");
        assertNull(g.submit(new int[]{1, 2}), "wrong-size grid rejected");
        assertEquals(0, g.guessCount());
    }

    @Test
    void everyPuzzleIsDealtOncePerCycle() {
        int pool = 127;
        for (long cycle = 0; cycle < 8; cycle++) {
            boolean[] seen = new boolean[pool];
            for (int i = 0; i < pool; i++) {
                int index = DailyPicker.pickIndex(cycle * pool + i, pool);
                assertFalse(seen[index], "puzzle " + index + " dealt twice in one cycle");
                seen[index] = true;
            }
        }
    }

    @Test
    void cyclesRunInDifferentOrders() {
        int pool = 127;
        int[] first = new int[pool];
        int[] second = new int[pool];
        for (int i = 0; i < pool; i++) {
            first[i] = DailyPicker.pickIndex(i, pool);
            second[i] = DailyPicker.pickIndex((long) pool + i, pool);
        }
        assertFalse(java.util.Arrays.equals(first, second), "two cycles dealt in the same order");
    }

    @Test
    void neverRepeatsOnConsecutiveDays() {
        int pool = 127;
        for (long day = -400; day < 1200; day++) {
            assertNotEquals(DailyPicker.pickIndex(day, pool), DailyPicker.pickIndex(day + 1, pool),
                    "same puzzle on days " + day + " and " + (day + 1));
        }
    }

    @Test
    void dailyPickerIsDeterministicAndInRange() {
        for (long day = 20000; day < 20400; day++) {
            int a = DailyPicker.pickIndex(day, 57);
            int b = DailyPicker.pickIndex(day, 57);
            assertEquals(a, b);
            assertTrue(a >= 0 && a < 57);
        }
        // consecutive days shouldn't all collide on one index
        boolean varied = false;
        for (long day = 20001; day < 20010; day++) {
            if (DailyPicker.pickIndex(day, 57) != DailyPicker.pickIndex(20000, 57)) {
                varied = true;
            }
        }
        assertTrue(varied);
    }
}
