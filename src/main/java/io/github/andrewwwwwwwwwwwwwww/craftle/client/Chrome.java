package io.github.andrewwwwwwwwwwwwwww.craftle.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Draws Minecraft-style GUI chrome with plain fills using vanilla's exact container
 * colours and geometry (cut corners, bevels), so the screen looks native without
 * shipping textures.
 */
final class Chrome {
    private Chrome() {
    }

    static final int BODY = 0xFFC6C6C6;
    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int DARK = 0xFF555555;
    static final int SLOT_SHADOW = 0xFF373737;
    static final int SLOT_FILL = 0xFF8B8B8B;

    /** A raised window panel with vanilla's cut (rounded) corners. */
    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 2, y, x + w - 2, y + h, BODY);
        g.fill(x, y + 2, x + w, y + h - 2, BODY);

        g.fill(x + 2, y + 1, x + w - 2, y + 3, WHITE);
        g.fill(x + 1, y + 2, x + 3, y + h - 2, WHITE);
        g.fill(x + 2, y + h - 3, x + w - 2, y + h - 1, DARK);
        g.fill(x + w - 3, y + 2, x + w - 1, y + h - 2, DARK);

        g.fill(x + 2, y, x + w - 2, y + 1, BLACK);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, BLACK);
        g.fill(x, y + 2, x + 1, y + h - 2, BLACK);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, BLACK);

        g.fill(x + 1, y + 1, x + 2, y + 2, BLACK);
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2, BLACK);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, BLACK);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, BLACK);
    }

    /** A sunken 16x16 inventory slot (item area top-left at x, y). */
    static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_SHADOW);
        g.fill(x, y, x + 17, y + 17, WHITE);
        g.fill(x, y, x + 16, y + 16, SLOT_FILL);
    }

    /** A sunken mini cell of arbitrary size (used by the guess-history grids). */
    static void miniCell(GuiGraphicsExtractor g, int x, int y, int size, int fillColor) {
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, SLOT_SHADOW);
        g.fill(x, y, x + size, y + size, fillColor);
    }

    /** A 1px border drawn around a 16x16 slot to mark the selected ingredient. */
    static void selection(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x - 2, y - 2, x + 18, y - 1, color);
        g.fill(x - 2, y + 17, x + 18, y + 18, color);
        g.fill(x - 2, y - 1, x - 1, y + 17, color);
        g.fill(x + 17, y - 1, x + 18, y + 17, color);
    }

    /** A rightward crafting arrow (like the vanilla crafting table's). */
    static void arrow(GuiGraphicsExtractor g, int x, int y, int length) {
        int shadow = 0xFF8B8B8B;
        g.fill(x, y + 5, x + length - 6, y + 9, shadow);
        for (int step = 0; step < 7; step++) {
            g.fill(x + length - 6 + step, y + step, x + length - 5 + step, y + 14 - step, shadow);
        }
    }
}
