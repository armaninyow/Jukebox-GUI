package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

// "Heartbeat/EKG" theme
public class JukeboxTheme11 implements JukeboxTheme {

    private static final Identifier CONTAINER_TEXTURE_11 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_11/jukebox_container_11.png");
    private static final Identifier TEX_THEME_11 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_11/theme_11.png");

    private static final int VIS_X = 8, VIS_Y = 16, VIS_W = 160, VIS_H = 30;
    private static final int BASELINE_Y = VIS_Y + VIS_H / 2;
    private static final float MAX_DEFLECTION = 14f;

    private static final float DERIVATIVE_LOOKBACK_SECONDS = 0.1f;
    private static final float DEFLECTION_GAIN = 3f;

    private static final float SWEEP_SPEED = 30f;
    private static final int ERASE_GAP_WIDTH = 5;


    private static final int TITLE_X  = 8,  TITLE_Y  = 48;
    private static final int ARTIST_X = 8,  ARTIST_Y = 60;
    private static final int ELAPSED_X = 143, ELAPSED_Y = 48;
    private static final int TOTAL_X   = 143, TOTAL_Y   = 60;
    private static final int BTN_X = 105, BTN_Y = 51;
    private static final int DISC_X = 123, DISC_Y = 50, DISC_SIZE = 16;

    @Override public int id() { return 11; }
    @Override public Identifier toggleIcon() { return TEX_THEME_11; }
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

        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE_11,
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
            renderEkgLine(graphics, gx, gy, discName, screen.getElapsedSeconds(), totalSeconds, playing);
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

        JukeboxThemeUtils.renderThemeButton(graphics, gx, gy, TEX_THEME_11);
    }

    private void renderEkgLine(GuiGraphicsExtractor graphics, int gx, int gy, String discName, float elapsedSeconds, float totalSeconds, boolean playing) {
        float globalSweepX = elapsedSeconds * SWEEP_SPEED;
        int currentLap = (int) Math.floor(globalSweepX / VIS_W);
        float tipX = globalSweepX - currentLap * VIS_W;

        int lineColor = playing ? JukeboxThemeUtils.getRainbowColor() : JukeboxThemeUtils.TITLE_COLOR;

        int baselineAbsY = gy + BASELINE_Y;
        Integer prevY = null;
        int prevCol = -1;

        for (int col = 0; col < VIS_W; col++) {
            Float queryTime = columnQueryTime(col, currentLap, tipX);
            if (queryTime == null) {
                prevY = null;
                continue;
            }

            float current = JukeboxBandData.getOnsetNormalizedLevel(discName, queryTime, totalSeconds);
            float previous = JukeboxBandData.getOnsetNormalizedLevel(
                discName, Math.max(0f, queryTime - DERIVATIVE_LOOKBACK_SECONDS), totalSeconds);
            float delta = Math.max(-1f, Math.min(1f, (current - previous) * DEFLECTION_GAIN));

            int y = baselineAbsY - Math.round(delta * MAX_DEFLECTION);
            int xAbs = gx + VIS_X + col;

            graphics.fill(xAbs, y, xAbs + 1, y + 1, lineColor);

            if (prevY != null && prevCol == col - 1) {
                int yTop = Math.min(prevY, y);
                int yBottom = Math.max(prevY, y) + 1;
                graphics.fill(xAbs, yTop, xAbs + 1, yBottom, lineColor);
            }

            prevY = y;
            prevCol = col;
        }
    }

    private Float columnQueryTime(int col, int currentLap, float tipX) {
        boolean inEraseGap = col > tipX && col <= tipX + ERASE_GAP_WIDTH;
        if (inEraseGap) return null;

        if (col <= tipX) {
            return (currentLap * VIS_W + col) / SWEEP_SPEED;
        } else {
            if (currentLap == 0) return null;
            return ((currentLap - 1) * VIS_W + col) / SWEEP_SPEED;
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