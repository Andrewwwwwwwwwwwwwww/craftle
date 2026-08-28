package io.github.andrewwwwwwwwwwwwwww.craftle.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/** Everything Craftle knows about one player: current games + daily stats. */
public record PlayerRecord(Optional<StoredGame> daily, Optional<StoredGame> random, PlayerStats stats) {

    public static final PlayerRecord FRESH = new PlayerRecord(Optional.empty(), Optional.empty(), PlayerStats.FRESH);

    public static final Codec<PlayerRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StoredGame.CODEC.optionalFieldOf("daily").forGetter(PlayerRecord::daily),
            StoredGame.CODEC.optionalFieldOf("random").forGetter(PlayerRecord::random),
            PlayerStats.CODEC.optionalFieldOf("stats", PlayerStats.FRESH).forGetter(PlayerRecord::stats)
    ).apply(instance, PlayerRecord::new));

    public PlayerRecord withDaily(StoredGame game) {
        return new PlayerRecord(Optional.ofNullable(game), random, stats);
    }

    public PlayerRecord withRandom(StoredGame game) {
        return new PlayerRecord(daily, Optional.ofNullable(game), stats);
    }

    public PlayerRecord withStats(PlayerStats newStats) {
        return new PlayerRecord(daily, random, newStats);
    }
}
