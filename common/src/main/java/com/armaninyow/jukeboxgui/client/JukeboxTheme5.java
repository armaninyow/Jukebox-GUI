package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

// "Spectrum visualizer" theme
public class JukeboxTheme5 implements JukeboxTheme {

    private static final Identifier CONTAINER_TEXTURE_5 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_5/jukebox_container_5.png");
    private static final Identifier TEX_THEME_5 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_5/theme_5.png");

    private static final int VIS_X = 8, VIS_Y = 16, VIS_W = 160, VIS_H = 30;
    private static final int FRONT_LAYER_H = 15;

    private static final int BAR_COUNT = 160;
    private static final int BAR_GAP = 0;
    private static final int SLOT_WIDTH = VIS_W / BAR_COUNT;
    private static final int BAR_WIDTH  = SLOT_WIDTH - BAR_GAP;

    private static final float MIN_BAR_VALUE = 0.04f;

    private static final int BAND_COUNT = 16;
    private static final float BAND_SPACING = (float) BAR_COUNT / BAND_COUNT;

    private static final float ATTACK_SPEED = 25f;
    private static final float DECAY_SPEED  = 8f;

    private static final int TITLE_X  = 8, TITLE_Y  = 48;
    private static final int ARTIST_X = 8, ARTIST_Y = 60;

    private static final int ELAPSED_X = 143, ELAPSED_Y = 48;
    private static final int TOTAL_X   = 143, TOTAL_Y   = 60;

    private static final int BTN_X = 105, BTN_Y = 51;

    private static final int DISC_X = 123, DISC_Y = 50, DISC_SIZE = 16;

    private final float[] smoothedLevel = new float[BAND_COUNT];
    private long lastSmoothTime = 0L;

    @Override public int id() { return 5; }
    @Override public Identifier toggleIcon() { return TEX_THEME_5; }
    @Override public int playPauseBtnX() { return BTN_X; }
    @Override public int playPauseBtnY() { return BTN_Y; }
    @Override public int playPauseBtnSize() { return JukeboxThemeUtils.BTN_SIZE; }
    @Override public boolean skipSlot(int slotIndex) { return slotIndex == 0; }

    @Override
    public boolean handleClick(JukeboxManagementScreen screen, MouseButtonEvent event) {
        int discAbsX = screen.getGuiLeft() + DISC_X;
        int discAbsY = screen.getGuiTop()  + DISC_Y;
        if (event.x() >= discAbsX && event.x() < discAbsX + DISC_SIZE
         && event.y() >= discAbsY && event.y() < discAbsY + DISC_SIZE) {
            screen.clickDiscSlot(event);
            return true;
        }
        return false;
    }

    @Override
    public void render(JukeboxManagementScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int gx = screen.getGuiLeft();
        int gy = screen.getGuiTop();

        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE_5,
            gx, gy, 0f, 0f, JukeboxManagementScreen.BG_WIDTH, JukeboxManagementScreen.BG_HEIGHT,
            JukeboxManagementScreen.BG_WIDTH, JukeboxManagementScreen.BG_HEIGHT);

        ItemStack disc = screen.getDiscStack();
        float totalSeconds = screen.totalSeconds;
        boolean playing = !screen.isClientPaused() && screen.timerStartTime > 0 && !screen.songFinished;

        int discAbsX = gx + DISC_X;
        int discAbsY = gy + DISC_Y;
        if (!disc.isEmpty()) {
            graphics.item(disc, discAbsX, discAbsY);
            graphics.itemDecorations(screen.getFont(), disc, discAbsX, discAbsY, null);
        }
        if (mouseX >= discAbsX && mouseX < discAbsX + DISC_SIZE
         && mouseY >= discAbsY && mouseY < discAbsY + DISC_SIZE) {
            graphics.fill(discAbsX, discAbsY, discAbsX + DISC_SIZE, discAbsY + DISC_SIZE, 0x80FFFFFF);
        }

