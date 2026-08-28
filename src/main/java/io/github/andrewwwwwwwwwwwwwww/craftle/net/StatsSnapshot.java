package io.github.andrewwwwwwwwwwwwwww.craftle.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Daily-mode career stats, sent for display on the results panel. */
public record StatsSnapshot(int played, int wins, int currentStreak, int maxStreak) {

    public static final StatsSnapshot EMPTY = new StatsSnapshot(0, 0, 0, 0);

    public static final StreamCodec<RegistryFriendlyByteBuf, StatsSnapshot> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StatsSnapshot::played,
            ByteBufCodecs.VAR_INT, StatsSnapshot::wins,
            ByteBufCodecs.VAR_INT, StatsSnapshot::currentStreak,
            ByteBufCodecs.VAR_INT, StatsSnapshot::maxStreak,
            StatsSnapshot::new);
}
