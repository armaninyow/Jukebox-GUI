package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class JukeboxThemeUtils {

    private JukeboxThemeUtils() {}

    public static final String[] DISC_NAMES = {
        "11", "13", "5", "blocks", "bounce", "cat", "chirp",
        "creator_music_box", "creator", "far", "lava_chicken",
        "mall", "mellohi", "otherside", "pigstep", "precipice",
        "relic", "stal", "strad", "tears", "wait", "ward"
    };

    public static final Identifier TEX_PAUSE     = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/pause_button.png");
    public static final Identifier TEX_PAUSE_HOV = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/pause_button_highlighted.png");
    public static final Identifier TEX_PLAY      = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/play_button.png");
    public static final Identifier TEX_PLAY_HOV  = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/play_button_highlighted.png");
    public static final Identifier TEX_NO_DISC   = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/no_disc.png");

    public static final int THEME_BTN_X = 161;
    public static final int THEME_BTN_Y = 6;
    public static final int THEME_BTN_SIZE = 8;

    public static final int BTN_SIZE = 14;
    public static final int TITLE_COLOR = 0xFF000000;
    public static final int CLOCK_COLOR = 0xFF3F3F3F;

    private static final long RAINBOW_CYCLE_MS = 2500L;

    public static Identifier getButtonTexture(ItemStack disc, boolean hovered, boolean paused, boolean soundActive) {
        if (disc.isEmpty())  return TEX_NO_DISC;
        if (paused)          return hovered ? TEX_PLAY_HOV  : TEX_PLAY;
        if (soundActive)     return hovered ? TEX_PAUSE_HOV : TEX_PAUSE;
        return hovered ? TEX_PLAY_HOV : TEX_PLAY;
    }

    public static boolean isBlinkVisible() {
        return (System.currentTimeMillis() / 1000L) % 2 == 0;
    }

    public static String formatTime(int totalSec) {
        if (totalSec < 0) return "xx:xx";
        return String.format("%02d:%02d", totalSec / 60, totalSec % 60);
    }

    public static int getRainbowColor() {
        float hue = (System.currentTimeMillis() % RAINBOW_CYCLE_MS) / (float) RAINBOW_CYCLE_MS;
        return hsbToRgb(hue) | 0xFF000000;
    }

    public static void drawRainbowText(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
        if (text.isEmpty()) return;
        graphics.text(font, text, x, y, getRainbowColor(), false);
    }

    public static void renderThemeButton(GuiGraphicsExtractor graphics, int gx, int gy, Identifier icon) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon,
            gx + THEME_BTN_X, gy + THEME_BTN_Y,
            0f, 0f, THEME_BTN_SIZE, THEME_BTN_SIZE,
            THEME_BTN_SIZE, THEME_BTN_SIZE);
    }

    public static String[] splitSongTitle(String full) {
        int idx = full.indexOf(" - ");
        if (idx >= 0) {
            String artist = full.substring(0, idx);
            String title  = full.substring(idx + 3);
            return new String[]{ title, artist };
        }
        return new String[]{ full, "" };
    }

    private static int hsbToRgb(float hue) {
        float h = hue * 6f;
        int sector = (int) h;
        float frac = h - sector;
        float q = 1f - frac;
        float r, g, b;
        switch (sector % 6) {
            case 0 -> { r = 1f;   g = frac; b = 0f;   }
            case 1 -> { r = q;    g = 1f;   b = 0f;   }
            case 2 -> { r = 0f;   g = 1f;   b = frac; }
            case 3 -> { r = 0f;   g = q;    b = 1f;   }
            case 4 -> { r = frac; g = 0f;   b = 1f;   }
            default-> { r = 1f;   g = 0f;   b = q;    }
        }
        return ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }
}