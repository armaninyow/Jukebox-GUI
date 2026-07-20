package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

// "Redstone" theme
public class JukeboxTheme10 implements JukeboxTheme {

    private static final Identifier CONTAINER_TEXTURE_10 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_10/jukebox_container_10.png");
    private static final Identifier TEX_THEME_10 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_10/theme_10.png");
    private static final Identifier TEX_SLOT =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_10/slot.png");

    private static final Identifier TEX_NS_ACTIVE   = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_10/redstone_dust_line_ns_active.png");
    private static final Identifier TEX_NS_INACTIVE = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_10/redstone_dust_line_ns_inactive.png");
    private static final Identifier TEX_SW_ACTIVE   = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_10/redstone_dust_line_sw_active.png");
    private static final Identifier TEX_SW_INACTIVE = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_10/redstone_dust_line_sw_inactive.png");
    private static final Identifier TEX_NW_ACTIVE   = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_10/redstone_dust_line_nw_active.png");
    private static final Identifier TEX_NW_INACTIVE = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_10/redstone_dust_line_nw_inactive.png");
    private static final Identifier TEX_EW_ACTIVE   = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_10/redstone_dust_line_ew_active.png");
    private static final Identifier TEX_EW_INACTIVE = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_10/redstone_dust_line_ew_inactive.png");

    private static final int DUST_SIZE = 16;
    private static final int SLOT_SIZE = 18;
    private static final int DISC_SIZE = 16;
    private static final int BTN_SIZE_VAL = 14;

    private static final int TITLE_X   = 8, TITLE_Y   = 41;
    private static final int ELAPSED_X = 8, ELAPSED_Y = 29;
    private static final int TOTAL_X   = 8, TOTAL_Y   = 53;

    private static final int[] SLOT_X = {
        151, 151, 134, 134, 118, 118, 102, 102, 86, 86, 70, 70, 54, 54, 38, 38
    };
    private static final int[] SLOT_Y = {
        26, 18, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19
    };

    private static final int[] BTN_X = {
        153, 153, 153, 138, 138, 122, 122, 106, 106, 90, 90, 74, 74, 58, 58, 42
    };
    private static final int[] BTN_Y = {
        44, 52, 52, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53
    };

    private static final int NS_X = 152, NS_Y = 36;
    private static final int SW_X = 152, SW_Y = 20;
    private static final int NW_X = 152, NW_Y = 52;
    private static final int EW_BASE_X = 136;
    private static final int EW_TOP_Y  = 20;
    private static final int EW_BOT_Y  = 52;

    private static int getPowerLevel(ItemStack disc) {
        if (disc.isEmpty()) return 0;
        String p = disc.getItem().toString();
        if (p.contains("13"))                                          return 1;
        if (p.contains("cat"))                                         return 2;
        if (p.contains("blocks"))                                      return 3;
        if (p.contains("chirp"))                                       return 4;
        if (p.contains("far"))                                         return 5;
        if (p.contains("mall"))                                        return 6;
        if (p.contains("mellohi"))                                     return 7;
        if (p.contains("stal") || p.contains("bounce"))               return 8;
        if (p.contains("strad") || p.contains("lava_chicken"))        return 9;
        if (p.contains("ward")  || p.contains("tears"))               return 10;
        if (p.contains("11")    || p.contains("creator_music_box"))   return 11;
        if (p.contains("wait")  || p.contains("creator"))             return 12;
        if (p.contains("pigstep") || p.contains("precipice"))         return 13;
        if (p.contains("otherside") || p.contains("relic"))           return 14;
        if (p.contains("5"))                                           return 15;
        return 0;
    }

    private int lastPower = 0;

    @Override public int id() { return 10; }
    @Override public Identifier toggleIcon() { return TEX_THEME_10; }
    @Override public int playPauseBtnX() { return BTN_X[lastPower]; }
    @Override public int playPauseBtnY() { return BTN_Y[lastPower]; }
    @Override public int playPauseBtnSize() { return BTN_SIZE_VAL; }
    @Override public boolean skipSlot(int slotIndex) { return slotIndex == 0; }

    @Override
    public boolean handleClick(JukeboxManagementScreen screen, MouseButtonEvent event) {
        int power = getPowerLevel(screen.getDiscStack());
        int itemAbsX = screen.getGuiLeft() + SLOT_X[power] + 1;
        int itemAbsY = screen.getGuiTop()  + SLOT_Y[power] + 1;
        if (event.x() >= itemAbsX && event.x() < itemAbsX + DISC_SIZE
         && event.y() >= itemAbsY && event.y() < itemAbsY + DISC_SIZE) {
            screen.clickDiscSlot(event);
            return true;
        }
        return false;
    }

