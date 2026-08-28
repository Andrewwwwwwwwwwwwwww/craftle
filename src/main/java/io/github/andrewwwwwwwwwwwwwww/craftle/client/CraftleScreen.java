package io.github.andrewwwwwwwwwwwwwww.craftle.client;

import io.github.andrewwwwwwwwwwwwwww.craftle.ItemIds;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.CellState;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.CraftleGame;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.GameMode;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.GameStatus;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.GuessEvaluator;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.GridBytes;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.GuessPayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.GuessResultPayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.OpenGamePayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.PayloadChecks;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.PreviewPayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.PreviewResultPayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.StatsSnapshot;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Craftle board: active 3x3 crafting grid with a live output slot in the middle, the
 * ingredient palette below it, and up to nine previous guesses as color-coded mini grids
 * flanking the sides for cross-referencing. The latest guess stays on the main grid with
 * its feedback overlaid.
 */
public class CraftleScreen extends Screen {
    private static final int CELL = 18;
    private static final int MINI = 10;
    private static final int MINI_STEP = MINI + 1;
    private static final int MINI_GRID = 3 * MINI_STEP - 1;
    private static final int HISTORY_ROWS = 5;
    private static final int HISTORY_PITCH = MINI_GRID + 6;
    private static final int HISTORY_TOP = 24;
    private static final int FLANK = 8;
    private static final int CENTER_W = 168;
    private static final int PANEL_W = FLANK + MINI_GRID + 12 + CENTER_W + 12 + MINI_GRID + FLANK;
    private static final int PANEL_H = 224;

    private static final int COLOR_CORRECT = 0xFF43A047;
    private static final int COLOR_PRESENT = 0xFFDD9A28;
    private static final int COLOR_ABSENT = 0xFF5A5A5A;
    private static final int COLOR_SELECTION = 0xFFFFD24A;
    private static final int TEXT_DARK = 0xFF3F3F3F;
    private static final int TEXT_SOFT = 0xFF6A6A6A;
    private static final int TEXT_GREEN = 0xFF1E7A1E;
    private static final int TEXT_RED = 0xFFB02E26;

    private final GameMode mode;
    private final long epochDay;
    private final List<ItemStack> palette = new ArrayList<>();
    private final List<int[]> guesses = new ArrayList<>();
    private final List<CellState[]> results = new ArrayList<>();
    private GameStatus status;
    private StatsSnapshot stats;
    private ItemStack resultStack = ItemStack.EMPTY;

    private final int[] grid = new int[GuessEvaluator.GRID_SIZE];
    private CellState[] gridColors;
    private int selected = -1;
    private boolean showHelp;

    /** Index of the optimistically-shown guess awaiting its server feedback, or -1. */
    private int pendingIndex = -1;
    /** What the current grid would craft, mirrored from the server. */
    private ItemStack previewStack = ItemStack.EMPTY;
    private byte[] previewSentFor;

    private Button craftButton;
    private Button clearButton;

    private int panelLeft;
    private int panelTop;
    private int centerX;
    private int gridX;
    private int gridY;
    private int outX;
    private int outY;
    private int palX;
    private int palY;

