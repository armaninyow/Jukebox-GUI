package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

// "Record player" theme
public class JukeboxTheme2 implements JukeboxTheme {

    private static final Identifier CONTAINER_TEXTURE_2 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_2/jukebox_container_2.png");
    private static final Identifier TEX_STYLUS =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_2/diamond_stylus.png");
    private static final Identifier TEX_DISC_BG =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_2/disc_background.png");
    private static final Identifier TEX_THEME_2 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_2/theme_2.png");

    private static final int STYLUS_X = 141, STYLUS_Y = 61, STYLUS_SIZE = 9;
    private static final int TITLE_X  = 8,   TITLE_Y  = 25;
    private static final int ARTIST_X = 8,   ARTIST_Y = 37;
    private static final int BTN_X    = 8,   BTN_Y    = 49;
    private static final int ELAPSED_X = 25, ELAPSED_Y = 52;
    private static final int TOTAL_X   = 57, TOTAL_Y   = 52;
    private static final int DISC_X   = 123, DISC_Y   = 21, DISC_SIZE = 45;

    private int  stylusYOffset  = 0;
    private long lastStylusTick = 0;

    @Override public int id() { return 2; }
    @Override public Identifier toggleIcon() { return TEX_THEME_2; }
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

        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE_2,
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
            Identifier discTex = getDiscTexture(disc);
            if (discTex != null) {
                float angle = getDiscAngle(screen);
                DiscRotationRenderState.submit(graphics, discTex, discAbsX, discAbsY, DISC_SIZE, angle);

                Identifier fgTex = getDiscForegroundTexture(disc);
                if (fgTex != null) {
                    DiscRotationRenderState.submit(graphics, fgTex, discAbsX, discAbsY, DISC_SIZE, 0f);
                }
            } else {
                int itemOffX = discAbsX + (DISC_SIZE - 16) / 2;
                int itemOffY = discAbsY + (DISC_SIZE - 16) / 2;
                graphics.item(disc, itemOffX, itemOffY);
                graphics.itemDecorations(screen.getFont(), disc, itemOffX, itemOffY, null);
            }

            if (mouseX >= discAbsX && mouseX < discAbsX + DISC_SIZE
             && mouseY >= discAbsY && mouseY < discAbsY + DISC_SIZE) {
                graphics.fill(discAbsX, discAbsY, discAbsX + DISC_SIZE, discAbsY + DISC_SIZE, 0x80FFFFFF);
            }
            renderStylus(graphics, gx, gy, disc, screen);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEX_DISC_BG,
                discAbsX, discAbsY, 0f, 0f, DISC_SIZE, DISC_SIZE, DISC_SIZE, DISC_SIZE);

            if (mouseX >= discAbsX && mouseX < discAbsX + DISC_SIZE
             && mouseY >= discAbsY && mouseY < discAbsY + DISC_SIZE) {
                graphics.fill(discAbsX, discAbsY, discAbsX + DISC_SIZE, discAbsY + DISC_SIZE, 0x80FFFFFF);
            }
            renderStylus(graphics, gx, gy, disc, screen);
        }

        JukeboxThemeUtils.renderThemeButton(graphics, gx, gy, TEX_THEME_2);
    }

    private void renderStylus(GuiGraphicsExtractor graphics, int gx, int gy, ItemStack disc, JukeboxManagementScreen screen) {
        boolean playing = !screen.isClientPaused() && screen.timerStartTime > 0 && !screen.songFinished && !disc.isEmpty();
        if (playing) {
            long now = System.currentTimeMillis();
            if (lastStylusTick == 0) lastStylusTick = now;
            if (now - lastStylusTick >= 1000) {
                stylusYOffset = (stylusYOffset == 0) ? 1 : 0;
                lastStylusTick = now;
            }
        } else {
            lastStylusTick = 0;
        }

        int stylusAbsX = gx + STYLUS_X;
        int stylusAbsY = gy + STYLUS_Y + stylusYOffset;
        float cx = stylusAbsX + STYLUS_SIZE / 2f;
        float cy = stylusAbsY + STYLUS_SIZE / 2f;

        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().rotate((float) Math.toRadians(-45));
        graphics.pose().translate(-cx, -cy);

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEX_STYLUS,
            stylusAbsX, stylusAbsY, 0f, 0f, STYLUS_SIZE, STYLUS_SIZE, STYLUS_SIZE, STYLUS_SIZE);

        graphics.pose().popMatrix();
    }

    private float getDiscAngle(JukeboxManagementScreen screen) {
        if (screen.totalSeconds <= 0f) return 0f;
        float elapsed = screen.getElapsedSeconds();
        if (screen.songFinished || elapsed >= screen.totalSeconds) return 0f;
        float stepped = (float) Math.floor((elapsed / screen.totalSeconds) * 100f) / 100f;
        return stepped * 360f;
    }

    private Identifier getDiscTexture(ItemStack disc) {
        String itemPath = disc.getItem().toString();
        for (String name : JukeboxThemeUtils.DISC_NAMES) {
            if (itemPath.contains(name)) {
                return Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID,
                    "textures/gui/theme_2/" + name + "_disc.png");
            }
        }
        return null;
    }

    private Identifier getDiscForegroundTexture(ItemStack disc) {
        String itemPath = disc.getItem().toString();
        for (String name : JukeboxThemeUtils.DISC_NAMES) {
            if (itemPath.contains(name)) {
                return Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID,
                    "textures/gui/theme_2/" + name + "_disc_foreground.png");
            }
        }
        return null;
    }
}