    @Override
    public void render(JukeboxManagementScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int gx = screen.getGuiLeft();
        int gy = screen.getGuiTop();

        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE_10,
            gx, gy, 0f, 0f, JukeboxManagementScreen.BG_WIDTH, JukeboxManagementScreen.BG_HEIGHT,
            JukeboxManagementScreen.BG_WIDTH, JukeboxManagementScreen.BG_HEIGHT);

        ItemStack disc = screen.getDiscStack();
        float totalSeconds = screen.totalSeconds;
        boolean playing = !screen.isClientPaused() && screen.timerStartTime > 0 && !screen.songFinished;
        boolean active = playing;
        int power = getPowerLevel(disc);
        lastPower = power;

        String songTitle = screen.songTitle;
        if (!songTitle.isEmpty()) {
            if (playing) {
                JukeboxThemeUtils.drawRainbowText(graphics, screen.getFont(), songTitle, gx + TITLE_X, gy + TITLE_Y);
            } else {
                graphics.text(screen.getFont(), songTitle, gx + TITLE_X, gy + TITLE_Y, JukeboxThemeUtils.TITLE_COLOR, false);
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

        if (power >= 1) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, active ? TEX_NS_ACTIVE : TEX_NS_INACTIVE,
                gx + NS_X, gy + NS_Y, 0f, 0f, DUST_SIZE, DUST_SIZE, DUST_SIZE, DUST_SIZE);

            if (power >= 2) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, active ? TEX_SW_ACTIVE : TEX_SW_INACTIVE,
                    gx + SW_X, gy + SW_Y, 0f, 0f, DUST_SIZE, DUST_SIZE, DUST_SIZE, DUST_SIZE);
            }

            if (power >= 3) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, active ? TEX_NW_ACTIVE : TEX_NW_INACTIVE,
                    gx + NW_X, gy + NW_Y, 0f, 0f, DUST_SIZE, DUST_SIZE, DUST_SIZE, DUST_SIZE);
            }

            int ewTopCount = Math.max(0, (power - 2) / 2);
            for (int i = 0; i < ewTopCount; i++) {
                int texX = EW_BASE_X - i * DUST_SIZE;
                graphics.blit(RenderPipelines.GUI_TEXTURED, active ? TEX_EW_ACTIVE : TEX_EW_INACTIVE,
                    gx + texX, gy + EW_TOP_Y, 0f, 0f, DUST_SIZE, DUST_SIZE, DUST_SIZE, DUST_SIZE);
            }

            int ewBotCount = Math.max(0, (power - 3) / 2);
            for (int i = 0; i < ewBotCount; i++) {
                int texX = EW_BASE_X - i * DUST_SIZE;
                graphics.blit(RenderPipelines.GUI_TEXTURED, active ? TEX_EW_ACTIVE : TEX_EW_INACTIVE,
                    gx + texX, gy + EW_BOT_Y, 0f, 0f, DUST_SIZE, DUST_SIZE, DUST_SIZE, DUST_SIZE);
            }
        }

        int slotAbsX = gx + SLOT_X[power];
        int slotAbsY = gy + SLOT_Y[power];
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEX_SLOT,
            slotAbsX, slotAbsY, 0f, 0f, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);

        int itemAbsX = slotAbsX + 1;
        int itemAbsY = slotAbsY + 1;
        if (!disc.isEmpty()) {
            graphics.item(disc, itemAbsX, itemAbsY);
            graphics.itemDecorations(screen.getFont(), disc, itemAbsX, itemAbsY, null);
        }
        if (mouseX >= itemAbsX && mouseX < itemAbsX + DISC_SIZE
         && mouseY >= itemAbsY && mouseY < itemAbsY + DISC_SIZE) {
            graphics.fill(itemAbsX, itemAbsY, itemAbsX + DISC_SIZE, itemAbsY + DISC_SIZE, 0x80FFFFFF);
        }

        int btnAbsX = gx + BTN_X[power];
        int btnAbsY = gy + BTN_Y[power];
        boolean btnHovered = mouseX >= btnAbsX && mouseX < btnAbsX + BTN_SIZE_VAL
                          && mouseY >= btnAbsY && mouseY < btnAbsY + BTN_SIZE_VAL;
        Identifier btnTex = JukeboxThemeUtils.getButtonTexture(disc, btnHovered, screen.isClientPaused(), screen.clientSoundActive);
        graphics.blit(RenderPipelines.GUI_TEXTURED, btnTex,
            btnAbsX, btnAbsY, 0f, 0f, BTN_SIZE_VAL, BTN_SIZE_VAL, BTN_SIZE_VAL, BTN_SIZE_VAL);

        JukeboxThemeUtils.renderThemeButton(graphics, gx, gy, TEX_THEME_10);
    }
}