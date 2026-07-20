package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

// "VU meter" theme
public class JukeboxTheme9 implements JukeboxTheme {

    private static final Identifier CONTAINER_TEXTURE_9 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_9/jukebox_container_9.png");
    private static final Identifier TEX_POINTER =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_9/pointer.png");
    private static final Identifier TEX_METER_FOREGROUND =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_9/meter_foreground.png");
    private static final Identifier TEX_PEAK_ON =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_9/peak_on.png");
    private static final Identifier TEX_PEAK_OFF =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_9/peak_off.png");
    private static final Identifier TEX_THEME_9 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_9/theme_9.png");

    private static final int TITLE_X  = 8,  TITLE_Y  = 25;
    private static final int ARTIST_X = 8,  ARTIST_Y = 37;
    private static final int BTN_X    = 8,  BTN_Y    = 49;
    private static final int ELAPSED_X = 25, ELAPSED_Y = 52;
    private static final int TOTAL_X   = 57, TOTAL_Y   = 52;

    private static final int DISC_X = 135, DISC_Y = 48, DISC_SIZE = 16;

    private static final int METER_FG_X = 116, METER_FG_Y = 17, METER_FG_W = 53, METER_FG_H = 49;

    private static final int PEAK_X = 139, PEAK_Y = 36, PEAK_SIZE = 8;
    private static final float PEAK_ON_VU_THRESHOLD = -3f;

    private static final int POINTER_W = 1, POINTER_H = 30;
    private static final int PIVOT_X = 142, PIVOT_Y = 56;

    private static final float ANGLE_AT_MIN_VU = -30f;
    private static final float ANGLE_AT_MAX_VU = 30f;
    private static final float MIN_VU = -20f;
    private static final float MAX_VU = 3f;

    private static final float VU_REFERENCE_DB = -41f;

    @Override public int id() { return 9; }
    @Override public Identifier toggleIcon() { return TEX_THEME_9; }
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

        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE_9,
            gx, gy, 0f, 0f, JukeboxManagementScreen.BG_WIDTH, JukeboxManagementScreen.BG_HEIGHT,
            JukeboxManagementScreen.BG_WIDTH, JukeboxManagementScreen.BG_HEIGHT);

        ItemStack disc = screen.getDiscStack();
        float totalSeconds = screen.totalSeconds;
        boolean playing = !screen.isClientPaused() && screen.timerStartTime > 0 && !screen.songFinished;

        float vu = computeVu(screen, disc);
        renderPointer(graphics, gx, gy, vuToAngle(vu));

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEX_METER_FOREGROUND,
            gx + METER_FG_X, gy + METER_FG_Y, 0f, 0f, METER_FG_W, METER_FG_H, METER_FG_W, METER_FG_H);

        Identifier peakTex = vu >= PEAK_ON_VU_THRESHOLD ? TEX_PEAK_ON : TEX_PEAK_OFF;
        graphics.blit(RenderPipelines.GUI_TEXTURED, peakTex,
            gx + PEAK_X, gy + PEAK_Y, 0f, 0f, PEAK_SIZE, PEAK_SIZE, PEAK_SIZE, PEAK_SIZE);

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

        JukeboxThemeUtils.renderThemeButton(graphics, gx, gy, TEX_THEME_9);
    }

    private float computeVu(JukeboxManagementScreen screen, ItemStack disc) {
        if (disc.isEmpty()) return MIN_VU;

        String discName = resolveDiscName(disc);
        float combinedDb = JukeboxBandData.getCombinedDb(discName, screen.getElapsedSeconds(), screen.totalSeconds);
        if (combinedDb == Float.NEGATIVE_INFINITY) return MIN_VU;

        return Math.max(MIN_VU, Math.min(MAX_VU, combinedDb - VU_REFERENCE_DB));
    }

    private float vuToAngle(float vu) {
        float t = (vu - MIN_VU) / (MAX_VU - MIN_VU);
        return ANGLE_AT_MIN_VU + t * (ANGLE_AT_MAX_VU - ANGLE_AT_MIN_VU);
    }

    private void renderPointer(GuiGraphicsExtractor graphics, int gx, int gy, float angleDeg) {
        int pivotAbsX = gx + PIVOT_X;
        int pivotAbsY = gy + PIVOT_Y;
        float rad = (float) Math.toRadians(angleDeg);
        float cosA = (float) Math.cos(rad);
        float sinA = (float) Math.sin(rad);

        int margin = POINTER_H + 2;
        for (int dy = -margin; dy <= margin; dy++) {
            for (int dx = -margin; dx <= margin; dx++) {
                float sx = dx + 0.5f;
                float sy = dy + 0.5f;

                float localX =  cosA * sx + sinA * sy;
                float localY = -sinA * sx + cosA * sy;

                if (localX < 0f || localX >= 1f) continue;
                if (localY < -POINTER_H || localY >= 0f) continue;

                int texRow = Math.max(0, Math.min(POINTER_H - 1, (int) Math.floor(localY + POINTER_H)));

                graphics.blit(RenderPipelines.GUI_TEXTURED, TEX_POINTER,
                    pivotAbsX + dx, pivotAbsY + dy,
                    0f, (float) texRow,
                    1, 1,
                    POINTER_W, POINTER_H);
            }
        }
    }

    private String resolveDiscName(ItemStack disc) {
        String itemPath = disc.getItem().toString();
        for (String name : JukeboxThemeUtils.DISC_NAMES) {
            if (itemPath.contains(name)) return name;
        }
        return null;
    }
}