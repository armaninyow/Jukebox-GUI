package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

// "Note row" theme
public class JukeboxTheme8 implements JukeboxTheme {

    private static final Identifier CONTAINER_TEXTURE_8 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_8/jukebox_container_8.png");
    private static final Identifier TEX_NOTE =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_8/note.png");
    private static final Identifier TEX_THEME_8 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_8/theme_8.png");

    private static final int TITLE_Y = 21;

    private static final int BTN_X = 81, BTN_Y = 32;
    private static final int ELAPSED_X = 53, ELAPSED_Y = 35;
    private static final int TOTAL_X   = 98, TOTAL_Y   = 35;

    private static final int DISC_X = 80, DISC_Y = 50, DISC_SIZE = 16;

    private static final int NOTE_W = 5, NOTE_H = 8;
    private static final int NOTES_Y = 56;
    private static final int LEFT_START_X  = 8;
    private static final int RIGHT_START_X = 98;
    private static final int NOTES_PER_SIDE = 14;
    private static final int TOTAL_NOTES = NOTES_PER_SIDE * 2;

    private static final float BOUNCE_AMPLITUDE_PX = 4f;
    private static final float NOTE_PROPAGATION_MS = 60f;
    private static final float BOUNCE_DURATION_MS = 800f;

    private static final float GAME_TICK_MS = 50f;

    private static final float COLOR_TICK_MS = 25f;
    private static final int COLOR_DURATION_TICKS = 30;
    private static final float MUSIC_NOTE_BRIGHTNESS = 1.25f;

    private static final int WHITE_MODIFIED_COLOR = -1644826;
    private static final int[] MUSIC_NOTE_DYE_DIFFUSE = {
        0,
        10329495,
        3847130,
        3949738,
        1481884,
        6192150,
        8439583,
        16701501,
        16351261,
        15961002,
        11546150,
        13061821
    };
    private static final int[] MUSIC_NOTE_MODIFIED_COLORS = buildModifiedColors();

