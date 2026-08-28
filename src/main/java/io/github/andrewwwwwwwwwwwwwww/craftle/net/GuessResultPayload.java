package io.github.andrewwwwwwwwwwwwwww.craftle.net;

import io.github.andrewwwwwwwwwwwwwww.craftle.Craftle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> client: feedback for one submitted guess. An empty {@code colors} array means
 * the guess was rejected (stale day, malformed grid, game already over) — the screen just
 * unlocks the craft button; any explanation arrives as a chat message.
 *
 * <p>{@code answer}/{@code resultItemId} are empty until the game is finished.</p>
 */
public record GuessResultPayload(byte mode, byte[] colors, byte status, byte[] answer,
                                 String resultItemId, int resultCount,
                                 StatsSnapshot stats) implements CustomPacketPayload {

    public static final Type<GuessResultPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Craftle.MOD_ID, "guess_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GuessResultPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, GuessResultPayload::mode,
            ByteBufCodecs.BYTE_ARRAY, GuessResultPayload::colors,
            ByteBufCodecs.BYTE, GuessResultPayload::status,
            ByteBufCodecs.BYTE_ARRAY, GuessResultPayload::answer,
            ByteBufCodecs.STRING_UTF8, GuessResultPayload::resultItemId,
            ByteBufCodecs.VAR_INT, GuessResultPayload::resultCount,
            StatsSnapshot.STREAM_CODEC, GuessResultPayload::stats,
            GuessResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
