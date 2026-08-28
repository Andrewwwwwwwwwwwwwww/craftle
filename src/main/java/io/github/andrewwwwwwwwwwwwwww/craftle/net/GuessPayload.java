package io.github.andrewwwwwwwwwwwwwww.craftle.net;

import io.github.andrewwwwwwwwwwwwwww.craftle.Craftle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server: submit one guess for the given mode. Cells are palette indices,
 * -1 = empty. The server re-validates everything.
 */
public record GuessPayload(byte mode, byte[] cells) implements CustomPacketPayload {

    public static final Type<GuessPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Craftle.MOD_ID, "guess"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GuessPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, GuessPayload::mode,
            ByteBufCodecs.BYTE_ARRAY, GuessPayload::cells,
            GuessPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
