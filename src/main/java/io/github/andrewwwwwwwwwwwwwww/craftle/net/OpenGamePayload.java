package io.github.andrewwwwwwwwwwwwwww.craftle.net;

import io.github.andrewwwwwwwwwwwwwww.craftle.Craftle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Server -> client: open (or refresh) the Craftle screen with the full state of one
 * game. The secret recipe is never included while the game is in progress — {@code answer}
 * and {@code resultItemId} are empty until the game is finished.
 *
 * @param mode         GameMode id (0 = daily, 1 = random)
 * @param paletteIds   ingredient item ids, in palette-index order
 * @param guesses      previous guesses, each byte[9] of palette indices (-1 = empty)
 * @param results      per-guess feedback, each byte[9] of CellState ids
 * @param status       GameStatus id
 * @param answer       the secret grid — empty array unless finished
 * @param resultItemId crafted item id — "" unless finished
 * @param resultCount  crafted item count (display only)
 * @param epochDay     UTC epoch day of the daily puzzle (0 for random mode)
 * @param stats        daily career stats for the results panel
 */
public record OpenGamePayload(byte mode, List<String> paletteIds, List<byte[]> guesses,
                              List<byte[]> results, byte status, byte[] answer,
                              String resultItemId, int resultCount, long epochDay,
                              StatsSnapshot stats) implements CustomPacketPayload {

    public static final Type<OpenGamePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Craftle.MOD_ID, "open_game"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenGamePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, OpenGamePayload::mode,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), OpenGamePayload::paletteIds,
            ByteBufCodecs.BYTE_ARRAY.apply(ByteBufCodecs.list()), OpenGamePayload::guesses,
            ByteBufCodecs.BYTE_ARRAY.apply(ByteBufCodecs.list()), OpenGamePayload::results,
            ByteBufCodecs.BYTE, OpenGamePayload::status,
            ByteBufCodecs.BYTE_ARRAY, OpenGamePayload::answer,
            ByteBufCodecs.STRING_UTF8, OpenGamePayload::resultItemId,
            ByteBufCodecs.VAR_INT, OpenGamePayload::resultCount,
            ByteBufCodecs.VAR_LONG, OpenGamePayload::epochDay,
            StatsSnapshot.STREAM_CODEC, OpenGamePayload::stats,
            OpenGamePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
