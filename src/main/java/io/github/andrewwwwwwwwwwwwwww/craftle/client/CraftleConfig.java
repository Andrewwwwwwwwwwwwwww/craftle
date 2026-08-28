package io.github.andrewwwwwwwwwwwwwww.craftle.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.andrewwwwwwwwwwwwwww.craftle.Craftle;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Client-side preferences, persisted in {@code config/craftle.json}. */
public final class CraftleConfig {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("craftle.json");

    /** Swaps the green/orange feedback colors for a colorblind-friendly blue/orange pair. */
    public static boolean highContrast;

    private CraftleConfig() {
    }

    public static void load() {
        try {
            if (!Files.exists(PATH)) {
                return;
            }
            JsonObject json = JsonParser.parseString(Files.readString(PATH)).getAsJsonObject();
            if (json.has("highContrast")) {
                highContrast = json.get("highContrast").getAsBoolean();
            }
        } catch (Exception e) {
            Craftle.LOGGER.warn("[Craftle] could not read {} — using defaults", PATH, e);
        }
    }

    public static void save() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("highContrast", highContrast);
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, json.toString());
        } catch (Exception e) {
            Craftle.LOGGER.warn("[Craftle] could not write {}", PATH, e);
        }
    }
}
