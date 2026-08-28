package io.github.andrewwwwwwwwwwwwwww.craftle.server;

import com.mojang.serialization.Codec;
import io.github.andrewwwwwwwwwwwwwww.craftle.Craftle;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** All per-player Craftle data, persisted on the overworld's data storage. */
public class CraftleState extends SavedData {

    public static final Codec<CraftleState> CODEC = Codec
            .unboundedMap(UUIDUtil.STRING_CODEC, PlayerRecord.CODEC)
            .xmap(CraftleState::new, state -> state.players);

    public static final SavedDataType<CraftleState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Craftle.MOD_ID, "games"),
            CraftleState::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, PlayerRecord> players;

    public CraftleState() {
        this.players = new HashMap<>();
    }

    private CraftleState(Map<UUID, PlayerRecord> map) {
        this.players = new HashMap<>(map);
    }

    public static CraftleState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public PlayerRecord record(UUID player) {
        return players.getOrDefault(player, PlayerRecord.FRESH);
    }

    public void put(UUID player, PlayerRecord record) {
        players.put(player, record);
        setDirty();
    }
}
