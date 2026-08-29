package io.github.andrewwwwwwwwwwwwwww.craftle.game;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Random;

/**
 * Deterministic global daily-puzzle selection. Every server running the same game version
 * derives the same puzzle for a given UTC day.
 *
 * <p>Puzzles are dealt like a shuffled deck rather than drawn at random each day: the pool
 * is shuffled, handed out one per day until it is exhausted, then reshuffled into a new
 * order for the next pass. So every puzzle appears exactly once per cycle — no repeats
 * inside a cycle, nothing sitting unused for months — and because each cycle is shuffled
 * from its own seed, successive passes don't run in a recognisable order.</p>
 */
public final class DailyPicker {
    /** Fixed salt so the sequence is unique to this game, not shared with other day-seeded code. */
    private static final long SALT = 0x4D43_4C45_2026L; // "MCLE" 2026
    private static final long MIX = 0x9E3779B97F4A7C15L;

    private DailyPicker() {
    }

    /** Days since 1970-01-01 in UTC — the identity of "today's puzzle" everywhere. */
    public static long todayUtc() {
        return LocalDate.now(ZoneOffset.UTC).toEpochDay();
    }

    /** Index into a pool of {@code poolSize} puzzles for the given UTC day. */
    public static int pickIndex(long epochDay, int poolSize) {
        if (poolSize <= 0) {
            throw new IllegalArgumentException("empty recipe pool");
        }
        if (poolSize == 1) {
            return 0;
        }
        long cycle = Math.floorDiv(epochDay, poolSize);
        int position = (int) Math.floorMod(epochDay, poolSize);
        return dealFor(cycle, poolSize)[position];
    }

    /**
     * The shuffled order this cycle deals in. The first card is swapped away if it would
     * repeat the last card of the previous cycle, so a puzzle never lands two days running.
     */
    private static int[] dealFor(long cycle, int poolSize) {
        int[] order = shuffle(cycle, poolSize);
        int previousLast = shuffle(cycle - 1, poolSize)[poolSize - 1];
        if (order[0] == previousLast) {
            order[0] = order[1];
            order[1] = previousLast;
        }
        return order;
    }

    private static int[] shuffle(long cycle, int poolSize) {
        int[] order = new int[poolSize];
        for (int i = 0; i < poolSize; i++) {
            order[i] = i;
        }
        Random rng = new Random((cycle + SALT) * MIX);
        for (int i = poolSize - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = order[i];
            order[i] = order[j];
            order[j] = tmp;
        }
        return order;
    }
}
