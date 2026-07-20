package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

// "Static album art" theme
public class JukeboxTheme6 implements JukeboxTheme {

    private static final Identifier CONTAINER_TEXTURE_6 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_6/jukebox_container_6.png");
    private static final Identifier TEX_THEME_6 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_6/theme_6.png");

    private static final int TITLE_X  = 56, TITLE_Y  = 25;
    private static final int ARTIST_X = 56, ARTIST_Y = 37;
    private static final int BTN_X    = 56, BTN_Y    = 49;
    private static final int ELAPSED_X = 73, ELAPSED_Y = 52;
    private static final int TOTAL_X   = 105, TOTAL_Y  = 52;

    private static final int DISC_X = 8, DISC_Y = 21, DISC_SIZE = 45;

    @Override public int id() { return 6; }
    @Override public Identifier toggleIcon() { return TEX_THEME_6; }
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

        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE_6,
            gx, gy, 0f, 0f, JukeboxManagementScreen.BG_WIDTH, JukeboxManagementScreen.BG_HEIGHT,
            JukeboxManagementScreen.BG_WIDTH, JukeboxManagementScreen.BG_HEIGHT);

        ItemStack disc = screen.getDiscStack();

        boolean playing = !screen.isClientPaused() && screen.timerStartTime > 0 && !screen.songFinished;
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

        int btnAbsX = gx + BTN_X;
        int btnAbsY = gy + BTN_Y;
        boolean btnHovered = mouseX >= btnAbsX && mouseX < btnAbsX + JukeboxThemeUtils.BTN_SIZE
                          && mouseY >= btnAbsY && mouseY < btnAbsY + JukeboxThemeUtils.BTN_SIZE;
        Identifier btnTex = JukeboxThemeUtils.getButtonTexture(disc, btnHovered, screen.isClientPaused(), screen.clientSoundActive);
        graphics.blit(RenderPipelines.GUI_TEXTURED, btnTex,
            btnAbsX, btnAbsY, 0f, 0f, JukeboxThemeUtils.BTN_SIZE, JukeboxThemeUtils.BTN_SIZE,
            JukeboxThemeUtils.BTN_SIZE, JukeboxThemeUtils.BTN_SIZE);

        String elapsedStr = JukeboxThemeUtils.formatTime(disc.isEmpty() ? -1 : (int) screen.getElapsedSeconds());
        String totalStr   = JukeboxThemeUtils.formatTime(disc.isEmpty() ? -1 : (int) screen.totalSeconds);
        graphics.text(screen.getFont(), elapsedStr, gx + ELAPSED_X, gy + ELAPSED_Y, JukeboxThemeUtils.CLOCK_COLOR, false);
        graphics.text(screen.getFont(), totalStr,   gx + TOTAL_X,   gy + TOTAL_Y,   JukeboxThemeUtils.CLOCK_COLOR, false);

        int discAbsX = gx + DISC_X;
        int discAbsY = gy + DISC_Y;

        if (!disc.isEmpty()) {
            Identifier albumTex = getAlbumTexture(disc);
            if (albumTex != null) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, albumTex,
                    discAbsX, discAbsY, 0f, 0f, DISC_SIZE, DISC_SIZE, DISC_SIZE, DISC_SIZE);
            } else {
                int itemOffX = discAbsX + (DISC_SIZE - 16) / 2;
                int itemOffY = discAbsY + (DISC_SIZE - 16) / 2;
                graphics.item(disc, itemOffX, itemOffY);
                graphics.itemDecorations(screen.getFont(), disc, itemOffX, itemOffY, null);
            }
        }

        if (mouseX >= discAbsX && mouseX < discAbsX + DISC_SIZE
         && mouseY >= discAbsY && mouseY < discAbsY + DISC_SIZE) {
            graphics.fill(discAbsX, discAbsY, discAbsX + DISC_SIZE, discAbsY + DISC_SIZE, 0x80FFFFFF);
        }

        JukeboxThemeUtils.renderThemeButton(graphics, gx, gy, TEX_THEME_6);
    }

    private Identifier getAlbumTexture(ItemStack disc) {
        String itemPath = disc.getItem().toString();
        for (String name : JukeboxThemeUtils.DISC_NAMES) {
            if (itemPath.contains(name)) {
                return Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID,
                    "textures/gui/theme_6/" + name + "_album.png");
            }
        }
        return null;
    }
}