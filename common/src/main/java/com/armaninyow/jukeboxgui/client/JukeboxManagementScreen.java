package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.network.JukeboxGuiActionPacket;
import com.armaninyow.jukeboxgui.network.JukeboxGuiPacket;
import com.armaninyow.jukeboxgui.network.JukeboxGuiPausePacket;
import com.armaninyow.jukeboxgui.network.JukeboxGuiRefreshPacket;
import com.armaninyow.jukeboxgui.screen.JukeboxScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class JukeboxManagementScreen extends AbstractContainerScreen<JukeboxScreenHandler> {

    public static final int BG_WIDTH  = 176;
    public static final int BG_HEIGHT = 166;
    private static final long REFRESH_INTERVAL_MS = 1000;

    private static final Map<BlockPos, Float> persistentElapsedTimes = new HashMap<>();
    private static final Map<BlockPos, Long>  guiCloseTimes          = new HashMap<>();
    private static final Set<BlockPos> finishedPositions             = new HashSet<>();
    public  static final Set<BlockPos> pausedPositions                = new HashSet<>();
    public  static final Map<BlockPos, Float> pausedElapsedMap        = new HashMap<>();
    public  static final Set<BlockPos> clientPlayedPositions          = new HashSet<>();
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

    private final BlockPos pos;
    private boolean isPlaying    = false;
    private long    tickCount    = 0;
    String  songTitle    = "";
    float   totalSeconds = 0f;
    long    timerStartTime       = 0;
    boolean songFinished         = false;
    private boolean initialDetectionDone = false;
    private ItemStack previousDisc = ItemStack.EMPTY;
    private long lastRefreshTime   = 0;
    boolean clientSoundActive = false;
    private boolean pendingRestart = false;

    private JukeboxTheme activeTheme;

    public JukeboxManagementScreen(JukeboxScreenHandler handler,
                                   Inventory playerInventory,
                                   Component title) {
        super(handler, playerInventory, title, BG_WIDTH, BG_HEIGHT);
        this.pos = handler.getPos();
    }

    public BlockPos getPos() { return pos; }

    ItemStack getDiscStack() { return this.menu.slots.get(0).getItem(); }

    int getGuiLeft() { return this.leftPos; }
    int getGuiTop() { return this.topPos; }

    void clickDiscSlot(MouseButtonEvent event) {
        Slot discSlot = this.menu.slots.get(0);
        boolean quickMove = event.hasShiftDown();
        this.slotClicked(discSlot, discSlot.index, event.button(),
            quickMove ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP);
    }

    boolean isClientPaused() { return pausedPositions.contains(pos); }
    float getPausedElapsed() { return pausedElapsedMap.getOrDefault(pos, 0f); }
    private void setClientPaused(boolean paused, float elapsed) {
        if (paused) { pausedPositions.add(pos); pausedElapsedMap.put(pos, elapsed); }
        else        { pausedPositions.remove(pos); pausedElapsedMap.remove(pos); }
    }

    float getElapsedSeconds() {
        if (isClientPaused()) return getPausedElapsed();
        if (timerStartTime == 0) return 0f;
        return Math.min((System.currentTimeMillis() - timerStartTime) / 1000f, totalSeconds);
    }

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

    @Override
    protected void init() {
        super.init();
        activeTheme = JukeboxThemeRegistry.current();
        this.titleLabelX     = -1000;
        this.titleLabelY     = -1000;
        this.inventoryLabelX = -1000;
        this.inventoryLabelY = -1000;
    }

    @Override
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (Slot slot : this.menu.slots) {
            if (activeTheme.skipSlot(slot.index)) continue;
            if (slot.isActive()) {
                this.extractSlot(graphics, slot, mouseX, mouseY);
            }
        }
    }

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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        int themeBtnAbsX = this.leftPos + JukeboxThemeUtils.THEME_BTN_X;
        int themeBtnAbsY = this.topPos  + JukeboxThemeUtils.THEME_BTN_Y;
        if (mouseX >= themeBtnAbsX && mouseX < themeBtnAbsX + JukeboxThemeUtils.THEME_BTN_SIZE
         && mouseY >= themeBtnAbsY && mouseY < themeBtnAbsY + JukeboxThemeUtils.THEME_BTN_SIZE) {
            if (event.button() == 0) {
                JukeboxThemeRegistry.cycleTheme();
                activeTheme = JukeboxThemeRegistry.current();
                return true;
            } else if (event.button() == 1) {
                JukeboxThemeRegistry.cycleThemeBackward();
                activeTheme = JukeboxThemeRegistry.current();
                return true;
            }
        }

        if (event.button() == 0) {
            Minecraft mc = Minecraft.getInstance();

            if (activeTheme.handleClick(this, event)) {
                return true;
            }

            int btnAbsX = this.leftPos + activeTheme.playPauseBtnX();
            int btnAbsY = this.topPos  + activeTheme.playPauseBtnY();
            int btnSize = activeTheme.playPauseBtnSize();

            if (mouseX >= btnAbsX && mouseX < btnAbsX + btnSize
             && mouseY >= btnAbsY && mouseY < btnAbsY + btnSize) {
                ItemStack disc = getDiscStack();
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
                            ClientPlayNetworking.send(new JukeboxGuiActionPacket(
                                JukeboxGuiActionPacket.Action.RESTART_DISC, pos));
                        } else {
                            float savedElapsed = getPausedElapsed();
                            long savedTick = (long)(savedElapsed * 20f);
                            setClientPaused(false, 0f);
                            timerStartTime = System.currentTimeMillis() - (long)(savedElapsed * 1000);
                            JukeboxSoundHelper.unpauseChannel(mc, pos);
                            ClientPlayNetworking.send(new JukeboxGuiPausePacket(
                                false, savedTick, pos));
                        }
                    } else if (clientSoundActive) {
                        boolean channelAlive = JukeboxSoundHelper.isChannelActive(mc, pos);
                        if (channelAlive) {
                            float elapsed = getElapsedSeconds();
                            long currentTick = tickCount;
                            setClientPaused(true, elapsed);
                            JukeboxSoundHelper.pauseChannel(mc, pos);
                            ClientPlayNetworking.send(new JukeboxGuiPausePacket(
                                true, currentTick, pos));
                        } else {
                            timerStartTime = 0;
                            clientSoundActive = false;
                            songFinished = false;
                            pendingRestart = true;
                            ClientPlayNetworking.send(new JukeboxGuiActionPacket(
                                JukeboxGuiActionPacket.Action.RESTART_DISC, pos));
                        }
                    } else {
                        timerStartTime = 0;
                        clientSoundActive = false;
                        songFinished = false;
                        pendingRestart = true;
                        ClientPlayNetworking.send(new JukeboxGuiActionPacket(
                            JukeboxGuiActionPacket.Action.RESTART_DISC, pos));
                    }
                    return true;
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime >= REFRESH_INTERVAL_MS) {
            ClientPlayNetworking.send(new JukeboxGuiRefreshPacket(pos));
            lastRefreshTime = now;
        }

        activeTheme.render(this, graphics, mouseX, mouseY);

        super.extractContents(graphics, mouseX, mouseY, a);
    }
}