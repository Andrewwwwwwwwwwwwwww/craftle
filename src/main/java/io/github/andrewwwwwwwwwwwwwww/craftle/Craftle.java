package io.github.andrewwwwwwwwwwwwwww.craftle;

import io.github.andrewwwwwwwwwwwwwww.craftle.net.GuessPayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.GuessResultPayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.OpenGamePayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.PreviewPayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.PreviewResultPayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.server.GameManager;
import io.github.andrewwwwwwwwwwwwwww.craftle.server.RecipePool;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Craftle implements ModInitializer {
    public static final String MOD_ID = "craftle";
    public static final Logger LOGGER = LoggerFactory.getLogger("Craftle");

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(OpenGamePayload.TYPE, OpenGamePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GuessResultPayload.TYPE, GuessResultPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PreviewResultPayload.TYPE, PreviewResultPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GuessPayload.TYPE, GuessPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PreviewPayload.TYPE, PreviewPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(GuessPayload.TYPE, (payload, context) -> {
            MinecraftServer server = context.server();
            if (server != null) {
                server.execute(() -> GameManager.handleGuess(context.player(), payload));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(PreviewPayload.TYPE, (payload, context) -> {
            MinecraftServer server = context.server();
            if (server != null) {
                server.execute(() -> GameManager.handlePreview(context.player(), payload));
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("craftle")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            GameManager.openDaily(player);
                            return 1;
                        })
                        .then(Commands.literal("random")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    GameManager.openRandom(player, false);
                                    return 1;
                                })
                                .then(Commands.literal("new")
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            GameManager.openRandom(player, true);
                                            return 1;
                                        })))));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            GameManager.clear();
            RecipePool.rebuild(server);
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) ->
                RecipePool.rebuild(server));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> GameManager.clear());

        LOGGER.info("[Craftle] ready — /craftle for the daily puzzle");
    }
}
