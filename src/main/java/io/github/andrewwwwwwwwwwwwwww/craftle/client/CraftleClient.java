package io.github.andrewwwwwwwwwwwwwww.craftle.client;

import io.github.andrewwwwwwwwwwwwwww.craftle.Craftle;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.GuessResultPayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.OpenGamePayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.PayloadChecks;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.PreviewResultPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class CraftleClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OpenGamePayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (!PayloadChecks.validOpen(payload)) {
                        Craftle.LOGGER.warn("[Craftle] ignoring malformed game payload from server");
                        return;
                    }
                    context.client().setScreenAndShow(new CraftleScreen(payload));
                }));

        ClientPlayNetworking.registerGlobalReceiver(GuessResultPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (context.client().gui.screen() instanceof CraftleScreen screen) {
                        screen.onGuessResult(payload);
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(PreviewResultPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (context.client().gui.screen() instanceof CraftleScreen screen) {
                        screen.onPreviewResult(payload);
                    }
                }));
    }
}
