package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

// Original "horizontal progress bar" theme
public class JukeboxTheme1 implements JukeboxTheme {

    private static final Identifier CONTAINER_TEXTURE =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_1/jukebox_container.png");
    private static final Identifier PROGRESS_BG_TEXTURE =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_1/progress_background.png");
    private static final Identifier TEX_THEME_1 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_1/theme_1.png");

    private static final int TITLE_Y = 38;
    private static final int PROG_BG_X = 45, PROG_BG_Y = 49, PROG_BG_W = 86, PROG_BG_H = 5;
    private static final int PROG_X    = 46, PROG_Y    = 50, PROG_W    = 84, PROG_H    = 3;
    private static final int CLOCK_Y   = 60;

    private static final int BTN_REL_X = 81;
    private static final int BTN_REL_Y = 57;

    private static final int DISC_X = 80, DISC_Y = 17, DISC_SIZE = 16;

    @Override public int id() { return 1; }
    @Override public Identifier toggleIcon() { return TEX_THEME_1; }
    @Override public int playPauseBtnX() { return BTN_REL_X; }
    @Override public int playPauseBtnY() { return BTN_REL_Y; }
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

        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE,
            gx, gy, 0f, 0f, JukeboxManagementScreen.BG_WIDTH, JukeboxManagementScreen.BG_HEIGHT,
            JukeboxManagementScreen.BG_WIDTH, JukeboxManagementScreen.BG_HEIGHT);

        ItemStack disc = screen.getDiscStack();
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

        graphics.blit(RenderPipelines.GUI_TEXTURED, PROGRESS_BG_TEXTURE,
            gx + PROG_BG_X, gy + PROG_BG_Y,
            0f, 0f, PROG_BG_W, PROG_BG_H, PROG_BG_W, PROG_BG_H);

        float totalSeconds = screen.totalSeconds;
        if (!disc.isEmpty() && totalSeconds > 0) {
            Identifier progTex = getProgressTexture(disc);
            if (progTex != null) {
                float ratio = Math.min(1f, screen.getElapsedSeconds() / totalSeconds);
                int progWidth = Math.max(0, Math.round(ratio * PROG_W));
                if (progWidth > 0) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, progTex,
                        gx + PROG_X, gy + PROG_Y,
                        0f, 0f, progWidth, PROG_H, PROG_W, PROG_H);
                }
            }
        }

        String songTitle = screen.songTitle;
        if (!songTitle.isEmpty()) {
            int titleX = gx + (JukeboxManagementScreen.BG_WIDTH - screen.getFont().width(songTitle)) / 2;
            if (!screen.isClientPaused() && screen.timerStartTime > 0 && !screen.songFinished) {
                JukeboxThemeUtils.drawRainbowText(graphics, screen.getFont(), songTitle, titleX, gy + TITLE_Y);
            } else {
                graphics.text(screen.getFont(), songTitle, titleX, gy + TITLE_Y, JukeboxThemeUtils.TITLE_COLOR, false);
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
        graphics.text(screen.getFont(), elapsedStr,
            gx + PROG_X + 7, gy + CLOCK_Y, JukeboxThemeUtils.CLOCK_COLOR, false);
        int totalW = screen.getFont().width(totalStr);
        graphics.text(screen.getFont(), totalStr,
            gx + PROG_X + PROG_W - 6 - totalW, gy + CLOCK_Y, JukeboxThemeUtils.CLOCK_COLOR, false);

        int btnAbsX = gx + BTN_REL_X;
        int btnAbsY = gy + BTN_REL_Y;
        boolean hovered = mouseX >= btnAbsX && mouseX < btnAbsX + JukeboxThemeUtils.BTN_SIZE
                       && mouseY >= btnAbsY && mouseY < btnAbsY + JukeboxThemeUtils.BTN_SIZE;
        Identifier btnTex = JukeboxThemeUtils.getButtonTexture(disc, hovered, screen.isClientPaused(), screen.clientSoundActive);
        graphics.blit(RenderPipelines.GUI_TEXTURED, btnTex,
            btnAbsX, btnAbsY, 0f, 0f, JukeboxThemeUtils.BTN_SIZE, JukeboxThemeUtils.BTN_SIZE,
            JukeboxThemeUtils.BTN_SIZE, JukeboxThemeUtils.BTN_SIZE);

        JukeboxThemeUtils.renderThemeButton(graphics, gx, gy, TEX_THEME_1);
    }

    private Identifier getProgressTexture(ItemStack disc) {
        String itemPath = disc.getItem().toString();
        for (String name : JukeboxThemeUtils.DISC_NAMES) {
            if (itemPath.contains(name)) {
                return Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID,
                    "textures/gui/theme_1/" + name + "_progress.png");
            }
        }
        return null;
    }
}