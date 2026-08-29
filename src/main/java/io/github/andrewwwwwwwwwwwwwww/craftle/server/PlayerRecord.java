package io.github.andrewwwwwwwwwwwwwww.craftle.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/** Everything Craftle knows about one player: current games, daily stats, last nudge. */
public record PlayerRecord(Optional<StoredGame> daily, Optional<StoredGame> random, PlayerStats stats,
                           long lastNotifiedDay) {

    public static final PlayerRecord FRESH =
            new PlayerRecord(Optional.empty(), Optional.empty(), PlayerStats.FRESH, -1L);

    public static final Codec<PlayerRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StoredGame.CODEC.optionalFieldOf("daily").forGetter(PlayerRecord::daily),
            StoredGame.CODEC.optionalFieldOf("random").forGetter(PlayerRecord::random),
            PlayerStats.CODEC.optionalFieldOf("stats", PlayerStats.FRESH).forGetter(PlayerRecord::stats),
            Codec.LONG.optionalFieldOf("lastNotifiedDay", -1L).forGetter(PlayerRecord::lastNotifiedDay)
    ).apply(instance, PlayerRecord::new));

    public PlayerRecord withDaily(StoredGame game) {
        return new PlayerRecord(Optional.ofNullable(game), random, stats, lastNotifiedDay);
    }

    public PlayerRecord withRandom(StoredGame game) {
        return new PlayerRecord(daily, Optional.ofNullable(game), stats, lastNotifiedDay);
    }

    public PlayerRecord withStats(PlayerStats newStats) {
        return new PlayerRecord(daily, random, newStats, lastNotifiedDay);
    }

    public PlayerRecord withNotifiedDay(long day) {
        return new PlayerRecord(daily, random, stats, day);
    }

    /** True when today's puzzle is still untouched — nothing guessed on it yet. */
    public boolean dailyUnstarted(long today) {
        StoredGame game = daily.orElse(null);
        return game == null || game.day() != today || game.guesses().isEmpty();
    }
}
