package io.github.andrewwwwwwwwwwwwwww.craftle.game;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Random;

/**
 * Deterministic global daily-puzzle selection. Every server running the same game
 * version (and therefore the same eligible recipe pool) picks the same puzzle for a
 * given UTC calendar day.
 */
public final class DailyPicker {
    /** Fixed salt so the sequence is unique to this game, not shared with other day-seeded code. */
    private static final long SALT = 0x4D43_4C45_2026L; // "MCLE" 2026

    private DailyPicker() {
    }

    /** Days since 1970-01-01 in UTC — the identity of "today's puzzle" everywhere. */
    public static long todayUtc() {
        return LocalDate.now(ZoneOffset.UTC).toEpochDay();
    }

    /** Index into a pool of {@code poolSize} recipes for the given UTC day. */
    public static int pickIndex(long epochDay, int poolSize) {
        if (poolSize <= 0) {
            throw new IllegalArgumentException("empty recipe pool");
        }
        // Mix the day through a fixed-increment splitmix step so consecutive days
        // don't walk the pool in order, then draw once from a seeded PRNG.
        long seed = (epochDay + SALT) * 0x9E3779B97F4A7C15L;
        return new Random(seed).nextInt(poolSize);
    }
}
