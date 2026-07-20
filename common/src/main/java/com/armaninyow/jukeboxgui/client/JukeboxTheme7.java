package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// "Block face" theme
public class JukeboxTheme7 implements JukeboxTheme {

    private static final Identifier CONTAINER_TEXTURE_7 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_7/jukebox_container_7.png");
    private static final Identifier TEX_JUKEBOX_SIDE =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_7/jukebox_side.png");
    private static final Identifier TEX_NOTE =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_7/note.png");
    private static final Identifier TEX_THEME_7 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_7/theme_7.png");

    private static final int TITLE_X  = 8,  TITLE_Y  = 25;
    private static final int ARTIST_X = 8,  ARTIST_Y = 37;
    private static final int BTN_X    = 8,  BTN_Y    = 49;
    private static final int ELAPSED_X = 25, ELAPSED_Y = 52;
    private static final int TOTAL_X   = 57, TOTAL_Y   = 52;

    private static final int DISC_X = 130, DISC_Y = 34, DISC_SIZE = 32;

    private static final int DISC_POP_X = 138, DISC_POP_Y = 26;

    private static final int NOTE_SIZE = 16;

    private static final float PIXELS_PER_BLOCK   = 32f;
    private static final float INITIAL_VELOCITY_PX = 0.2f * PIXELS_PER_BLOCK;
    private static final float FRICTION            = 0.66f;
    private static final float LIFETIME_TICKS      = 6f;
    private static final float TICK_MS             = 50f;
    private static final int   SPAWN_INTERVAL_TICKS = 20;

    private static final float TWO_PI = (float) (Math.PI * 2);
    private static final Random NOTE_RANDOM = new Random();

    private final List<NoteBlip> blips = new ArrayList<>();
    private long  lastFrameTimeMs = 0L;
    private float tickAccumulator = 0f;
    private long  localTickCount  = 0L;
    private boolean wasPlaying    = false;

    private static final class NoteBlip {
        float ageTicks = 0f;
        float velocityPx;
        float risePx = 0f;
        final int color;
        NoteBlip(int color) {
            this.color = color;
            this.velocityPx = INITIAL_VELOCITY_PX;
        }
    }

    @Override public int id() { return 7; }
    @Override public Identifier toggleIcon() { return TEX_THEME_7; }
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

        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE_7,
            gx, gy, 0f, 0f, JukeboxManagementScreen.BG_WIDTH, JukeboxManagementScreen.BG_HEIGHT,
            JukeboxManagementScreen.BG_WIDTH, JukeboxManagementScreen.BG_HEIGHT);

        ItemStack disc = screen.getDiscStack();

        boolean playing = !screen.isClientPaused() && screen.timerStartTime > 0 && !screen.songFinished && !disc.isEmpty();
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
        boolean hovered = mouseX >= discAbsX && mouseX < discAbsX + DISC_SIZE
                        && mouseY >= discAbsY && mouseY < discAbsY + DISC_SIZE;

        boolean showEjectedDisc = !disc.isEmpty() && (hovered || screen.songFinished);
        if (showEjectedDisc) {
            int popAbsX = gx + DISC_POP_X;
            int popAbsY = gy + DISC_POP_Y;
            graphics.item(disc, popAbsX, popAbsY);
            graphics.itemDecorations(screen.getFont(), disc, popAbsX, popAbsY, null);
        }

        renderNoteParticles(graphics, discAbsX, discAbsY, playing, screen.isClientPaused());

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEX_JUKEBOX_SIDE,
            discAbsX, discAbsY, 0f, 0f, DISC_SIZE, DISC_SIZE, DISC_SIZE, DISC_SIZE);

        if (hovered) {
            graphics.fill(discAbsX, discAbsY, discAbsX + DISC_SIZE, discAbsY + DISC_SIZE, 0x80FFFFFF);
        }

        JukeboxThemeUtils.renderThemeButton(graphics, gx, gy, TEX_THEME_7);
    }

    private void renderNoteParticles(GuiGraphicsExtractor graphics, int slotAbsX, int slotAbsY,
                                      boolean playing, boolean paused) {
        long now = System.currentTimeMillis();
        if (lastFrameTimeMs == 0L) lastFrameTimeMs = now;
        long deltaMs = now - lastFrameTimeMs;
        lastFrameTimeMs = now;

        if (!paused) {
            if (playing && !wasPlaying) {
                localTickCount = 0L;
                tickAccumulator = 0f;
            }
            wasPlaying = playing;

            tickAccumulator += deltaMs / TICK_MS;
            while (tickAccumulator >= 1f) {
                tickAccumulator -= 1f;

                for (NoteBlip blip : blips) {
                    blip.risePx += blip.velocityPx;
                    blip.velocityPx *= FRICTION;
                    blip.ageTicks += 1f;
                }
                blips.removeIf(b -> b.ageTicks >= LIFETIME_TICKS);

                if (playing) {
                    if (localTickCount % SPAWN_INTERVAL_TICKS == 0L) {
                        float note = NOTE_RANDOM.nextInt(4) / 24f;
                        blips.add(new NoteBlip(computeNoteColor(note)));
                    }
                    localTickCount++;
                }
            }
        }

        if (!playing && !paused) {
            wasPlaying = false;
        }

        int anchorX = slotAbsX + DISC_SIZE / 2;
        int anchorY = slotAbsY;

        for (NoteBlip blip : blips) {
            int drawX = anchorX - NOTE_SIZE / 2;
            int drawY = (int) (anchorY - NOTE_SIZE / 2f - blip.risePx);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEX_NOTE,
                drawX, drawY, 0f, 0f, NOTE_SIZE, NOTE_SIZE, NOTE_SIZE, NOTE_SIZE, blip.color);
        }
    }

    /** Exact port of NoteParticle's constructor color formula. */
    private static int computeNoteColor(float note) {
        float r = Math.max(0f, (float) Math.sin((note + 0.0f) * TWO_PI) * 0.65f + 0.35f);
        float g = Math.max(0f, (float) Math.sin((note + 0.33333334f) * TWO_PI) * 0.65f + 0.35f);
        float b = Math.max(0f, (float) Math.sin((note + 0.6666667f) * TWO_PI) * 0.65f + 0.35f);
        int ri = Math.round(r * 255f);
        int gi = Math.round(g * 255f);
        int bi = Math.round(b * 255f);
        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }
}