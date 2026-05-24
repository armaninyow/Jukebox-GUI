package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import com.armaninyow.jukeboxgui.network.JukeboxGuiPacket;
import com.armaninyow.jukeboxgui.network.JukeboxGuiRefreshPacket;
import com.armaninyow.jukeboxgui.screen.JukeboxScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

// 26.1.x — GuiGraphics is now GuiGraphicsExtractor; ResourceLocation is now Identifier
@Environment(EnvType.CLIENT)
public class JukeboxManagementScreen extends AbstractContainerScreen<JukeboxScreenHandler> {

    // ── Textures ─────────────────────────────────────────────────────────
    private static final Identifier CONTAINER_TEXTURE =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/jukebox_container.png");
    private static final Identifier PROGRESS_BG_TEXTURE =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/progress_background.png");

    // ── Layout ────────────────────────────────────────────────────────────
    private static final int BG_WIDTH  = 176;
    private static final int BG_HEIGHT = 166;
    private static final int TITLE_Y   = 38;
    private static final int TITLE_COLOR  = 0xFF000000;
    private static final int PROG_BG_X = 45, PROG_BG_Y = 49, PROG_BG_W = 86, PROG_BG_H = 5;
    private static final int PROG_X    = 46, PROG_Y    = 50, PROG_W    = 84, PROG_H    = 3;
    private static final int CLOCK_COLOR = 0xFF3F3F3F;
    private static final int CLOCK_Y   = 58;
    private static final long REFRESH_INTERVAL_MS = 1000;

    private static final String[] DISC_NAMES = {
        "11", "13", "5", "blocks", "cat", "chirp",
        "creator_music_box", "creator", "far", "lava_chicken",
        "mall", "mellohi", "otherside", "pigstep", "precipice",
        "relic", "stal", "strad", "tears", "wait", "ward"
    };

    // ── Static persistence ────────────────────────────────────────────────
    private static final Map<BlockPos, Float> persistentElapsedTimes = new HashMap<>();
    private static final Map<BlockPos, Long>  guiCloseTimes          = new HashMap<>();
    private static final java.util.Set<BlockPos> finishedPositions   = new java.util.HashSet<>();
    private static long worldJoinTime = 0L;

    public static void clearPersistentData() {
        persistentElapsedTimes.clear();
        guiCloseTimes.clear();
        finishedPositions.clear();
        worldJoinTime = System.currentTimeMillis();
    }

    // ── Instance state ────────────────────────────────────────────────────
    private final BlockPos pos;
    private boolean isPlaying    = false;
    private long    tickCount    = 0;
    private String  songTitle    = "";
    private float   totalSeconds = 0f;
    private long    timerStartTime       = 0;
    private boolean songFinished         = false;
    private boolean initialDetectionDone = false;
    private ItemStack previousDisc = ItemStack.EMPTY;
    private long lastRefreshTime   = 0;

    // ── Constructor — imageWidth/imageHeight are final, pass via super ────
    public JukeboxManagementScreen(JukeboxScreenHandler handler,
                                   Inventory playerInventory,
                                   Component title) {
        super(handler, playerInventory, title, BG_WIDTH, BG_HEIGHT);
        this.pos = handler.getPos();
    }

    public BlockPos getPos() { return pos; }

    // ── Data update from server ───────────────────────────────────────────
    public void updateData(JukeboxGuiPacket.Payload payload) {
        this.isPlaying    = payload.isPlaying();
        this.tickCount    = payload.tickCount();
        this.songTitle    = payload.songTitle();
        this.totalSeconds = payload.totalSeconds();
        boolean serverFinished = payload.isFinished();

        ItemStack incoming = payload.discStack().orElse(ItemStack.EMPTY);
        boolean discChanged = !ItemStack.isSameItem(incoming, previousDisc);

        if (incoming.isEmpty()) {
            timerStartTime = 0;
            persistentElapsedTimes.remove(pos);
            guiCloseTimes.remove(pos);
        } else if (discChanged && initialDetectionDone
                && !incoming.getItem().equals(previousDisc.getItem())) {
            timerStartTime = System.currentTimeMillis();
            persistentElapsedTimes.remove(pos);
            guiCloseTimes.remove(pos);
        } else if (timerStartTime == 0) {
            Float saved  = persistentElapsedTimes.get(pos);
            Long  closed = guiCloseTimes.get(pos);
            if (saved != null && closed != null) {
                persistentElapsedTimes.remove(pos);
                guiCloseTimes.remove(pos);
                float serverElapsed = tickCount > 0 ? tickCount / 20f : 0f;
                long songStart = System.currentTimeMillis() - (long)(serverElapsed * 1000);
                if (songStart > closed) {
                    if (songStart >= worldJoinTime) timerStartTime = songStart;
                } else {
                    long passed = System.currentTimeMillis() - closed;
                    float total = saved + (passed / 1000f);
                    timerStartTime = System.currentTimeMillis() - (long)(total * 1000);
                }
            } else if (!isPlaying) {
                if (totalSeconds > 0 && (songFinished || finishedPositions.contains(pos) || serverFinished)) {
                    timerStartTime = System.currentTimeMillis() - (long)(totalSeconds * 1000);
                    songFinished = true;
                    finishedPositions.add(pos);
                }
            } else {
                float serverElapsed = tickCount > 0 ? tickCount / 20f : 0f;
                long songStart = System.currentTimeMillis() - (long)(serverElapsed * 1000);
                if (songStart >= worldJoinTime) timerStartTime = songStart;
            }
        }

        if (!incoming.isEmpty() && !isPlaying && timerStartTime > 0 && !songFinished) {
            timerStartTime = System.currentTimeMillis() - (long)(totalSeconds * 1000);
            songFinished = true;
        }
        if (incoming.isEmpty() || (discChanged && initialDetectionDone
                && !incoming.getItem().equals(previousDisc.getItem()))) {
            songFinished = false;
        }
        if (!initialDetectionDone) {
            initialDetectionDone = true;
            if (finishedPositions.contains(pos)) songFinished = true;
        }

        previousDisc = incoming.isEmpty() ? ItemStack.EMPTY : incoming.copy();
        if (discChanged)
            this.menu.slots.get(0).set(incoming.isEmpty() ? ItemStack.EMPTY : incoming.copy());
    }

