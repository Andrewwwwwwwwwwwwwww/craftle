package io.github.andrewwwwwwwwwwwwwww.craftle.client;

import io.github.andrewwwwwwwwwwwwwww.craftle.ItemIds;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.CellState;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.GameMode;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.GameStatus;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.GuessEvaluator;
import io.github.andrewwwwwwwwwwwwwww.craftle.game.CraftleGame;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.GridBytes;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.GuessPayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.GuessResultPayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.OpenGamePayload;
import io.github.andrewwwwwwwwwwwwwww.craftle.net.PayloadChecks;
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
import java.util.List;

/**
 * The Craftle board: active 3x3 crafting grid + output in the middle, the 18-item
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
    private static final int CENTER_W = 132;
    private static final int PANEL_W = 8 + MINI_GRID + 10 + CENTER_W + 10 + MINI_GRID + 8;
    private static final int PANEL_H = 210;

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
    private boolean pending;
    private boolean showHelp;

    private Button craftButton;
    private Button clearButton;

    private int panelLeft;
    private int panelTop;
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

        java.util.Arrays.fill(grid, GuessEvaluator.NO_ITEM);
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
        int centerLeft = panelLeft + 8 + MINI_GRID + 10;
        gridX = centerLeft + (CENTER_W - 98) / 2;
        gridY = panelTop + 24;
        outX = gridX + 80;
        outY = gridY + CELL + 1;
        palX = centerLeft + (CENTER_W - 6 * CELL) / 2;
        palY = gridY + 70;

        int buttonY = panelTop + 156;
        craftButton = addRenderableWidget(Button.builder(Component.literal("Craft"), b -> submitGuess())
                .bounds(centerLeft + (CENTER_W - 124) / 2, buttonY, 70, 20).build());
        clearButton = addRenderableWidget(Button.builder(Component.literal("Clear"), b -> clearGrid())
                .bounds(centerLeft + (CENTER_W - 124) / 2 + 74, buttonY, 50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("?"), b -> showHelp = !showHelp)
                .bounds(panelLeft + PANEL_W - 24, panelTop + 6, 16, 16).build());
        updateButtons();
    }

    private void updateButtons() {
        boolean canPlay = status == GameStatus.IN_PROGRESS && !pending;
        craftButton.active = canPlay && !isGridEmpty();
        clearButton.active = canPlay;
    }

    private boolean canEdit() {
        return status == GameStatus.IN_PROGRESS && !pending && !showHelp;
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
        pending = true;
        updateButtons();
        ClientPlayNetworking.send(new GuessPayload(mode.id(), GridBytes.fromGrid(grid)));
    }

    private void clearGrid() {
        if (!canEdit()) {
            return;
        }
        java.util.Arrays.fill(grid, GuessEvaluator.NO_ITEM);
        gridColors = null;
        updateButtons();
    }

    /** Called from the network receiver when the server answers a guess. */
    public void onGuessResult(GuessResultPayload payload) {
        if (payload.mode() != mode.id()) {
            return;
        }
        pending = false;
        if (!PayloadChecks.validResult(payload, palette.size())
                || payload.colors().length != GuessEvaluator.GRID_SIZE) {
            updateButtons(); // rejected (or malformed) — just unlock; chat explains rejections
            return;
        }
        CellState[] colors = GridBytes.toResults(payload.colors());
        guesses.add(grid.clone());
        results.add(colors);
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
            return true;
        }
        if (super.mouseClicked(event, doubled)) {
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
                }
                return true;
            }
            if (event.button() == 0) {
                if (selected >= 0) {
                    grid[cell] = selected;
                    gridColors = null;
                    click();
                    updateButtons();
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
        int col = (mx - gridX) / CELL;
        int row = (my - gridY) / CELL;
        if (mx < gridX || my < gridY || col > 2 || row > 2 || col < 0 || row < 0) {
            return -1;
        }
        return row * 3 + col;
    }

    private int paletteIndexAt(int mx, int my) {
        int col = (mx - palX) / CELL;
        int row = (my - palY) / CELL;
        if (mx < palX || my < palY || col > 5 || row > 2 || col < 0 || row < 0) {
            return -1;
        }
        int index = row * 6 + col;
        return index < palette.size() ? index : -1;
    }

    // ------------------------------------------------------------------ rendering

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
        Chrome.arrow(g, gridX + 58, gridY + CELL + 2, 20);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        g.centeredText(this.font, subtitle(), panelLeft + PANEL_W / 2, panelTop + 8, TEXT_DARK);

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
            return "Craftle — Daily " + LocalDate.ofEpochDay(epochDay);
        }
        return "Craftle — Random Practice";
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

    private void drawOutput(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = outX + 1;
        int y = outY + 1;
        if (!resultStack.isEmpty() && status.finished()) {
            g.item(resultStack, x, y);
            g.itemDecorations(this.font, resultStack, x, y);
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16 && !showHelp) {
                g.setTooltipForNextFrame(this.font, resultStack, mouseX, mouseY);
            }
        } else {
            g.centeredText(this.font, "?", x + 8, y + 4, TEXT_SOFT);
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
     * Previous guesses (all but the latest, which sits on the main grid) as mini grids:
     * up to five down the left flank, four more down the right.
     */
    private void drawHistory(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int shown = Math.min(guesses.size() - 1, CraftleGame.MAX_GUESSES - 1);
        for (int n = 0; n < shown; n++) {
            boolean rightSide = n >= HISTORY_ROWS;
            int baseX = rightSide ? panelLeft + PANEL_W - 8 - MINI_GRID : panelLeft + 8;
            int baseY = panelTop + 24 + (n % HISTORY_ROWS) * (MINI_GRID + 6);

            int[] guess = guesses.get(n);
            CellState[] colors = results.get(n);
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
        int centerX = panelLeft + PANEL_W / 2;
        int counterY = gridY + 58;
        switch (status) {
            case IN_PROGRESS -> g.centeredText(this.font,
                    "Guess " + (guesses.size() + 1) + "/" + CraftleGame.MAX_GUESSES,
                    centerX, counterY, TEXT_SOFT);
            case WON -> g.centeredText(this.font,
                    "Solved in " + guesses.size() + "/" + CraftleGame.MAX_GUESSES + "!",
                    centerX, counterY, TEXT_GREEN);
            case LOST -> g.centeredText(this.font,
                    "Out of guesses — this was the recipe.",
                    centerX, counterY, TEXT_RED);
        }
        if (status.finished() && mode == GameMode.DAILY) {
            // Two short lines so the text stays inside the center column, clear of the
            // flanking history minis.
            g.centeredText(this.font,
                    "Played " + stats.played() + " · Won " + stats.wins(),
                    centerX, panelTop + 182, TEXT_SOFT);
            g.centeredText(this.font,
                    "Streak " + stats.currentStreak() + " · Best " + stats.maxStreak(),
                    centerX, panelTop + 193, TEXT_SOFT);
        } else if (pending) {
            g.centeredText(this.font, "Crafting...", centerX, panelTop + 182, TEXT_SOFT);
        }
    }

    private void drawHelp(GuiGraphicsExtractor g) {
        g.fill(panelLeft + 4, panelTop + 4, panelLeft + PANEL_W - 4, panelTop + PANEL_H - 4, 0xF5202020);
        int x = panelLeft + 16;
        int y = panelTop + 14;
        g.centeredText(this.font, "How to Play", panelLeft + PANEL_W / 2, y, 0xFFFFFFFF);
        y += 16;
        String[] lines = {
                "Guess the hidden crafting recipe.",
                "You have " + CraftleGame.MAX_GUESSES + " tries.",
                "Pick an ingredient, click the grid",
                "to place it. Right-click clears.",
                "Press Craft to submit your guess.",
        };
        for (String line : lines) {
            g.text(this.font, line, x, y, 0xFFDDDDDD, false);
            y += 12;
        }
        y += 4;
        y = legendLine(g, x, y, COLOR_CORRECT, "Right ingredient, right cell");
        y = legendLine(g, x, y, COLOR_PRESENT, "In the recipe, but elsewhere");
        y = legendLine(g, x, y, COLOR_ABSENT, "Not in the recipe");
        y += 4;
        String[] tail = {
                "Empty cells give no hints.",
                "Recipes anchor to the top-left.",
                "The daily resets at midnight UTC",
                "and is the same for everyone.",
                "/craftle random = practice.",
        };
        for (String line : tail) {
            g.text(this.font, line, x, y, 0xFFDDDDDD, false);
            y += 12;
        }
        g.centeredText(this.font, "Click anywhere to close", panelLeft + PANEL_W / 2,
                panelTop + PANEL_H - 18, 0xFF9E9E9E);
    }

    private int legendLine(GuiGraphicsExtractor g, int x, int y, int color, String label) {
        g.fill(x, y, x + 9, y + 9, color);
        g.text(this.font, label, x + 14, y + 1, 0xFFDDDDDD, false);
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
        // answer guess payloads.
        return false;
    }
}
