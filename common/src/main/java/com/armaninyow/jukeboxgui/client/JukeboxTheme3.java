package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

// "Sliding indicator" theme
public class JukeboxTheme3 implements JukeboxTheme {

    private static final Identifier CONTAINER_TEXTURE_3 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_3/jukebox_container_3.png");
    private static final Identifier DISC_BG_TEXTURE_3 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_3/disc_background_3.png");
    private static final Identifier PROGRESS_BG_TEXTURE_3 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_3/progress_background_3.png");
    private static final Identifier TEX_THEME_3 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_3/theme_3.png");

    private static final int TITLE_Y  = 49;
    private static final int ARTIST_Y = 61;

    private static final int BTN_X = 81, BTN_Y = 17;

    private static final int ELAPSED_X = 27, ELAPSED_Y = 22;
    private static final int TOTAL_X   = 124, TOTAL_Y  = 22;

    private static final int PROG_BG_X = 27, PROG_BG_Y = 38, PROG_BG_W = 122, PROG_BG_H = 3;
    private static final int PROG_X    = 28, PROG_Y    = 39, PROG_W    = 120, PROG_H    = 1;

    private static final int DISC_START_X = 23, DISC_END_X = 138, DISC_Y = 32, DISC_SIZE = 16;

    @Override public int id() { return 3; }
    @Override public Identifier toggleIcon() { return TEX_THEME_3; }
    @Override public int playPauseBtnX() { return BTN_X; }
    @Override public int playPauseBtnY() { return BTN_Y; }
    @Override public int playPauseBtnSize() { return JukeboxThemeUtils.BTN_SIZE; }
    @Override public boolean skipSlot(int slotIndex) { return slotIndex == 0; }

    @Override
    public boolean handleClick(JukeboxManagementScreen screen, MouseButtonEvent event) {
        int discAbsX = screen.getGuiLeft() + getDiscIndicatorX(screen);
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

        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE_3,
            gx, gy, 0f, 0f, JukeboxManagementScreen.BG_WIDTH, JukeboxManagementScreen.BG_HEIGHT,
            JukeboxManagementScreen.BG_WIDTH, JukeboxManagementScreen.BG_HEIGHT);

        graphics.blit(RenderPipelines.GUI_TEXTURED, PROGRESS_BG_TEXTURE_3,
            gx + PROG_BG_X, gy + PROG_BG_Y, 0f, 0f, PROG_BG_W, PROG_BG_H, PROG_BG_W, PROG_BG_H);

        ItemStack disc = screen.getDiscStack();
        float totalSeconds = screen.totalSeconds;
        boolean playing = !screen.isClientPaused() && screen.timerStartTime > 0 && !screen.songFinished;

        if (!disc.isEmpty() && totalSeconds > 0) {
            float ratio = Math.min(1f, screen.getElapsedSeconds() / totalSeconds);
            int fillWidth = Math.max(0, Math.round(ratio * PROG_W));
            if (fillWidth > 0) {
                int fillColor = playing ? JukeboxThemeUtils.getRainbowColor() : JukeboxThemeUtils.TITLE_COLOR;
                graphics.fill(gx + PROG_X, gy + PROG_Y, gx + PROG_X + fillWidth, gy + PROG_Y + PROG_H, fillColor);
            }
        }

        String songTitle = screen.songTitle;
        if (!songTitle.isEmpty()) {
            String[] parts = JukeboxThemeUtils.splitSongTitle(songTitle);
            String titleLine  = parts[0];
            String artistLine = parts[1];

            int titleX = gx + (JukeboxManagementScreen.BG_WIDTH - screen.getFont().width(titleLine)) / 2;
            if (playing) {
                JukeboxThemeUtils.drawRainbowText(graphics, screen.getFont(), titleLine, titleX, gy + TITLE_Y);
            } else {
                graphics.text(screen.getFont(), titleLine, titleX, gy + TITLE_Y, JukeboxThemeUtils.TITLE_COLOR, false);
            }

            if (!artistLine.isEmpty()) {
                int artistX = gx + (JukeboxManagementScreen.BG_WIDTH - screen.getFont().width(artistLine)) / 2;
                if (playing) {
                    JukeboxThemeUtils.drawRainbowText(graphics, screen.getFont(), artistLine, artistX, gy + ARTIST_Y);
                } else {
                    graphics.text(screen.getFont(), artistLine, artistX, gy + ARTIST_Y, JukeboxThemeUtils.TITLE_COLOR, false);
                }
            }
        } else {
            String insertText = "INSERT DISC";
            int insertX = gx + (JukeboxManagementScreen.BG_WIDTH - screen.getFont().width(insertText)) / 2;
            if (JukeboxThemeUtils.isBlinkVisible()) {
                graphics.text(screen.getFont(), insertText, insertX, gy + TITLE_Y, JukeboxThemeUtils.CLOCK_COLOR, false);
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

        int discAbsX = gx + getDiscIndicatorX(screen);
        int discAbsY = gy + DISC_Y;

        if (!disc.isEmpty()) {
            graphics.item(disc, discAbsX, discAbsY);
            graphics.itemDecorations(screen.getFont(), disc, discAbsX, discAbsY, null);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, DISC_BG_TEXTURE_3,
                discAbsX, discAbsY, 0f, 0f, DISC_SIZE, DISC_SIZE, DISC_SIZE, DISC_SIZE);
        }

        if (mouseX >= discAbsX && mouseX < discAbsX + DISC_SIZE
         && mouseY >= discAbsY && mouseY < discAbsY + DISC_SIZE) {
            graphics.fill(discAbsX, discAbsY, discAbsX + DISC_SIZE, discAbsY + DISC_SIZE, 0x80FFFFFF);
        }

        JukeboxThemeUtils.renderThemeButton(graphics, gx, gy, TEX_THEME_3);
    }

    private int getDiscIndicatorX(JukeboxManagementScreen screen) {
        ItemStack disc = screen.getDiscStack();
        if (disc.isEmpty() || screen.totalSeconds <= 0f) return DISC_START_X;
        float ratio = Math.min(1f, screen.getElapsedSeconds() / screen.totalSeconds);
        return Math.round(DISC_START_X + ratio * (DISC_END_X - DISC_START_X));
    }
}