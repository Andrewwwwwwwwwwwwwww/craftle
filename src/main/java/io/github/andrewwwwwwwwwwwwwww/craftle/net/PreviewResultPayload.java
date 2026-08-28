package io.github.andrewwwwwwwwwwwwwww.craftle.net;

import io.github.andrewwwwwwwwwwwwwww.craftle.Craftle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> client: what the previewed arrangement crafts. An empty {@code itemId} means
 * the grid matches no recipe, so the output slot stays empty.
 *
 * @param cells the grid this answer belongs to, echoed back so a stale reply can be dropped
 */
public record PreviewResultPayload(byte[] cells, String itemId, int count) implements CustomPacketPayload {

    public static final Type<PreviewResultPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Craftle.MOD_ID, "preview_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PreviewResultPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE_ARRAY, PreviewResultPayload::cells,
            ByteBufCodecs.STRING_UTF8, PreviewResultPayload::itemId,
            ByteBufCodecs.VAR_INT, PreviewResultPayload::count,
            PreviewResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
