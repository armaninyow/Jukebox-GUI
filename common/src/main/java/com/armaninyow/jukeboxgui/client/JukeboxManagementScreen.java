package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import com.armaninyow.jukeboxgui.network.JukeboxGuiPacket;
import com.armaninyow.jukeboxgui.network.JukeboxGuiRefreshPacket;
import com.armaninyow.jukeboxgui.screen.JukeboxScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.armaninyow.jukeboxgui.client.DiscRotationRenderState;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class JukeboxManagementScreen extends AbstractContainerScreen<JukeboxScreenHandler> {

    // ── Textures ─────────────────────────────────────────────────────────
    private static final Identifier CONTAINER_TEXTURE =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_1/jukebox_container.png");
    private static final Identifier CONTAINER_TEXTURE_2 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_2/jukebox_container_2.png");
    private static final Identifier PROGRESS_BG_TEXTURE =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_1/progress_background.png");

    private static final Identifier TEX_STYLUS =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_2/diamond_stylus.png");

    private static final Identifier TEX_DISC_BG =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_2/disc_background.png");

    // ── Theme toggle button textures (8x8) ───────────────────────────────
    private static final Identifier TEX_THEME_1 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_1/theme_1.png");
    private static final Identifier TEX_THEME_2 =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/theme_2/theme_2.png");
    private static final int THEME_BTN_X = 161;
    private static final int THEME_BTN_Y = 6;
    private static final int THEME_BTN_SIZE = 8;

    // ── Play/Pause button textures ────────────────────────────────────────
    private static final Identifier TEX_PAUSE     = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/pause_button.png");
    private static final Identifier TEX_PAUSE_HOV = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/pause_button_highlighted.png");
    private static final Identifier TEX_PLAY      = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/play_button.png");
    private static final Identifier TEX_PLAY_HOV  = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/play_button_highlighted.png");
    private static final Identifier TEX_NO_DISC   = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "textures/gui/no_disc.png");

    // ── Layout — Theme 1 ─────────────────────────────────────────────────
    private static final int BG_WIDTH  = 176;
    private static final int BG_HEIGHT = 166;
    private static final int TITLE_Y   = 38;
    private static final int TITLE_COLOR = 0xFF000000;
    private static final int PROG_BG_X = 45, PROG_BG_Y = 49, PROG_BG_W = 86, PROG_BG_H = 5;
    private static final int PROG_X    = 46, PROG_Y    = 50, PROG_W    = 84, PROG_H    = 3;
    private static final int CLOCK_COLOR = 0xFF3F3F3F;
    private static final int CLOCK_Y   = 60;
    private static final long REFRESH_INTERVAL_MS = 1000;

    // ── Layout — Theme 1 play/pause button ───────────────────────────────
    private static final int BTN_SIZE  = 14;
    private static final int BTN_REL_X = 81;
    private static final int BTN_REL_Y = 57;

    // ── Layout — Theme 2 ─────────────────────────────────────────────────
    private static final int T2_STYLUS_X    = 141;
    private static final int T2_STYLUS_Y    = 61;
    private static final int T2_STYLUS_SIZE = 9;
    // Text
    private static final int T2_TITLE_X  = 8;
    private static final int T2_TITLE_Y  = 25;
    private static final int T2_ARTIST_X = 8;
    private static final int T2_ARTIST_Y = 37;
    // Play/Pause button
    private static final int T2_BTN_X   = 8;
    private static final int T2_BTN_Y   = 49;
    // Clock
    private static final int T2_ELAPSED_X = 25;
    private static final int T2_ELAPSED_Y = 52;
    private static final int T2_TOTAL_X   = 57;
    private static final int T2_TOTAL_Y   = 52;
    // Disc slot
    private static final int T2_DISC_X   = 123;
    private static final int T2_DISC_Y   = 21;
    private static final int T2_DISC_SIZE = 45;

    // ── Disc name list ────────────────────────────────────────────────────
    private static final String[] DISC_NAMES = {
        "11", "13", "5", "blocks", "cat", "chirp",
        "creator_music_box", "creator", "far", "lava_chicken",
        "mall", "mellohi", "otherside", "pigstep", "precipice",
        "relic", "stal", "strad", "tears", "wait", "ward"
    };

    // ── Config persistence ────────────────────────────────────────────────
    private static final Gson GSON = new Gson();
    private static int currentTheme = 1; // 1 or 2, persisted to disk
    private static boolean themeConfigLoaded = false;

    private static void ensureThemeConfigLoaded() {
        if (themeConfigLoaded) return;
        themeConfigLoaded = true;
        try {
            File f = new File(Minecraft.getInstance().gameDirectory, "config/jukeboxgui.json");
            if (!f.exists()) return;
            try (FileReader r = new FileReader(f)) {
                JsonObject obj = GSON.fromJson(r, JsonObject.class);
                if (obj != null && obj.has("theme")) {
                    int t = obj.get("theme").getAsInt();
                    currentTheme = (t == 2) ? 2 : 1;
                }
            }
        } catch (Exception ignored) {}
    }

    private static void saveThemeConfig() {
        try {
            File f = new File(Minecraft.getInstance().gameDirectory, "config/jukeboxgui.json");
            f.getParentFile().mkdirs();
            JsonObject obj = new JsonObject();
            obj.addProperty("theme", currentTheme);
            try (FileWriter w = new FileWriter(f)) {
                GSON.toJson(obj, w);
            }
        } catch (IOException ignored) {}
    }

    // ── Static persistence ────────────────────────────────────────────────
    private static final Map<BlockPos, Float> persistentElapsedTimes = new HashMap<>();
    private static final Map<BlockPos, Long>  guiCloseTimes          = new HashMap<>();
    private static final java.util.Set<BlockPos> finishedPositions   = new java.util.HashSet<>();
    public  static final java.util.Set<BlockPos> pausedPositions     = new java.util.HashSet<>();
    public  static final Map<BlockPos, Float>    pausedElapsedMap    = new HashMap<>();
    public  static final java.util.Set<BlockPos> clientPlayedPositions = new java.util.HashSet<>();
    private static long worldJoinTime = 0L;

    public static boolean hasAnyPausedPosition() { return !pausedPositions.isEmpty(); }

    public static void clearPersistentData() {
        persistentElapsedTimes.clear();
        guiCloseTimes.clear();
        finishedPositions.clear();
        pausedPositions.clear();
        pausedElapsedMap.clear();
        clientPlayedPositions.clear();
        worldJoinTime = System.currentTimeMillis();
    }

    // ── Instance state ────────────────────────────────────────────────────
    private final BlockPos pos;
    private boolean isPlaying    = false;
    private long    tickCount    = 0;
    private String  songTitle    = "";   // full "Artist - Title"
    private float   totalSeconds = 0f;
    private long    timerStartTime       = 0;
    private boolean songFinished         = false;
    private boolean initialDetectionDone = false;
    private ItemStack previousDisc = ItemStack.EMPTY;
    private long lastRefreshTime   = 0;
    private boolean clientSoundActive = false;
    private boolean pendingRestart = false;

    // ── Stylus state (Theme 2) ───────────────────────────────────────────
    private int  stylusYOffset  = 0;
    private long lastStylusTick = 0;

    // ── Disc rotation state (Theme 2): continuous angle = (elapsed/total)*360

    // ── Constructor ────────────────────────────────────────────────────────
    public JukeboxManagementScreen(JukeboxScreenHandler handler,
                                   Inventory playerInventory,
                                   Component title) {
        super(handler, playerInventory, title, BG_WIDTH, BG_HEIGHT);
        this.pos = handler.getPos();
    }

    public BlockPos getPos() { return pos; }

    private boolean isClientPaused() { return pausedPositions.contains(pos); }
    private float getPausedElapsed() { return pausedElapsedMap.getOrDefault(pos, 0f); }
    private void setClientPaused(boolean paused, float elapsed) {
        if (paused) { pausedPositions.add(pos); pausedElapsedMap.put(pos, elapsed); }
        else        { pausedPositions.remove(pos); pausedElapsedMap.remove(pos); }
    }

    // ── Split "Artist - Title" ────────────────────────────────────────────
    /** Returns [title, artist] split from "Artist - Title", or [songTitle, ""] if no separator. */
    private String[] splitSongTitle(String full) {
        int idx = full.indexOf(" - ");
        if (idx >= 0) {
            String artist = full.substring(0, idx);
            String title  = full.substring(idx + 3);
            return new String[]{ title, artist };
        }
        return new String[]{ full, "" };
    }

    // ── Data update from server ───────────────────────────────────────────
    public void updateData(JukeboxGuiPacket.Payload payload) {
        this.isPlaying    = payload.isPlaying();
        this.tickCount    = payload.tickCount();
        this.songTitle    = payload.songTitle();
        this.totalSeconds = payload.totalSeconds();
        boolean serverFinished = payload.isFinished();

        ItemStack incoming = payload.discStack().orElse(ItemStack.EMPTY);
        boolean discChanged = !ItemStack.isSameItem(incoming, previousDisc);

        if (incoming.isEmpty() || (discChanged && initialDetectionDone
                && !incoming.getItem().equals(previousDisc.getItem()))) {
            if (isClientPaused()) {
                Minecraft.getInstance().getSoundManager().resume();
                setClientPaused(false, 0f);
            }
            clientSoundActive = false;
        }

        if (!isPlaying) {
            if (!isClientPaused()) clientSoundActive = false;
        } else if (!initialDetectionDone) {
            // handled below
        } else {
            clientSoundActive = (timerStartTime > 0 && !pendingRestart && clientPlayedPositions.contains(pos));
        }

        if (incoming.isEmpty()) {
            timerStartTime = 0;
            persistentElapsedTimes.remove(pos);
            guiCloseTimes.remove(pos);
        } else if (discChanged && initialDetectionDone
                && !incoming.getItem().equals(previousDisc.getItem())) {
            timerStartTime = System.currentTimeMillis();
            persistentElapsedTimes.remove(pos);
            guiCloseTimes.remove(pos);
        } else if (timerStartTime == 0 && !pendingRestart) {
            Float saved  = persistentElapsedTimes.get(pos);
            Long  closed = guiCloseTimes.get(pos);
            if (saved != null && closed != null) {
                persistentElapsedTimes.remove(pos);
                guiCloseTimes.remove(pos);
                if (saved <= 0f) {
                    float frozenElapsed = -saved;
                    timerStartTime = System.currentTimeMillis() - (long)(frozenElapsed * 1000);
                } else {
                    float serverElapsed = tickCount > 0 ? tickCount / 20f : 0f;
                    long songStart = System.currentTimeMillis() - (long)(serverElapsed * 1000);
                    if (songStart > closed) {
                        if (songStart >= worldJoinTime) timerStartTime = songStart;
                    } else {
                        long passed = System.currentTimeMillis() - closed;
                        float total = saved + (passed / 1000f);
                        timerStartTime = System.currentTimeMillis() - (long)(total * 1000);
                    }
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

        if (!incoming.isEmpty() && !isPlaying && timerStartTime > 0 && !songFinished && !pendingRestart) {
            timerStartTime = System.currentTimeMillis() - (long)(totalSeconds * 1000);
            songFinished = true;
        }
        if (pendingRestart && isPlaying) {
            pendingRestart = false;
            songFinished = false;
            timerStartTime = 0;
            finishedPositions.remove(pos);
            clientPlayedPositions.add(pos);
            clientSoundActive = true;
        }
        if (incoming.isEmpty() || (discChanged && initialDetectionDone
                && !incoming.getItem().equals(previousDisc.getItem()))) {
            songFinished = false;
        }
        if (!initialDetectionDone) {
            initialDetectionDone = true;
            if (finishedPositions.contains(pos)) songFinished = true;
            if (isPlaying) clientSoundActive = clientPlayedPositions.contains(pos);
        }

        previousDisc = incoming.isEmpty() ? ItemStack.EMPTY : incoming.copy();
        if (discChanged)
            this.menu.slots.get(0).set(incoming.isEmpty() ? ItemStack.EMPTY : incoming.copy());
    }

    // ── Init ──────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        super.init();
        ensureThemeConfigLoaded();
        this.titleLabelX     = -1000;
        this.titleLabelY     = -1000;
        this.inventoryLabelX = -1000;
        this.inventoryLabelY = -1000;
    }

    /**
     * In theme 2, skip slot 0 (disc slot) — we draw the 45x45 disc ourselves.
     * extractSlots() iterates menu.slots and calls extractSlot() for each active slot.
     */
    @Override
    protected void extractSlots(net.minecraft.client.gui.GuiGraphicsExtractor graphics,
                                int mouseX, int mouseY) {
        for (net.minecraft.world.inventory.Slot slot : this.menu.slots) {
            if (slot.index == 0 && currentTheme == 2) continue;
            if (slot.isActive()) {
                this.extractSlot(graphics, slot, mouseX, mouseY);
            }
        }
    }

    // ── Close ─────────────────────────────────────────────────────────────
    @Override
    public void onClose() {
        if (isClientPaused()) {
            persistentElapsedTimes.put(pos, -getPausedElapsed());
            guiCloseTimes.put(pos, System.currentTimeMillis());
        } else if (timerStartTime > 0) {
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

    // ── Mouse click ───────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            Minecraft mc = Minecraft.getInstance();
            double mouseX = event.x();
            double mouseY = event.y();

            // ── Theme toggle button ───────────────────────────────────────
            int themeBtnAbsX = this.leftPos + THEME_BTN_X;
            int themeBtnAbsY = this.topPos  + THEME_BTN_Y;
            if (mouseX >= themeBtnAbsX && mouseX < themeBtnAbsX + THEME_BTN_SIZE
             && mouseY >= themeBtnAbsY && mouseY < themeBtnAbsY + THEME_BTN_SIZE) {
                currentTheme = (currentTheme == 1) ? 2 : 1;
                saveThemeConfig();
                return true;
            }

            // ── Theme 2: handle clicks on 45x45 disc area ────────────────
            if (currentTheme == 2) {
                int discAbsX = this.leftPos + T2_DISC_X;
                int discAbsY = this.topPos  + T2_DISC_Y;
                if (mouseX >= discAbsX && mouseX < discAbsX + T2_DISC_SIZE
                 && mouseY >= discAbsY && mouseY < discAbsY + T2_DISC_SIZE) {
                    // Normal slot click — pick up / place / shift-move naturally
                    net.minecraft.world.inventory.Slot discSlot = this.menu.slots.get(0);
                    boolean quickMove = event.hasShiftDown();
                    slotClicked(discSlot, discSlot.index, event.button(),
                        quickMove ? net.minecraft.world.inventory.ContainerInput.QUICK_MOVE
                                  : net.minecraft.world.inventory.ContainerInput.PICKUP);
                    return true;
                }
            }

            // ── Play/Pause button (position depends on theme) ─────────────
            int btnAbsX, btnAbsY, btnSize;
            if (currentTheme == 2) {
                btnAbsX = this.leftPos + T2_BTN_X;
                btnAbsY = this.topPos  + T2_BTN_Y;
                btnSize = BTN_SIZE;
            } else {
                btnAbsX = this.leftPos + BTN_REL_X;
                btnAbsY = this.topPos  + BTN_REL_Y;
                btnSize = BTN_SIZE;
            }

            if (mouseX >= btnAbsX && mouseX < btnAbsX + btnSize
             && mouseY >= btnAbsY && mouseY < btnAbsY + btnSize) {
                ItemStack disc = this.menu.slots.get(0).getItem();
                if (!disc.isEmpty()) {
                    if (pendingRestart) return true;
                    if (isClientPaused()) {
                        boolean channelAlive = JukeboxSoundHelper.isChannelActive(mc, pos);
                        if (!channelAlive || songFinished) {
                            setClientPaused(false, 0f);
                            timerStartTime = 0;
                            clientSoundActive = false;
                            songFinished = false;
                            pendingRestart = true;
                            ClientPlayNetworking.send(new com.armaninyow.jukeboxgui.network.JukeboxGuiActionPacket(
                                com.armaninyow.jukeboxgui.network.JukeboxGuiActionPacket.Action.RESTART_DISC, pos));
                        } else {
                            float savedElapsed = getPausedElapsed();
                            long savedTick = (long)(savedElapsed * 20f);
                            setClientPaused(false, 0f);
                            timerStartTime = System.currentTimeMillis() - (long)(savedElapsed * 1000);
                            JukeboxSoundHelper.unpauseChannel(mc, pos);
                            ClientPlayNetworking.send(new com.armaninyow.jukeboxgui.network.JukeboxGuiPausePacket(
                                false, savedTick, pos));
                        }
                    } else if (clientSoundActive) {
                        boolean channelAlive = JukeboxSoundHelper.isChannelActive(mc, pos);
                        if (channelAlive) {
                            float elapsed = getElapsedSeconds();
                            long currentTick = tickCount;
                            setClientPaused(true, elapsed);
                            JukeboxSoundHelper.pauseChannel(mc, pos);
                            ClientPlayNetworking.send(new com.armaninyow.jukeboxgui.network.JukeboxGuiPausePacket(
                                true, currentTick, pos));
                        } else {
                            timerStartTime = 0;
                            clientSoundActive = false;
                            songFinished = false;
                            pendingRestart = true;
                            ClientPlayNetworking.send(new com.armaninyow.jukeboxgui.network.JukeboxGuiActionPacket(
                                com.armaninyow.jukeboxgui.network.JukeboxGuiActionPacket.Action.RESTART_DISC, pos));
                        }
                    } else {
                        timerStartTime = 0;
                        clientSoundActive = false;
                        songFinished = false;
                        pendingRestart = true;
                        ClientPlayNetworking.send(new com.armaninyow.jukeboxgui.network.JukeboxGuiActionPacket(
                            com.armaninyow.jukeboxgui.network.JukeboxGuiActionPacket.Action.RESTART_DISC, pos));
                    }
                    return true;
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    // ── Disc rotation (Theme 2) ───────────────────────────────────────────
    /**
     * Stepped rotation: angle = floor(elapsed) / total * 360, once per second.
     * Freezes when paused. Resets to 0 at song end.
     */
    private float getDiscAngle() {
        if (totalSeconds <= 0f) return 0f;
        float elapsed = getElapsedSeconds();
        if (songFinished || elapsed >= totalSeconds) return 0f;
        // Step at each whole 1% of playback (like theme 1 progress bar steps by pixels)
        float stepped = (float) Math.floor((elapsed / totalSeconds) * 100f) / 100f;
        return stepped * 360f;
    }

    // ── Render ────────────────────────────────────────────────────────────
    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime >= REFRESH_INTERVAL_MS) {
            ClientPlayNetworking.send(new JukeboxGuiRefreshPacket(pos));
            lastRefreshTime = now;
        }

        if (currentTheme == 2) {
            renderTheme2(graphics, mouseX, mouseY);
        } else {
            renderTheme1(graphics, mouseX, mouseY);
        }

        super.extractContents(graphics, mouseX, mouseY, a);
    }

    // ── Theme 1 render ────────────────────────────────────────────────────
    private void renderTheme1(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int gx = this.leftPos;
        int gy = this.topPos;

        // Background
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

        // Song title (centered)
        if (!songTitle.isEmpty()) {
            int titleX = gx + (BG_WIDTH - this.font.width(songTitle)) / 2;
            if (!isClientPaused() && timerStartTime > 0 && !songFinished) {
                drawRainbowText(graphics, songTitle, titleX, gy + TITLE_Y);
            } else {
                graphics.text(this.font, songTitle, titleX, gy + TITLE_Y, TITLE_COLOR, false);
            }
        } else {
            String insertText = "INSERT DISC";
            int insertX = gx + (BG_WIDTH - this.font.width(insertText)) / 2;
            graphics.text(this.font, insertText, insertX, gy + TITLE_Y, CLOCK_COLOR, false);
        }

        // Clock
        String elapsedStr = formatTime(disc.isEmpty() ? -1 : (int) getElapsedSeconds());
        String totalStr   = formatTime(disc.isEmpty() ? -1 : (int) totalSeconds);
        graphics.text(this.font, elapsedStr,
            gx + PROG_X + 7, gy + CLOCK_Y, CLOCK_COLOR, false);
        int totalW = this.font.width(totalStr);
        graphics.text(this.font, totalStr,
            gx + PROG_X + PROG_W - 6 - totalW, gy + CLOCK_Y, CLOCK_COLOR, false);

        // Play/Pause button
        int btnAbsX = gx + BTN_REL_X;
        int btnAbsY = gy + BTN_REL_Y;
        boolean hovered = mouseX >= btnAbsX && mouseX < btnAbsX + BTN_SIZE
                       && mouseY >= btnAbsY && mouseY < btnAbsY + BTN_SIZE;
        graphics.blit(RenderPipelines.GUI_TEXTURED, getButtonTexture(disc, hovered),
            btnAbsX, btnAbsY, 0f, 0f, BTN_SIZE, BTN_SIZE, BTN_SIZE, BTN_SIZE);

        // Theme toggle button — show "1" since we're on theme 1
        renderThemeButton(graphics, gx, gy, TEX_THEME_1);
    }

    // ── Theme 2 render ────────────────────────────────────────────────────
    private void renderTheme2(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int gx = this.leftPos;
        int gy = this.topPos;

        // Background
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE_2,
            gx, gy, 0f, 0f, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        ItemStack disc = this.menu.slots.get(0).getItem();

        // ── Text: Title / Artist (left-aligned) ──────────────────────────
        boolean playing = !isClientPaused() && timerStartTime > 0 && !songFinished;
        if (!songTitle.isEmpty()) {
            String[] parts = splitSongTitle(songTitle);
            String titleLine  = parts[0]; // Title
            String artistLine = parts[1]; // Artist

            // Title line (with rainbow when playing)
            if (playing) {
                drawRainbowText(graphics, titleLine, gx + T2_TITLE_X, gy + T2_TITLE_Y);
            } else {
                graphics.text(this.font, titleLine, gx + T2_TITLE_X, gy + T2_TITLE_Y, TITLE_COLOR, false);
            }
            // Artist line
            if (!artistLine.isEmpty()) {
                if (playing) {
                    drawRainbowText(graphics, artistLine, gx + T2_ARTIST_X, gy + T2_ARTIST_Y);
                } else {
                    graphics.text(this.font, artistLine, gx + T2_ARTIST_X, gy + T2_ARTIST_Y, TITLE_COLOR, false);
                }
            }
        } else {
            // No disc
            graphics.text(this.font, "INSERT DISC", gx + T2_TITLE_X, gy + T2_TITLE_Y, CLOCK_COLOR, false);
        }

        // ── Play/Pause button ─────────────────────────────────────────────
        int btnAbsX = gx + T2_BTN_X;
        int btnAbsY = gy + T2_BTN_Y;
        boolean btnHovered = mouseX >= btnAbsX && mouseX < btnAbsX + BTN_SIZE
                          && mouseY >= btnAbsY && mouseY < btnAbsY + BTN_SIZE;
        graphics.blit(RenderPipelines.GUI_TEXTURED, getButtonTexture(disc, btnHovered),
            btnAbsX, btnAbsY, 0f, 0f, BTN_SIZE, BTN_SIZE, BTN_SIZE, BTN_SIZE);

        // ── Clock ─────────────────────────────────────────────────────────
        String elapsedStr = formatTime(disc.isEmpty() ? -1 : (int) getElapsedSeconds());
        String totalStr   = formatTime(disc.isEmpty() ? -1 : (int) totalSeconds);
        graphics.text(this.font, elapsedStr, gx + T2_ELAPSED_X, gy + T2_ELAPSED_Y, CLOCK_COLOR, false);
        graphics.text(this.font, totalStr,   gx + T2_TOTAL_X,   gy + T2_TOTAL_Y,   CLOCK_COLOR, false);

        // ── 45x45 disc slot & rotating disc texture ───────────────────────
        int discAbsX = gx + T2_DISC_X;
        int discAbsY = gy + T2_DISC_Y;

        if (!disc.isEmpty()) {
            Identifier discTex = getDiscTexture(disc);
            if (discTex != null) {
                // Rotate around center of the 45x45 slot
                float angle = getDiscAngle();

                // Build a custom render state that pre-computes rotated vertex positions,
                // clamps them to the 45x45 boundary, and remaps UVs accordingly.
                // This produces genuine warp/morph distortion at the corners.
                DiscRotationRenderState.submit(
                    graphics, discTex,
                    discAbsX, discAbsY, T2_DISC_SIZE, angle);

                // Draw foreground (static, no rotation) on top of the rotating disc
                // Also 15x15 upscaled to 45x45 — use angle=0 so no rotation
                Identifier fgTex = getDiscForegroundTexture(disc);
                if (fgTex != null) {
                    DiscRotationRenderState.submit(
                        graphics, fgTex,
                        discAbsX, discAbsY, T2_DISC_SIZE, 0f);
                }
            } else {
                // Fallback for unrecognized discs: render vanilla 16x16 item centered
                int itemOffX = discAbsX + (T2_DISC_SIZE - 16) / 2;
                int itemOffY = discAbsY + (T2_DISC_SIZE - 16) / 2;
                graphics.item(disc, itemOffX, itemOffY);
                graphics.itemDecorations(this.font, disc, itemOffX, itemOffY, null);
            }

            // Slot hover highlight scaled to 45x45
            if (mouseX >= discAbsX && mouseX < discAbsX + T2_DISC_SIZE
             && mouseY >= discAbsY && mouseY < discAbsY + T2_DISC_SIZE) {
                graphics.fill(discAbsX, discAbsY,
                    discAbsX + T2_DISC_SIZE, discAbsY + T2_DISC_SIZE, 0x80FFFFFF);
            }
            // Draw stylus on top of disc and foreground
            renderStylus(graphics, gx, gy, disc);
        } else {
            // No disc — draw disc_background.png
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEX_DISC_BG,
                discAbsX, discAbsY,
                0f, 0f, T2_DISC_SIZE, T2_DISC_SIZE,
                T2_DISC_SIZE, T2_DISC_SIZE);

            // Hover highlight on empty slot
            if (mouseX >= discAbsX && mouseX < discAbsX + T2_DISC_SIZE
             && mouseY >= discAbsY && mouseY < discAbsY + T2_DISC_SIZE) {
                graphics.fill(discAbsX, discAbsY,
                    discAbsX + T2_DISC_SIZE, discAbsY + T2_DISC_SIZE, 0x80FFFFFF);
            }
            // Draw stylus even with no disc
            renderStylus(graphics, gx, gy, disc);
        }

        // Theme toggle button — show "2" since we're on theme 2
        renderThemeButton(graphics, gx, gy, TEX_THEME_2);
    }

    // ── Stylus render ─────────────────────────────────────────────────────
    private void renderStylus(GuiGraphicsExtractor graphics, int gx, int gy, ItemStack disc) {
        boolean playing = !isClientPaused() && timerStartTime > 0 && !songFinished && !disc.isEmpty();
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

        int stylusAbsX = gx + T2_STYLUS_X;
        int stylusAbsY = gy + T2_STYLUS_Y + stylusYOffset;
        float cx = stylusAbsX + T2_STYLUS_SIZE / 2f;
        float cy = stylusAbsY + T2_STYLUS_SIZE / 2f;

        // Lazy 45 degrees counter-clockwise rotation
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().rotate((float) Math.toRadians(-45));
        graphics.pose().translate(-cx, -cy);

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEX_STYLUS,
            stylusAbsX, stylusAbsY,
            0f, 0f,
            T2_STYLUS_SIZE, T2_STYLUS_SIZE,
            T2_STYLUS_SIZE, T2_STYLUS_SIZE);

        graphics.pose().popMatrix();
    }

    // ── Theme button helper ───────────────────────────────────────────────
    private void renderThemeButton(GuiGraphicsExtractor graphics, int gx, int gy, Identifier tex) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, tex,
            gx + THEME_BTN_X, gy + THEME_BTN_Y,
            0f, 0f, THEME_BTN_SIZE, THEME_BTN_SIZE,
            THEME_BTN_SIZE, THEME_BTN_SIZE);
    }

    // ── Button texture helper ─────────────────────────────────────────────
    private Identifier getButtonTexture(ItemStack disc, boolean hovered) {
        if (disc.isEmpty())       return TEX_NO_DISC;
        if (isClientPaused())     return hovered ? TEX_PLAY_HOV  : TEX_PLAY;
        if (clientSoundActive)    return hovered ? TEX_PAUSE_HOV : TEX_PAUSE;
        return hovered ? TEX_PLAY_HOV : TEX_PLAY;
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private float getElapsedSeconds() {
        if (isClientPaused()) return getPausedElapsed();
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
                    "textures/gui/theme_1/" + name + "_progress.png");
            }
        }
        return null;
    }

    /** Returns the 45x45 disc texture for theme 2, or null if not recognized. */
    private Identifier getDiscTexture(ItemStack disc) {
        String itemPath = disc.getItem().toString();
        for (String name : DISC_NAMES) {
            if (itemPath.contains(name)) {
                return Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID,
                    "textures/gui/theme_2/" + name + "_disc.png");
            }
        }
        return null;
    }

    /** Returns the static 45x45 disc foreground texture for theme 2, or null if not recognized. */
    private Identifier getDiscForegroundTexture(ItemStack disc) {
        String itemPath = disc.getItem().toString();
        for (String name : DISC_NAMES) {
            if (itemPath.contains(name)) {
                return Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID,
                    "textures/gui/theme_2/" + name + "_disc_foreground.png");
            }
        }
        return null;
    }
}