    // ── Init ──────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        super.init();
        // Hide vanilla labels by moving them off-screen
        this.titleLabelX     = -1000;
        this.titleLabelY     = -1000;
        this.inventoryLabelX = -1000;
        this.inventoryLabelY = -1000;
    }

    // ── Close ─────────────────────────────────────────────────────────────
    @Override
    public void onClose() {
        if (timerStartTime > 0) {
            persistentElapsedTimes.put(pos, getElapsedSeconds());
            guiCloseTimes.put(pos, System.currentTimeMillis());
        } else {
            persistentElapsedTimes.remove(pos);
            guiCloseTimes.remove(pos);
        }
        if (songFinished) finishedPositions.add(pos);
        else              finishedPositions.remove(pos);
        super.onClose();
    }

    // ── Render — 26.1 uses extractRenderState instead of render ──────────
    // AbstractContainerScreen overrides extractRenderState (not render).
    // We override extractContents to inject our background drawing.
    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // Refresh ticker
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime >= REFRESH_INTERVAL_MS) {
            ClientPlayNetworking.send(new JukeboxGuiRefreshPacket(pos));
            lastRefreshTime = now;
        }

        int gx = this.leftPos;
        int gy = this.topPos;

        // Main container background
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE,
            gx, gy, 0f, 0f, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        // Progress bar background
        graphics.blit(RenderPipelines.GUI_TEXTURED, PROGRESS_BG_TEXTURE,
            gx + PROG_BG_X, gy + PROG_BG_Y,
            0f, 0f, PROG_BG_W, PROG_BG_H, PROG_BG_W, PROG_BG_H);

        // Progress bar foreground
        ItemStack disc = this.menu.slots.get(0).getItem();
        if (!disc.isEmpty() && totalSeconds > 0) {
            Identifier progTex = getProgressTexture(disc);
            if (progTex != null) {
                float ratio = Math.min(1f, getElapsedSeconds() / totalSeconds);
                int progWidth = Math.max(0, Math.round(ratio * PROG_W));
                if (progWidth > 0) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, progTex,
                        gx + PROG_X, gy + PROG_Y,
                        0f, 0f, progWidth, PROG_H, PROG_W, PROG_H);
                }
            }
        }

        // Song title
        if (!songTitle.isEmpty()) {
            int titleX = gx + (BG_WIDTH - this.font.width(songTitle)) / 2;
            if (timerStartTime > 0 && !songFinished) {
                drawRainbowText(graphics, songTitle, titleX, gy + TITLE_Y);
            } else {
                graphics.text(this.font, songTitle, titleX, gy + TITLE_Y, TITLE_COLOR, false);
            }
        } else {
            String insertText = "INSERT DISC";
            int insertX = gx + (BG_WIDTH - this.font.width(insertText)) / 2;
            graphics.text(this.font, insertText, insertX, gy + TITLE_Y, CLOCK_COLOR, false);
        }

        // Clock display
        String elapsedStr = formatTime(disc.isEmpty() ? -1 : (int) getElapsedSeconds());
        String totalStr   = formatTime(disc.isEmpty() ? -1 : (int) totalSeconds);
        graphics.text(this.font, elapsedStr,
            gx + PROG_X + 14, gy + CLOCK_Y, CLOCK_COLOR, false);
        int totalW = this.font.width(totalStr);
        graphics.text(this.font, totalStr,
            gx + PROG_X + PROG_W - 12 - totalW, gy + CLOCK_Y, CLOCK_COLOR, false);

        // Let super draw slots on top of our background
        super.extractContents(graphics, mouseX, mouseY, a);
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private float getElapsedSeconds() {
        if (timerStartTime == 0) return 0f;
        return Math.min((System.currentTimeMillis() - timerStartTime) / 1000f, totalSeconds);
    }

    private static String formatTime(int totalSec) {
        if (totalSec < 0) return "xx:xx";
        return String.format("%02d:%02d", totalSec / 60, totalSec % 60);
    }

    private void drawRainbowText(GuiGraphicsExtractor graphics, String text, int x, int y) {
        if (text.isEmpty()) return;
        float hue = (System.currentTimeMillis() % 4000L) / 4000f;
        int rgb = hsbToRgb(hue) | 0xFF000000;
        graphics.text(this.font, text, x, y, rgb, false);
    }

    private static int hsbToRgb(float hue) {
        float h = hue * 6f;
        int sector = (int) h;
        float frac = h - sector;
        float q = 1f - frac;
        float r, g, b;
        switch (sector % 6) {
            case 0 -> { r = 1f;   g = frac; b = 0f;   }
            case 1 -> { r = q;    g = 1f;   b = 0f;   }
            case 2 -> { r = 0f;   g = 1f;   b = frac; }
            case 3 -> { r = 0f;   g = q;    b = 1f;   }
            case 4 -> { r = frac; g = 0f;   b = 1f;   }
            default-> { r = 1f;   g = 0f;   b = q;    }
        }
        return ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    private Identifier getProgressTexture(ItemStack disc) {
        String itemPath = disc.getItem().toString();
        for (String name : DISC_NAMES) {
            if (itemPath.contains(name)) {
                return Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID,
                    "textures/gui/" + name + "_progress.png");
            }
        }
        return null;
    }
}