        if (!disc.isEmpty()) {
            String discName = resolveDiscName(disc);
            float t = screen.getElapsedSeconds();
            renderSpectrum(graphics, gx, gy, discName, t, totalSeconds);
        }

        String songTitle = screen.songTitle;
        if (!songTitle.isEmpty()) {
            String[] parts = JukeboxThemeUtils.splitSongTitle(songTitle);
            String titleLine  = parts[0];
            String artistLine = parts[1];

            if (playing) {
                JukeboxThemeUtils.drawRainbowText(graphics, screen.getFont(), titleLine, gx + TITLE_X, gy + TITLE_Y);
            } else {
                graphics.text(screen.getFont(), titleLine, gx + TITLE_X, gy + TITLE_Y, JukeboxThemeUtils.TITLE_COLOR, false);
            }

            if (!artistLine.isEmpty()) {
                if (playing) {
                    JukeboxThemeUtils.drawRainbowText(graphics, screen.getFont(), artistLine, gx + ARTIST_X, gy + ARTIST_Y);
                } else {
                    graphics.text(screen.getFont(), artistLine, gx + ARTIST_X, gy + ARTIST_Y, JukeboxThemeUtils.TITLE_COLOR, false);
                }
            }
        } else {
            if (JukeboxThemeUtils.isBlinkVisible()) {
                graphics.text(screen.getFont(), "INSERT DISC", gx + TITLE_X, gy + TITLE_Y, JukeboxThemeUtils.CLOCK_COLOR, false);
            }
        }

        String elapsedStr = JukeboxThemeUtils.formatTime(disc.isEmpty() ? -1 : (int) screen.getElapsedSeconds());
        String totalStr   = JukeboxThemeUtils.formatTime(disc.isEmpty() ? -1 : (int) totalSeconds);
        graphics.text(screen.getFont(), elapsedStr, gx + ELAPSED_X, gy + ELAPSED_Y, JukeboxThemeUtils.CLOCK_COLOR, false);
        graphics.text(screen.getFont(), totalStr,   gx + TOTAL_X,   gy + TOTAL_Y,   JukeboxThemeUtils.CLOCK_COLOR, false);

        int btnAbsX = gx + BTN_X;
        int btnAbsY = gy + BTN_Y;
        boolean btnHovered = mouseX >= btnAbsX && mouseX < btnAbsX + JukeboxThemeUtils.BTN_SIZE
                          && mouseY >= btnAbsY && mouseY < btnAbsY + JukeboxThemeUtils.BTN_SIZE;
        Identifier btnTex = JukeboxThemeUtils.getButtonTexture(disc, btnHovered, screen.isClientPaused(), screen.clientSoundActive);
        graphics.blit(RenderPipelines.GUI_TEXTURED, btnTex,
            btnAbsX, btnAbsY, 0f, 0f, JukeboxThemeUtils.BTN_SIZE, JukeboxThemeUtils.BTN_SIZE,
            JukeboxThemeUtils.BTN_SIZE, JukeboxThemeUtils.BTN_SIZE);

