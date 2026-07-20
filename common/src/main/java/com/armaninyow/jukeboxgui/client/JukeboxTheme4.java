package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

// "Waveform" theme
public class JukeboxTheme4 implements JukeboxTheme {

    private static final Identifier CONTAINER_TEXTURE_4 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_4/jukebox_container_4.png");
    private static final Identifier TEX_THEME_4 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_4/theme_4.png");

    private static final int WAVEFORM_X = 8, WAVEFORM_Y = 16, WAVEFORM_W = 160, WAVEFORM_H = 30;

    private static final int PLAYHEAD_CENTER = WAVEFORM_W / 2;

    private static final int TITLE_X  = 8, TITLE_Y  = 48;
    private static final int ARTIST_X = 8, ARTIST_Y = 60;

    private static final int ELAPSED_X = 143, ELAPSED_Y = 48;
    private static final int TOTAL_X   = 143, TOTAL_Y   = 60;

    private static final int BTN_X = 105, BTN_Y = 51;

    private static final int DISC_X = 123, DISC_Y = 50, DISC_SIZE = 16;

    private static int getWaveformTexWidth(String discName) {
        if (discName == null) return WAVEFORM_W;
        return switch (discName) {
            case "11"                -> 357;
            case "13"                -> 898;
            case "5"                 -> 899;
            case "blocks"            -> 1750;
            case "bounce"            -> 1188;
            case "cat"               -> 935;
            case "chirp"             -> 936;
            case "creator_music_box" -> 367;
            case "creator"           -> 889;
            case "far"               -> 880;
            case "lava_chicken"      -> 678;
            case "mall"              -> 995;
            case "mellohi"           -> 484;
            case "otherside"         -> 987;
            case "pigstep"           -> 747;
            case "precipice"         -> 1511;
            case "relic"             -> 1105;
            case "stal"              -> 760;
            case "strad"             -> 949;
            case "tears"             -> 883;
            case "wait"              -> 1200;
            case "ward"              -> 1268;
            default                  -> WAVEFORM_W;
        };
    }

    @Override public int id() { return 4; }
    @Override public Identifier toggleIcon() { return TEX_THEME_4; }
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

        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE_4,
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
            int texW = getWaveformTexWidth(discName);

            float ratio = (totalSeconds > 0) ? Math.min(1f, screen.getElapsedSeconds() / totalSeconds) : 0f;

            float playheadInTexture = ratio * texW;

            float maxScroll = texW - WAVEFORM_W;
            float rawScroll = playheadInTexture - PLAYHEAD_CENTER;
            float scrollOffset = (float) Math.floor(Math.max(0f, Math.min(maxScroll, rawScroll)));

            int playheadInFrame;
            if (rawScroll <= 0f) {
                playheadInFrame = Math.round(playheadInTexture);
            } else if (scrollOffset >= maxScroll) {
                playheadInFrame = Math.round(playheadInTexture - scrollOffset);
            } else {
                playheadInFrame = PLAYHEAD_CENTER;
            }
            playheadInFrame = Math.max(0, Math.min(WAVEFORM_W, playheadInFrame));

            Identifier bgTex = getWaveformBackgroundTexture(disc);
            if (bgTex != null) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, bgTex,
                    gx + WAVEFORM_X, gy + WAVEFORM_Y,
                    scrollOffset, 0f,
                    WAVEFORM_W, WAVEFORM_H,
                    texW, WAVEFORM_H);
            }

            Identifier fgTex = getWaveformTexture(disc);
            if (fgTex != null && playheadInFrame > 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, fgTex,
                    gx + WAVEFORM_X, gy + WAVEFORM_Y,
                    scrollOffset, 0f,
                    playheadInFrame, WAVEFORM_H,
                    texW, WAVEFORM_H);
            }
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

        JukeboxThemeUtils.renderThemeButton(graphics, gx, gy, TEX_THEME_4);
    }

    private String resolveDiscName(ItemStack disc) {
        String itemPath = disc.getItem().toString();
        for (String name : JukeboxThemeUtils.DISC_NAMES) {
            if (itemPath.contains(name)) return name;
        }
        return null;
    }

    private Identifier getWaveformTexture(ItemStack disc) {
        String itemPath = disc.getItem().toString();
        for (String name : JukeboxThemeUtils.DISC_NAMES) {
            if (itemPath.contains(name)) {
                return Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID,
                    "textures/gui/theme_4/" + name + "_waveform.png");
            }
        }
        return null;
    }

    private Identifier getWaveformBackgroundTexture(ItemStack disc) {
        String itemPath = disc.getItem().toString();
        for (String name : JukeboxThemeUtils.DISC_NAMES) {
            if (itemPath.contains(name)) {
                return Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID,
                    "textures/gui/theme_4/" + name + "_waveform_background.png");
            }
        }
        return null;
    }
}