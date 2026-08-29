package io.github.andrewwwwwwwwwwwwwww.craftle.server;

import io.github.andrewwwwwwwwwwwwwww.craftle.Craftle;
import io.github.andrewwwwwwwwwwwwwww.craftle.ItemIds;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.CellState;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.DailyPicker;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.GameMode;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.GameStatus;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.GuessEvaluator;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.CraftleGame;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.Palette;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.PuzzleRecipe;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.GridBytes;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.GuessPayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.GuessResultPayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.OpenGamePayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.PreviewPayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.PreviewResultPayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.StatsSnapshot;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Server-side game orchestration. The server is the sole authority: it picks puzzles,
 * holds the answers, validates guesses, and never tells the client the secret recipe
 * until the game is over.
 *
 * <p>Everything here runs on the server thread (commands run there; the payload receiver
 * hops via {@code server.execute}).</p>
 */
public final class GameManager {
    private GameManager() {
    }

    /**
     * A join sits in here for a moment before the nudge is sent: the client's channel
     * list arrives just after login, so asking straight away would wrongly conclude that a
     * modded client can't play — and it reads better after the world has finished loading.
     */
    private static final int NOTICE_DELAY_TICKS = 60;

    private static final Random RNG = new Random();
    private static final Map<UUID, Integer> pendingNotices = new HashMap<>();
    private static final Map<UUID, CraftleGame> liveDaily = new HashMap<>();
    private static final Map<UUID, CraftleGame> liveRandom = new HashMap<>();

    /** Dropped between servers/worlds (singleplayer world switches share the JVM). */
    public static void clear() {
        liveDaily.clear();
        liveRandom.clear();
        pendingNotices.clear();
    }

    // ------------------------------------------------------------------ daily nudge

    public static void onJoin(ServerPlayer player) {
        pendingNotices.put(player.getUUID(), NOTICE_DELAY_TICKS);
    }

    public static void onLeave(ServerPlayer player) {
        UUID id = player.getUUID();
        pendingNotices.remove(id);
        // Their games are reloaded from the save on reconnect, so don't hold them here.
        liveDaily.remove(id);
        liveRandom.remove(id);
    }