        JukeboxThemeUtils.renderThemeButton(graphics, gx, gy, TEX_THEME_5);
    }

    private void renderSpectrum(GuiGraphicsExtractor graphics, int gx, int gy, String discName, float t, float totalSeconds) {
        int firstColor  = getFirstLayerColor(discName);
        int secondColor = getSecondLayerColor(discName);
        int bottom = gy + VIS_Y + VIS_H;

        long now = System.currentTimeMillis();
        float dt = lastSmoothTime == 0L ? 0f : Math.min(0.5f, (now - lastSmoothTime) / 1000f);
        lastSmoothTime = now;

        for (int b = 0; b < BAND_COUNT; b++) {
            float raw = JukeboxBandData.getNormalizedLevel(discName, b, t, totalSeconds);
            float speed = raw > smoothedLevel[b] ? ATTACK_SPEED : DECAY_SPEED;
            float easeFactor = 1f - (float) Math.exp(-speed * dt);
            smoothedLevel[b] += (raw - smoothedLevel[b]) * easeFactor;
        }

        for (int i = 0; i < BAR_COUNT; i++) {
            float scale = Math.max(MIN_BAR_VALUE, interpolatedLevel(i));

            int frontHeight = Math.round(scale * FRONT_LAYER_H);
            int backHeight = frontHeight * 2;
            int barX = gx + VIS_X + i * SLOT_WIDTH;

            if (backHeight > 0) {
                graphics.fill(barX, bottom - backHeight, barX + BAR_WIDTH, bottom, secondColor);
            }
            if (frontHeight > 0) {
                graphics.fill(barX, bottom - frontHeight, barX + BAR_WIDTH, bottom, firstColor);
            }
        }
    }

    private float interpolatedLevel(int barIndex) {
        float p = barIndex / BAND_SPACING - 0.5f;
        int i0 = (int) Math.floor(p);
        int i1 = i0 + 1;
        float frac = p - i0;

        int c0 = Math.max(0, Math.min(BAND_COUNT - 1, i0));
        int c1 = Math.max(0, Math.min(BAND_COUNT - 1, i1));
        return smoothedLevel[c0] + (smoothedLevel[c1] - smoothedLevel[c0]) * frac;
    }

    private String resolveDiscName(ItemStack disc) {
        String itemPath = disc.getItem().toString();
        for (String name : JukeboxThemeUtils.DISC_NAMES) {
            if (itemPath.contains(name)) return name;
        }
        return null;
    }

    private int getFirstLayerColor(String discName) {
        if (discName == null) return 0xFFAAAAAA;
        return switch (discName) {
            case "5" -> 0xFF05625D;
            case "11" -> 0xFF494949;
            case "13" -> 0xFFFFFFFF;
            case "blocks" -> 0xFFE2543B;
            case "bounce" -> 0xFFE8E292;
            case "cat" -> 0xFF4CFF00;
            case "chirp" -> 0xFFFF0004;
            case "creator_music_box" -> 0xFFFFC967;
            case "creator" -> 0xFFFFC967;
            case "far" -> 0xFFB6FF00;
            case "lava_chicken" -> 0xFFFFFFF9;
            case "mall" -> 0xFF9A75FF;
            case "mellohi" -> 0xFFFFFFFF;
            case "otherside" -> 0xFF5FC546;
            case "pigstep" -> 0xFFF9B122;
            case "precipice" -> 0xFF6EC59F;
            case "relic" -> 0xFF88E6FF;
            case "stal" -> 0xFF393939;
            case "strad" -> 0xFFFFFFFF;
            case "tears" -> 0xFF9FC3C3;
            case "wait" -> 0xFF81A9E2;
            case "ward" -> 0xFF8EC600;
            default -> 0xFFAAAAAA;
        };
    }

    private int getSecondLayerColor(String discName) {
        if (discName == null) return 0xFF666666;
        return switch (discName) {
            case "5" -> 0xFF034150;
            case "11" -> 0xFF393939;
            case "13" -> 0xFFFFD800;
            case "blocks" -> 0xFFE2543B;
            case "bounce" -> 0xFF969968;
            case "cat" -> 0xFF1F6800;
            case "chirp" -> 0xFF7F0002;
            case "creator_music_box" -> 0xFFF09958;
            case "creator" -> 0xFFAFB374;
            case "far" -> 0xFF00FF90;
            case "lava_chicken" -> 0xFFDE1D1D;
            case "mall" -> 0xFF4800FF;
            case "mellohi" -> 0xFFB200FF;
            case "otherside" -> 0xFF31A2F2;
            case "pigstep" -> 0xFFE97D15;
            case "precipice" -> 0xFFE3826C;
            case "relic" -> 0xFF1F7BD6;
            case "stal" -> 0xFF000000;
            case "strad" -> 0xFF393939;
            case "tears" -> 0xFF577878;
            case "wait" -> 0xFF4874B3;
            case "ward" -> 0xFF006B57;
            default -> 0xFF666666;
        };
    }
}