package io.github.andrewwwwwwwwwwwwwww.craftle.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.StatsSnapshot;

/** Daily-mode career record for one player. Immutable — updates replace the record. */
public record PlayerStats(int played, int wins, int currentStreak, int maxStreak, long lastWinDay) {

    public static final PlayerStats FRESH = new PlayerStats(0, 0, 0, 0, -1L);

    public static final Codec<PlayerStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("played").forGetter(PlayerStats::played),
            Codec.INT.fieldOf("wins").forGetter(PlayerStats::wins),
            Codec.INT.fieldOf("currentStreak").forGetter(PlayerStats::currentStreak),
            Codec.INT.fieldOf("maxStreak").forGetter(PlayerStats::maxStreak),
            Codec.LONG.optionalFieldOf("lastWinDay", -1L).forGetter(PlayerStats::lastWinDay)
    ).apply(instance, PlayerStats::new));

    public PlayerStats afterDaily(boolean won, long day) {
        if (!won) {
            return new PlayerStats(played + 1, wins, 0, maxStreak, lastWinDay);
        }
        int streak = (lastWinDay == day - 1) ? currentStreak + 1 : 1;
        return new PlayerStats(played + 1, wins + 1, streak, Math.max(maxStreak, streak), day);
    }

    public StatsSnapshot snapshot() {
        return new StatsSnapshot(played, wins, currentStreak, maxStreak);
    }
}