    public CraftleScreen(OpenGamePayload payload) {
        super(Component.literal("Craftle"));
        this.mode = GameMode.byId(payload.mode());
        this.epochDay = payload.epochDay();
        this.status = GameStatus.byId(payload.status());
        this.stats = payload.stats();
        for (String id : payload.paletteIds()) {
            palette.add(new ItemStack(ItemIds.resolve(id)));
        }
        for (byte[] guess : payload.guesses()) {
            guesses.add(GridBytes.toGrid(guess));
        }
        for (byte[] result : payload.results()) {
            results.add(GridBytes.toResults(result));
        }
        if (!payload.resultItemId().isEmpty()) {
            resultStack = new ItemStack(ItemIds.resolve(payload.resultItemId()),
                    Math.max(1, payload.resultCount()));
        }

        Arrays.fill(grid, GuessEvaluator.NO_ITEM);
        if (status == GameStatus.LOST && payload.answer().length == GuessEvaluator.GRID_SIZE) {
            // Reveal the recipe arrangement on the main grid after a loss.
            System.arraycopy(GridBytes.toGrid(payload.answer()), 0, grid, 0, grid.length);
        } else if (!guesses.isEmpty()) {
            System.arraycopy(guesses.get(guesses.size() - 1), 0, grid, 0, grid.length);
            gridColors = results.get(results.size() - 1);
        }
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        panelLeft = (this.width - PANEL_W) / 2;
        panelTop = Math.max(4, (this.height - PANEL_H) / 2);
        centerX = panelLeft + PANEL_W / 2;
        int centerLeft = panelLeft + FLANK + MINI_GRID + 12;

        gridX = centerLeft + 31;
        gridY = panelTop + HISTORY_TOP;
        outX = gridX + 88;
        outY = gridY + 19;
        palX = centerLeft + 30;
        palY = panelTop + 98;

        int buttonY = panelTop + 158;
        int buttonsLeft = centerLeft + 4;
        craftButton = addRenderableWidget(Button.builder(Component.literal("Craft"), b -> submitGuess())
                .bounds(buttonsLeft, buttonY, 58, 20).build());
        clearButton = addRenderableWidget(Button.builder(Component.literal("Clear"), b -> clearGrid())
                .bounds(buttonsLeft + 62, buttonY, 46, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(buttonsLeft + 112, buttonY, 48, 20).build());
        addRenderableWidget(Button.builder(Component.literal("?"), b -> showHelp = !showHelp)
                .bounds(panelLeft + PANEL_W - 22, panelTop + 5, 16, 16).build());

        updateButtons();
        requestPreview();
    }

    private void updateButtons() {
        boolean canPlay = canEdit();
        craftButton.active = canPlay && !isGridEmpty();
        clearButton.active = canPlay && !isGridEmpty();
    }

    private boolean canEdit() {
        return status == GameStatus.IN_PROGRESS && pendingIndex < 0 && !showHelp;
    }

    private boolean isGridEmpty() {
        for (int cell : grid) {
            if (cell != GuessEvaluator.NO_ITEM) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ actions

    private void submitGuess() {
        if (!canEdit() || isGridEmpty()) {
            return;
        }
        // Show the attempt immediately; the colors fill in when the server answers.
        guesses.add(grid.clone());
        CellState[] blank = new CellState[GuessEvaluator.GRID_SIZE];
        Arrays.fill(blank, CellState.EMPTY);
        results.add(blank);
        pendingIndex = guesses.size() - 1;
        gridColors = null;
        updateButtons();
        ClientPlayNetworking.send(new GuessPayload(mode.id(), GridBytes.fromGrid(grid)));
    }

    private void clearGrid() {
        if (!canEdit()) {
            return;
        }
        Arrays.fill(grid, GuessEvaluator.NO_ITEM);
        gridColors = null;
        updateButtons();
        requestPreview();
    }

    /** Asks the server what the current arrangement crafts (skipped if unchanged). */
    private void requestPreview() {
        byte[] cells = GridBytes.fromGrid(grid);
        if (previewSentFor != null && Arrays.equals(previewSentFor, cells)) {
            return;
        }
        previewSentFor = cells;
        // Blank the slot until the authoritative answer lands, so it never advertises
        // what the *previous* arrangement crafted.
        previewStack = ItemStack.EMPTY;
        if (isGridEmpty()) {
            return;
        }
        ClientPlayNetworking.send(new PreviewPayload(cells));
    }

    /** Called from the network receiver with the crafted result for a previewed grid. */
    public void onPreviewResult(PreviewResultPayload payload) {
        if (!Arrays.equals(payload.cells(), GridBytes.fromGrid(grid))) {
            return; // stale reply for a grid we've already moved on from
        }
        previewStack = PayloadChecks.validPreview(payload)
                ? new ItemStack(ItemIds.resolve(payload.itemId()), payload.count())
                : ItemStack.EMPTY;
    }

    /** Called from the network receiver when the server answers a guess. */
    public void onGuessResult(GuessResultPayload payload) {
        if (payload.mode() != mode.id() || pendingIndex < 0) {
            return;
        }
        int index = pendingIndex;
        pendingIndex = -1;

        if (!PayloadChecks.validResult(payload, palette.size())
                || payload.colors().length != GuessEvaluator.GRID_SIZE) {
            // Rejected (or malformed): take the optimistic attempt back off the board.
            guesses.remove(index);
            results.remove(index);
            gridColors = null;
            updateButtons();
            return;
        }

        CellState[] colors = GridBytes.toResults(payload.colors());
        results.set(index, colors);
        gridColors = colors;
        status = GameStatus.byId(payload.status());
        stats = payload.stats();

        if (status.finished()) {
            if (!payload.resultItemId().isEmpty()) {
                resultStack = new ItemStack(ItemIds.resolve(payload.resultItemId()),
                        Math.max(1, payload.resultCount()));
            }
            if (status == GameStatus.LOST && payload.answer().length == GuessEvaluator.GRID_SIZE) {
                System.arraycopy(GridBytes.toGrid(payload.answer()), 0, grid, 0, grid.length);
                gridColors = null;
            }
            playSound(status == GameStatus.WON);
        }
        updateButtons();
    }

    private void playSound(boolean won) {
        if (won) {
            this.minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f));
        } else {
            this.minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0f));
        }
    }

    private void click() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (showHelp) {
            showHelp = false;
            updateButtons();
            return true;
        }
        if (super.mouseClicked(event, doubled)) {
            // Drop the focus ring the container just put on the clicked button — it has
            // to happen after dispatch, since the container re-focuses on the way out.
            clearFocus();
            updateButtons();
            return true;
        }
        int mx = (int) event.x();
        int my = (int) event.y();

        int paletteIndex = paletteIndexAt(mx, my);
        if (paletteIndex >= 0 && event.button() == 0 && canEdit()) {
            selected = (selected == paletteIndex) ? -1 : paletteIndex;
            click();
            return true;
        }

        int cell = gridCellAt(mx, my);
        if (cell >= 0 && canEdit()) {
            if (event.button() == 1) {
                if (grid[cell] != GuessEvaluator.NO_ITEM) {
                    grid[cell] = GuessEvaluator.NO_ITEM;
                    gridColors = null;
                    click();
                    updateButtons();
                    requestPreview();
                }
                return true;
            }
            if (event.button() == 0) {
                if (selected >= 0) {
                    grid[cell] = selected;
                    gridColors = null;
                    click();
                    updateButtons();
                    requestPreview();
                } else if (grid[cell] != GuessEvaluator.NO_ITEM) {
                    selected = grid[cell];
                    click();
                }
                return true;
            }
        }
        return false;
    }

    private int gridCellAt(int mx, int my) {
        if (mx < gridX || my < gridY) {
            return -1;
        }
        int col = (mx - gridX) / CELL;
        int row = (my - gridY) / CELL;
        if (col > 2 || row > 2) {
            return -1;
        }
        return row * 3 + col;
    }

    private int paletteIndexAt(int mx, int my) {
        if (mx < palX || my < palY) {
            return -1;
        }
        int col = (mx - palX) / CELL;
        int row = (my - palY) / CELL;
        if (col > 5 || row > 2) {
            return -1;
        }
        int index = row * 6 + col;
        return index < palette.size() ? index : -1;
    }

    // ------------------------------------------------------------------ rendering

    /** Widest a help-overlay line may be before it leaves the dark scrim. */
    private static final int HELP_W = PANEL_W - 44;

    private void text(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.text(this.font, fit(s, HELP_W), x, y, color, false);
    }

    /**
     * Centered on the panel but never wider than the center column, so a long line can't
     * reach into the history grids flanking either side.
     */
    private void centered(GuiGraphicsExtractor g, String s, int cx, int y, int color) {
        centered(g, s, cx, y, color, CENTER_W);
    }

    private void centered(GuiGraphicsExtractor g, String s, int cx, int y, int color, int maxWidth) {
        String shown = fit(s, maxWidth);
        g.text(this.font, shown, cx - this.font.width(shown) / 2, y, color, false);
    }

    /** Truncates with an ellipsis rather than letting text escape its box. */
    private String fit(String s, int maxWidth) {
        if (this.font.width(s) <= maxWidth) {
            return s;
        }
        return this.font.plainSubstrByWidth(s, maxWidth - this.font.width("...")) + "...";
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        Chrome.panel(g, panelLeft, panelTop, PANEL_W, PANEL_H);
        for (int i = 0; i < GuessEvaluator.GRID_SIZE; i++) {
            Chrome.slot(g, gridX + (i % 3) * CELL + 1, gridY + (i / 3) * CELL + 1);
        }
        Chrome.slot(g, outX + 1, outY + 1);
        for (int i = 0; i < palette.size(); i++) {
            Chrome.slot(g, palX + (i % 6) * CELL + 1, palY + (i / 6) * CELL + 1);
        }
        Chrome.arrow(g, gridX + 60, outY + 1, 22);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        centered(g, subtitle(), centerX, panelTop + 8, TEXT_DARK);

        drawMainGrid(g, mouseX, mouseY);
        drawOutput(g, mouseX, mouseY);
        drawPalette(g, mouseX, mouseY);
        drawHistory(g, mouseX, mouseY);
        drawStatusLines(g);

        if (showHelp) {
            drawHelp(g);
        }
    }

    private String subtitle() {
        if (mode == GameMode.DAILY) {
            return "Daily · " + LocalDate.ofEpochDay(epochDay);
        }
        return "Random Practice";
    }

    private void drawMainGrid(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        for (int i = 0; i < GuessEvaluator.GRID_SIZE; i++) {
            int x = gridX + (i % 3) * CELL + 1;
            int y = gridY + (i / 3) * CELL + 1;
            if (gridColors != null && gridColors[i] != CellState.EMPTY) {
                g.fill(x, y, x + 16, y + 16, overlayColor(gridColors[i]));
            }
            if (grid[i] != GuessEvaluator.NO_ITEM) {
                g.item(palette.get(grid[i]), x, y);
            }
        }
        int hovered = gridCellAt(mouseX, mouseY);
        if (hovered >= 0) {
            int x = gridX + (hovered % 3) * CELL + 1;
            int y = gridY + (hovered / 3) * CELL + 1;
            g.fill(x, y, x + 16, y + 16, 0x66FFFFFF);
            if (grid[hovered] != GuessEvaluator.NO_ITEM && !showHelp) {
                g.setTooltipForNextFrame(this.font, palette.get(grid[hovered]), mouseX, mouseY);
            }
        }
    }

    /** The output slot mirrors a crafting table: it shows what the current grid makes. */
    private void drawOutput(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = outX + 1;
        int y = outY + 1;
        ItemStack shown = status.finished() && !resultStack.isEmpty() ? resultStack : previewStack;
        if (!shown.isEmpty()) {
            g.item(shown, x, y);
            g.itemDecorations(this.font, shown, x, y);
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16 && !showHelp) {
                g.setTooltipForNextFrame(this.font, shown, mouseX, mouseY);
            }
        } else {
            centered(g, "?", x + 8, y + 4, TEXT_SOFT);
        }
    }

    private void drawPalette(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        for (int i = 0; i < palette.size(); i++) {
            int x = palX + (i % 6) * CELL + 1;
            int y = palY + (i / 6) * CELL + 1;
            if (i == selected) {
                Chrome.selection(g, x, y, COLOR_SELECTION);
            }
            g.item(palette.get(i), x, y);
        }
        int hovered = paletteIndexAt(mouseX, mouseY);
        if (hovered >= 0) {
            int x = palX + (hovered % 6) * CELL + 1;
            int y = palY + (hovered / 6) * CELL + 1;
            g.fill(x, y, x + 16, y + 16, 0x66FFFFFF);
            if (!showHelp) {
                g.setTooltipForNextFrame(this.font, palette.get(hovered), mouseX, mouseY);
            }
        }
    }

    /**
     * Previous guesses as mini grids: up to five down the left flank, four more down the
     * right, newest last. A guess is only left out when the main grid is still showing it
     * (right after its result, or while its result is in flight) — the moment the player
     * starts editing, it moves into the flanks so its feedback is never lost.
     */
    private void drawHistory(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        boolean latestOnGrid = gridColors != null || pendingIndex >= 0;
        int total = latestOnGrid ? guesses.size() - 1 : guesses.size();
        int shown = Math.min(total, CraftleGame.MAX_GUESSES - 1);
        int first = Math.max(0, total - shown);

        for (int n = 0; n < shown; n++) {
            boolean rightSide = n >= HISTORY_ROWS;
            int baseX = rightSide ? panelLeft + PANEL_W - FLANK - MINI_GRID : panelLeft + FLANK;
            int baseY = panelTop + HISTORY_TOP + (n % HISTORY_ROWS) * HISTORY_PITCH;

            int[] guess = guesses.get(first + n);
            CellState[] colors = results.get(first + n);
            for (int i = 0; i < GuessEvaluator.GRID_SIZE; i++) {
                int x = baseX + (i % 3) * MINI_STEP;
                int y = baseY + (i / 3) * MINI_STEP;
                int fill = colors[i] == CellState.EMPTY ? Chrome.SLOT_FILL : overlayColor(colors[i]);
                Chrome.miniCell(g, x, y, MINI, fill);
                if (guess[i] != GuessEvaluator.NO_ITEM) {
                    var pose = g.pose();
                    pose.pushMatrix();
                    pose.translate(x - 1, y - 1);
                    pose.scale(0.75f, 0.75f);
                    g.item(palette.get(guess[i]), 0, 0);
                    pose.popMatrix();
                }
                if (!showHelp && mouseX >= x && mouseX < x + MINI && mouseY >= y && mouseY < y + MINI
                        && guess[i] != GuessEvaluator.NO_ITEM) {
                    g.setTooltipForNextFrame(this.font, palette.get(guess[i]), mouseX, mouseY);
                }
            }
        }
    }

    private void drawStatusLines(GuiGraphicsExtractor g) {
        int line1 = panelTop + 184;
        int line2 = panelTop + 195;
        int line3 = panelTop + 206;

        if (pendingIndex >= 0) {
            centered(g, "Crafting...", centerX, line1, TEXT_SOFT);
            return;
        }

        switch (status) {
            case IN_PROGRESS -> {
                centered(g, "Guess " + (guesses.size() + 1) + " of " + CraftleGame.MAX_GUESSES,
                        centerX, line1, TEXT_SOFT);
                // Keep every centered line under ~190px: wider than that and it reaches
                // into the history flanks either side.
                centered(g, selected >= 0 ? "Click to place · right-click clears"
                        : "Pick an ingredient below", centerX, line2, TEXT_SOFT);
            }
            case WON -> {
                centered(g, "Solved in " + guesses.size() + " of " + CraftleGame.MAX_GUESSES + "!",
                        centerX, line1, TEXT_GREEN);
                centered(g, resultName(), centerX, line2, TEXT_DARK);
            }
            case LOST -> {
                centered(g, "Out of guesses!", centerX, line1, TEXT_RED);
                centered(g, resultName(), centerX, line2, TEXT_DARK);
            }
        }

        if (status.finished() && mode == GameMode.DAILY) {
            centered(g, "Streak " + capped(stats.currentStreak()) + " · Best " + capped(stats.maxStreak())
                            + " · Won " + capped(stats.wins()) + "/" + capped(stats.played()),
                    centerX, line3, TEXT_SOFT);
        }
    }

    /** Keeps the stats line narrow enough to stay clear of the history flanks. */
    private static String capped(int value) {
        return value > 999 ? "999+" : Integer.toString(value);
    }

    private String resultName() {
        if (resultStack.isEmpty()) {
            return "";
        }
        String name = resultStack.getHoverName().getString();
        return resultStack.getCount() > 1 ? name + " x" + resultStack.getCount() : name;
    }

    private void drawHelp(GuiGraphicsExtractor g) {
        g.fill(panelLeft + 4, panelTop + 4, panelLeft + PANEL_W - 4, panelTop + PANEL_H - 4, 0xF5202020);
        int x = panelLeft + 20;
        int y = panelTop + 16;
        centered(g, "How to Play", centerX, y, 0xFFFFFFFF, HELP_W);
        y += 18;
        // Every line must measure under ~248px to stay inside the dark scrim.
        String[] lines = {
                "Guess the hidden recipe in " + CraftleGame.MAX_GUESSES + " tries.",
                "Pick an ingredient, then click the grid",
                "to place it. Right-click clears a cell.",
                "Press Craft to submit your guess.",
                "The output slot shows what you'd craft.",
        };
        for (String line : lines) {
            text(g, line, x, y, 0xFFDDDDDD);
            y += 12;
        }
        y += 6;
        y = legendLine(g, x, y, COLOR_CORRECT, "Right ingredient, in the right cell");
        y = legendLine(g, x, y, COLOR_PRESENT, "In the recipe, but somewhere else");
        y = legendLine(g, x, y, COLOR_ABSENT, "Not in the recipe");
        y += 6;
        String[] tail = {
                "Empty cells give no hints, and recipes are",
                "anchored to the top-left of the grid.",
                "The daily resets at midnight UTC and is the",
                "same puzzle for everyone. /craftle random",
                "deals endless practice puzzles.",
        };
        for (String line : tail) {
            text(g, line, x, y, 0xFFDDDDDD);
            y += 12;
        }
        centered(g, "Click anywhere to close", centerX, panelTop + PANEL_H - 18, 0xFF9E9E9E, HELP_W);
    }

    private int legendLine(GuiGraphicsExtractor g, int x, int y, int color, String label) {
        g.fill(x, y, x + 9, y + 9, color);
        g.text(this.font, fit(label, HELP_W - 14), x + 14, y + 1, 0xFFDDDDDD, false);
        return y + 12;
    }

    private static int overlayColor(CellState state) {
        return switch (state) {
            case CORRECT -> COLOR_CORRECT;
            case PRESENT -> COLOR_PRESENT;
            default -> COLOR_ABSENT;
        };
    }

    @Override
    public boolean isPauseScreen() {
        // Never pause: in singleplayer the integrated server must keep ticking to
        // answer guess and preview payloads.
        return false;
    }
}