    private static int[] buildModifiedColors() {
        int[] colors = new int[MUSIC_NOTE_DYE_DIFFUSE.length];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = (i == 0) ? WHITE_MODIFIED_COLOR : modifiedColor(MUSIC_NOTE_DYE_DIFFUSE[i]);
        }
        return colors;
    }

    private static int modifiedColor(int diffuse) {
        int r = (diffuse >> 16) & 0xFF;
        int g = (diffuse >> 8) & 0xFF;
        int b = diffuse & 0xFF;
        int rr = (int) Math.floor(r * MUSIC_NOTE_BRIGHTNESS);
        int gg = (int) Math.floor(g * MUSIC_NOTE_BRIGHTNESS);
        int bb = (int) Math.floor(b * MUSIC_NOTE_BRIGHTNESS);
        return (255 << 24) | ((rr & 0xFF) << 16) | ((gg & 0xFF) << 8) | (bb & 0xFF);
    }

    private long lastFrameTimeMs = 0L;
    private float activeTimeMs = 0f;
    private float waveElapsedMs = 0f;

    @Override public int id() { return 8; }
    @Override public Identifier toggleIcon() { return TEX_THEME_8; }
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

        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE_8,
            gx, gy, 0f, 0f, JukeboxManagementScreen.BG_WIDTH, JukeboxManagementScreen.BG_HEIGHT,
            JukeboxManagementScreen.BG_WIDTH, JukeboxManagementScreen.BG_HEIGHT);

        ItemStack disc = screen.getDiscStack();
        boolean playing = !screen.isClientPaused() && screen.timerStartTime > 0 && !screen.songFinished;

        String songTitle = screen.songTitle;
        if (!songTitle.isEmpty()) {
            int titleX = gx + (JukeboxManagementScreen.BG_WIDTH - screen.getFont().width(songTitle)) / 2;
            if (playing) {
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
            graphics.item(disc, discAbsX, discAbsY);
            graphics.itemDecorations(screen.getFont(), disc, discAbsX, discAbsY, null);
        }
        if (mouseX >= discAbsX && mouseX < discAbsX + DISC_SIZE
         && mouseY >= discAbsY && mouseY < discAbsY + DISC_SIZE) {
            graphics.fill(discAbsX, discAbsY, discAbsX + DISC_SIZE, discAbsY + DISC_SIZE, 0x80FFFFFF);
        }

        renderNoteRow(graphics, gx, gy, disc, playing, screen.isClientPaused(),
            screen.getElapsedSeconds(), screen.totalSeconds);

        JukeboxThemeUtils.renderThemeButton(graphics, gx, gy, TEX_THEME_8);
    }

    private void renderNoteRow(GuiGraphicsExtractor graphics, int gx, int gy, ItemStack disc,
                                boolean playing, boolean paused, float elapsed, float total) {
        long now = System.currentTimeMillis();
        if (lastFrameTimeMs == 0L) lastFrameTimeMs = now;
        long deltaMs = now - lastFrameTimeMs;
        lastFrameTimeMs = now;

        if (playing) {
            activeTimeMs += deltaMs;
            waveElapsedMs += deltaMs;
        }

        boolean hasProgress = !disc.isEmpty() && total > 0f;
        double progressRatio = hasProgress ? (elapsed / total) : 0.0;

        int activeCount = hasProgress
            ? Math.min(TOTAL_NOTES, (int) Math.floor(progressRatio * TOTAL_NOTES) + 1)
            : 0;

        if (activeCount > 0) {
            float waitTicks = (activeCount <= 14) ? (15 - activeCount) : 0f;
            float waitMs = waitTicks * GAME_TICK_MS;
            float cycleDurationMs = (activeCount - 1) * NOTE_PROPAGATION_MS + BOUNCE_DURATION_MS + waitMs;
            while (cycleDurationMs > 0f && waveElapsedMs >= cycleDurationMs) {
                waveElapsedMs -= cycleDurationMs;
            }
        } else {
            waveElapsedMs = 0f;
        }

        float colorTick = activeTimeMs / COLOR_TICK_MS;
        int currentColor = activeCount > 0 ? getLerpedNoteColor(colorTick) : 0;

        for (int i = 0; i < TOTAL_NOTES; i++) {
            int side = i / NOTES_PER_SIDE;
            int slot = i % NOTES_PER_SIDE;
            int baseX = (side == 0 ? LEFT_START_X : RIGHT_START_X) + slot * NOTE_W;
            int baseY = NOTES_Y;

            boolean active = i < activeCount;

            int drawX = gx + baseX;
            int drawY = gy + baseY;

            if (active) {
                float localT = waveElapsedMs - i * NOTE_PROPAGATION_MS;
                float riseFraction = 0f;
                if (localT >= 0f && localT <= BOUNCE_DURATION_MS) {
                    riseFraction = 0.5f * (1f - (float) Math.cos(2.0 * Math.PI * (localT / BOUNCE_DURATION_MS)));
                }
                drawY -= Math.round(BOUNCE_AMPLITUDE_PX * riseFraction);

                graphics.blit(RenderPipelines.GUI_TEXTURED, TEX_NOTE,
                    drawX, drawY, 0f, 0f, NOTE_W, NOTE_H, NOTE_W, NOTE_H, currentColor);
            } else {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEX_NOTE,
                    drawX, drawY, 0f, 0f, NOTE_W, NOTE_H, NOTE_W, NOTE_H);
            }
        }
    }

    private static int getLerpedNoteColor(float tick) {
        int tickCount = (int) Math.floor(tick);
        int colorCount = MUSIC_NOTE_MODIFIED_COLORS.length;
        int value = Math.floorDiv(tickCount, COLOR_DURATION_TICKS);
        int c1 = Math.floorMod(value, colorCount);
        int c2 = Math.floorMod(value + 1, colorCount);
        float frac = tick - tickCount;
        float subStep = (Math.floorMod(tickCount, COLOR_DURATION_TICKS) + frac) / (float) COLOR_DURATION_TICKS;
        return srgbLerp(subStep, MUSIC_NOTE_MODIFIED_COLORS[c1], MUSIC_NOTE_MODIFIED_COLORS[c2]);
    }

    private static int srgbLerp(float t, int p0, int p1) {
        int a = lerpInt(t, (p0 >>> 24), (p1 >>> 24));
        int r = lerpInt(t, (p0 >> 16) & 0xFF, (p1 >> 16) & 0xFF);
        int g = lerpInt(t, (p0 >> 8) & 0xFF, (p1 >> 8) & 0xFF);
        int b = lerpInt(t, p0 & 0xFF, p1 & 0xFF);
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    private static int lerpInt(float t, int a, int b) {
        return Math.round(a + t * (b - a));
    }
}