    /** Counts down queued joins and tells each player once that today's puzzle is waiting. */
    public static void tick(MinecraftServer server) {
        if (pendingNotices.isEmpty()) {
            return;
        }
        List<UUID> due = null;
        for (Map.Entry<UUID, Integer> entry : pendingNotices.entrySet()) {
            int ticksLeft = entry.getValue() - 1;
            entry.setValue(ticksLeft);
            if (ticksLeft <= 0) {
                if (due == null) {
                    due = new ArrayList<>();
                }
                due.add(entry.getKey());
            }
        }
        if (due == null) {
            return;
        }
        for (UUID id : due) {
            pendingNotices.remove(id);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                notifyNewDaily(player, server);
            }
        }
    }

    private static void notifyNewDaily(ServerPlayer player, MinecraftServer server) {
        if (RecipePool.pool().isEmpty()
                || !ServerPlayNetworking.canSend(player, OpenGamePayload.TYPE)) {
            return; // nothing to play, or this client can't open the board anyway
        }
        long today = DailyPicker.todayUtc();
        CraftleState state = CraftleState.get(server);
        UUID id = player.getUUID();
        PlayerRecord record = state.record(id);
        if (record.lastNotifiedDay() == today) {
            return; // already told them about this one
        }
        state.put(id, record.withNotifiedDay(today));
        if (!record.dailyUnstarted(today)) {
            return; // they're already on it
        }

        MutableComponent command = Component.literal("/craftle")
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.RunCommand("craftle"))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("Play today's Craftle"))));
        player.sendSystemMessage(Component.literal("A new Craftle is ready — ")
                .withStyle(ChatFormatting.GOLD)
                .append(command)
                .append(Component.literal(" to play.").withStyle(ChatFormatting.GOLD)));
    }

    // ------------------------------------------------------------------ opening

    public static void openDaily(ServerPlayer player) {
        if (!requireModdedClient(player) || !requirePool(player)) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        CraftleState state = CraftleState.get(server);
        UUID id = player.getUUID();
        long today = DailyPicker.todayUtc();

        PlayerRecord record = state.record(id);
        StoredGame stored = record.daily().orElse(null);
        if (stored == null || stored.day() != today) {
            List<PuzzleRecipe> pool = RecipePool.pool();
            PuzzleRecipe puzzle = pool.get(DailyPicker.pickIndex(today, pool.size()));
            stored = StoredGame.fresh(today, puzzle.recipeId(), puzzle.grid(),
                    puzzle.resultItemId(), puzzle.resultCount());
            record = record.withDaily(stored);
            state.put(id, record);
            liveDaily.remove(id);
        }

        CraftleGame game = stored.rehydrate(GameMode.DAILY);
        if (game == null) {
            // Corrupted save entry — replace it with a fresh game for today.
            List<PuzzleRecipe> pool = RecipePool.pool();
            PuzzleRecipe puzzle = pool.get(DailyPicker.pickIndex(today, pool.size()));
            stored = StoredGame.fresh(today, puzzle.recipeId(), puzzle.grid(),
                    puzzle.resultItemId(), puzzle.resultCount());
            state.put(id, record.withDaily(stored));
            game = stored.rehydrate(GameMode.DAILY);
        }
        liveDaily.put(id, game);
        ServerPlayNetworking.send(player, openPayload(game, today, state.record(id).stats().snapshot()));
    }

    public static void openRandom(ServerPlayer player, boolean forceNew) {
        if (!requireModdedClient(player) || !requirePool(player)) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        CraftleState state = CraftleState.get(server);
        UUID id = player.getUUID();

        PlayerRecord record = state.record(id);
        StoredGame stored = record.random().orElse(null);
        CraftleGame game = (stored == null || forceNew) ? null : stored.rehydrate(GameMode.RANDOM);
        if (game != null && !game.status().finished() && isTodaysDaily(game)) {
            // A practice game carried across midnight UTC can BE today's daily — replace
            // it rather than let it spoil the answer.
            player.sendSystemMessage(Component.literal(
                    "Your practice puzzle became today's daily — dealing a new one.")
                    .withStyle(ChatFormatting.AQUA));
            game = null;
        }
        if (game == null || game.status().finished()) {
            PuzzleRecipe puzzle = pickRandomPuzzle();
            stored = StoredGame.fresh(0L, puzzle.recipeId(), puzzle.grid(),
                    puzzle.resultItemId(), puzzle.resultCount());
            state.put(id, record.withRandom(stored));
            game = stored.rehydrate(GameMode.RANDOM);
        }
        liveRandom.put(id, game);
        ServerPlayNetworking.send(player, openPayload(game, 0L, state.record(id).stats().snapshot()));
    }

    /** Random practice must not spoil the daily, so today's answer is excluded. */
    private static PuzzleRecipe pickRandomPuzzle() {
        List<PuzzleRecipe> pool = RecipePool.pool();
        int dailyIndex = DailyPicker.pickIndex(DailyPicker.todayUtc(), pool.size());
        int index;
        do {
            index = RNG.nextInt(pool.size());
        } while (pool.size() > 1 && index == dailyIndex);
        return pool.get(index);
    }

    /** True when this (practice) game's answer is today's daily recipe. */
    private static boolean isTodaysDaily(CraftleGame game) {
        List<PuzzleRecipe> pool = RecipePool.pool();
        if (pool.isEmpty()) {
            return false;
        }
        PuzzleRecipe daily = pool.get(DailyPicker.pickIndex(DailyPicker.todayUtc(), pool.size()));
        return Arrays.equals(game.answer(), daily.grid());
    }

    // ------------------------------------------------------------------ guessing

    public static void handleGuess(ServerPlayer player, GuessPayload payload) {
        if (payload.mode() < 0 || payload.mode() >= GameMode.VALUES.length) {
            reject(player, payload.mode(), null); // echo the raw byte so the sender unlocks
            return;
        }
        GameMode mode = GameMode.byId(payload.mode());
        if (!GridBytes.isValidGrid(payload.cells(), Palette.SIZE)) {
            reject(player, mode.id(), null);
            return;
        }

        MinecraftServer server = player.level().getServer();
        CraftleState state = CraftleState.get(server);
        UUID id = player.getUUID();
        PlayerRecord record = state.record(id);

        if (mode == GameMode.DAILY) {
            handleDailyGuess(player, server, state, id, record, payload.cells());
        } else {
            handleRandomGuess(player, server, state, id, record, payload.cells());
        }
    }

    private static void handleDailyGuess(ServerPlayer player, MinecraftServer server, CraftleState state,
                                         UUID id, PlayerRecord record, byte[] cells) {
        long today = DailyPicker.todayUtc();
        StoredGame stored = record.daily().orElse(null);
        if (stored == null || stored.day() != today) {
            liveDaily.remove(id);
            reject(player, GameMode.DAILY.id(),
                    "A new daily puzzle is available — run /craftle to open it.");
            return;
        }
        CraftleGame game = liveDaily.computeIfAbsent(id, key -> stored.rehydrate(GameMode.DAILY));
        if (game == null) {
            liveDaily.remove(id);
            reject(player, GameMode.DAILY.id(), null);
            return;
        }

        int[] grid = GridBytes.toGrid(cells);
        CellState[] colors = game.submit(grid);
        if (colors == null) {
            reject(player, GameMode.DAILY.id(), null);
            return;
        }

        record = record.withDaily(stored.withGuess(grid));
        boolean finished = game.status().finished();
        if (finished) {
            boolean won = game.status() == GameStatus.WON;
            record = record.withStats(record.stats().afterDaily(won, today));
            broadcastDailyResult(server, player, won, game.guessCount());
        }
        state.put(id, record);
        ServerPlayNetworking.send(player, resultPayload(game, colors, record.stats().snapshot()));
    }

    private static void handleRandomGuess(ServerPlayer player, MinecraftServer server, CraftleState state,
                                          UUID id, PlayerRecord record, byte[] cells) {
        StoredGame stored = record.random().orElse(null);
        if (stored == null) {
            reject(player, GameMode.RANDOM.id(), "No practice game in progress — run /craftle random.");
            return;
        }
        CraftleGame game = liveRandom.computeIfAbsent(id, key -> stored.rehydrate(GameMode.RANDOM));
        if (game == null) {
            liveRandom.remove(id);
            reject(player, GameMode.RANDOM.id(), null);
            return;
        }
        if (!game.status().finished() && isTodaysDaily(game)) {
            // Crossed midnight UTC mid-game and the practice puzzle is now the daily.
            liveRandom.remove(id);
            state.put(id, record.withRandom(null));
            reject(player, GameMode.RANDOM.id(),
                    "Your practice puzzle became today's daily — run /craftle random for a new one.");
            return;
        }

        int[] grid = GridBytes.toGrid(cells);
        CellState[] colors = game.submit(grid);
        if (colors == null) {
            reject(player, GameMode.RANDOM.id(), null);
            return;
        }

        state.put(id, record.withRandom(stored.withGuess(grid)));
        if (game.status().finished()) {
            sendRandomResultMessage(player, game);
        }
        ServerPlayNetworking.send(player, resultPayload(game, colors, record.stats().snapshot()));
    }

    // ------------------------------------------------------------------ output preview

    /**
     * Answers "what would this arrangement craft?" so the client's output slot behaves
     * like a real crafting table. Uses only the public recipe registry — it reveals
     * nothing about the puzzle that a crafting table wouldn't.
     */
    public static void handlePreview(ServerPlayer player, PreviewPayload payload) {
        if (!GridBytes.isValidGrid(payload.cells(), Palette.SIZE)) {
            return;
        }
        List<ItemStack> stacks = new ArrayList<>(GuessEvaluator.GRID_SIZE);
        boolean any = false;
        for (byte cell : payload.cells()) {
            if (cell == GuessEvaluator.NO_ITEM) {
                stacks.add(ItemStack.EMPTY);
            } else {
                stacks.add(new ItemStack(ItemIds.resolve(Palette.ITEM_IDS.get(cell))));
                any = true;
            }
        }
        if (!any) {
            ServerPlayNetworking.send(player, new PreviewResultPayload(payload.cells(), "", 0));
            return;
        }

        ItemStack result = ItemStack.EMPTY;
        try {
            CraftingInput input = CraftingInput.of(3, 3, stacks);
            ServerLevel level = player.level();
            result = player.level().getServer().getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, input, level)
                    .map(holder -> holder.value().assemble(input))
                    .orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            Craftle.LOGGER.debug("[Craftle] preview lookup failed", e);
        }

        ServerPlayNetworking.send(player, result.isEmpty()
                ? new PreviewResultPayload(payload.cells(), "", 0)
                : new PreviewResultPayload(payload.cells(), ItemIds.idOf(result.getItem()), result.getCount()));
    }

    // ------------------------------------------------------------------ messaging

    private static void broadcastDailyResult(MinecraftServer server, ServerPlayer player, boolean won, int guessCount) {
        String name = player.getGameProfile().name();
        MutableComponent message;
        if (won) {
            message = Component.literal("★ ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .append(Component.literal(" solved today's Craftle in ").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(guessCount + "/" + CraftleGame.MAX_GUESSES)
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .append(Component.literal("!").withStyle(ChatFormatting.GREEN));
        } else {
            message = Component.literal(name).withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    .append(Component.literal(" couldn't solve today's Craftle — all ").withStyle(ChatFormatting.RED))
                    .append(Component.literal(CraftleGame.MAX_GUESSES + " guesses").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
                    .append(Component.literal(" used up.").withStyle(ChatFormatting.RED));
        }
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            online.sendSystemMessage(message);
        }
    }

    private static void sendRandomResultMessage(ServerPlayer player, CraftleGame game) {
        Component itemName = new ItemStack(ItemIds.resolve(game.resultItemId())).getHoverName();
        MutableComponent message;
        if (game.status() == GameStatus.WON) {
            message = Component.literal("Cracked it in " + game.guessCount() + "/" + CraftleGame.MAX_GUESSES
                            + " — it was ").withStyle(ChatFormatting.GREEN)
                    .append(itemName.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .append(Component.literal("!").withStyle(ChatFormatting.GREEN));
        } else {
            message = Component.literal("Out of guesses — it was ").withStyle(ChatFormatting.RED)
                    .append(itemName.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .append(Component.literal(".").withStyle(ChatFormatting.RED));
        }
        player.sendSystemMessage(message);
    }

    private static void reject(ServerPlayer player, byte modeByte, String chatMessage) {
        if (chatMessage != null) {
            player.sendSystemMessage(Component.literal(chatMessage).withStyle(ChatFormatting.RED));
        }
        ServerPlayNetworking.send(player, new GuessResultPayload(modeByte, new byte[0],
                GameStatus.IN_PROGRESS.id(), new byte[0], "", 0, StatsSnapshot.EMPTY));
    }

    // ------------------------------------------------------------------ payload assembly

    private static OpenGamePayload openPayload(CraftleGame game, long epochDay, StatsSnapshot stats) {
        List<byte[]> guesses = new ArrayList<>(game.guessCount());
        List<byte[]> results = new ArrayList<>(game.guessCount());
        for (int i = 0; i < game.guessCount(); i++) {
            guesses.add(GridBytes.fromGrid(game.guesses().get(i)));
            results.add(GridBytes.fromResults(game.results().get(i)));
        }
        boolean finished = game.status().finished();
        return new OpenGamePayload(game.mode().id(), Palette.ITEM_IDS, guesses, results,
                game.status().id(),
                finished ? GridBytes.fromGrid(game.answer()) : new byte[0],
                finished ? game.resultItemId() : "",
                finished ? game.resultCount() : 0,
                epochDay, stats);
    }

    private static GuessResultPayload resultPayload(CraftleGame game, CellState[] colors, StatsSnapshot stats) {
        boolean finished = game.status().finished();
        return new GuessResultPayload(game.mode().id(), GridBytes.fromResults(colors),
                game.status().id(),
                finished ? GridBytes.fromGrid(game.answer()) : new byte[0],
                finished ? game.resultItemId() : "",
                finished ? game.resultCount() : 0,
                stats);
    }

    // ------------------------------------------------------------------ guards

    private static boolean requireModdedClient(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, OpenGamePayload.TYPE)) {
            return true;
        }
        player.sendSystemMessage(Component.literal(
                "Craftle must be installed on your client to play.").withStyle(ChatFormatting.RED));
        return false;
    }

    private static boolean requirePool(ServerPlayer player) {
        if (!RecipePool.pool().isEmpty()) {
            return true;
        }
        player.sendSystemMessage(Component.literal(
                "No eligible recipes found on this server — Craftle can't build a puzzle.")
                .withStyle(ChatFormatting.RED));
        return false;
    }
}
