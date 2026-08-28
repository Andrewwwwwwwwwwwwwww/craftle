package io.github.andrewwwwwwwwwwwwwww.craftle.net;

import io.github.andrewwwwwwwwwwwwwww.craftle.Craftle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server: "what would this arrangement craft?". Sent whenever the grid changes
 * so the output slot can mirror a real crafting table. Carries no game state — the server
 * answers purely from the recipe registry.
 */
public record PreviewPayload(byte[] cells) implements CustomPacketPayload {

    public static final Type<PreviewPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Craftle.MOD_ID, "preview"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PreviewPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE_ARRAY, PreviewPayload::cells,
            PreviewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
