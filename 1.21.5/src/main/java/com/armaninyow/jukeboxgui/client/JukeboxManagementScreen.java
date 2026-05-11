package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import com.armaninyow.jukeboxgui.network.JukeboxGuiActionPacket;
import com.armaninyow.jukeboxgui.network.JukeboxGuiPacket;
import com.armaninyow.jukeboxgui.network.JukeboxGuiRefreshPacket;
import com.armaninyow.jukeboxgui.screen.JukeboxScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.HashMap;

// 1.21.5
@Environment(EnvType.CLIENT)
public class JukeboxManagementScreen extends HandledScreen<JukeboxScreenHandler> {

	// ── Textures ────────────────────────────────────────────────────────────
	private static final Identifier CONTAINER_TEXTURE =
		Identifier.of(JukeboxGUI.MOD_ID, "textures/gui/jukebox_container.png");

	private static final Identifier PROGRESS_BG_TEXTURE =
		Identifier.of(JukeboxGUI.MOD_ID, "textures/gui/progress_background.png");

	// Vanilla slot highlight sprites (built-in atlas, no local PNGs needed)
	private static final Identifier SLOT_HIGHLIGHT_BACK =
		Identifier.of("minecraft", "container/slot_highlight_back");
	private static final Identifier SLOT_HIGHLIGHT_FRONT =
		Identifier.of("minecraft", "container/slot_highlight_front");


    /** Progress bar textures keyed by disc item path (e.g. "cat" → cat_progress.png) */
    private static final String[] DISC_NAMES = {
            "11", "13", "5", "blocks", "cat", "chirp",
            "creator_music_box", "creator", "far", "lava_chicken",
            "mall", "mellohi", "otherside", "pigstep", "precipice",
            "relic", "stal", "strad", "tears", "wait", "ward"
    };

    // ── Layout constants (all in GUI pixels, matching jukebox_container.png) ─
    // Container size: 176×166
    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 166;

    // Disc slot: x=80 y=17  (16×16) — matches ScreenHandler slot registration
    private static final int DISC_SLOT_X = 80;
    private static final int DISC_SLOT_Y = 17;

    // Song title text: centered above progress bar, baseline y=38 (moved 1px down from 37)
    private static final int TITLE_Y = 38;
    private static final int TITLE_COLOR = 0xFF000000; // Black

    // Progress bar: x=46 y=50, size 84×3  (background is 86×5 at x=45 y=49)
    private static final int PROG_BG_X = 45;
    private static final int PROG_BG_Y = 49;
    private static final int PROG_BG_W = 86;
    private static final int PROG_BG_H = 5;
    private static final int PROG_X = 46;
    private static final int PROG_Y = 50;
    private static final int PROG_W = 84;
    private static final int PROG_H = 3;

    // Clock text color #3f3f3f
    private static final int CLOCK_COLOR = 0xFF3F3F3F;
    private static final int CLOCK_Y = 58; // moved 3px down from 56


    // Auto-refresh interval
    private static final long REFRESH_INTERVAL_MS = 1000;

    // Static storage for elapsed time persistence
    private static final Map<BlockPos, Float> persistentElapsedTimes = new HashMap<>();
    private static final Map<BlockPos, Long> guiCloseTimes = new HashMap<>(); // Track when GUI was closed
    private static final java.util.Set<BlockPos> finishedPositions = new java.util.HashSet<>(); // Jukeboxes where song finished
    private static long worldJoinTime = 0L; // Wall-clock ms when this world/session was joined
    
    // Clear all persistent data when world reloads (like vanilla discs)
    public static void clearPersistentData() {
        persistentElapsedTimes.clear();
        guiCloseTimes.clear();
        finishedPositions.clear();
        worldJoinTime = System.currentTimeMillis();
    }
    
    // ── State ───────────────────────────────────────────────────────
    private final BlockPos pos;
    private boolean isPlaying = false;
    private long tickCount = 0;
    private long recordStartTick = 0;
	private String songTitle = "";
	private float totalSeconds = 0f;
	private long guiOpenTime = 0; // Track when GUI opened
	private long timerStartTime = 0; // When timer started (0 = not running)
	private boolean songFinished = false; // True when song played to completion
	private ItemStack previousDisc = ItemStack.EMPTY; // Track previous disc for change detection
	private boolean initialDetectionDone = false; // Track if initial disc detection done
	private long lastRefreshTime = 0;

	// ── Constructor ─────────────────────────────────────────────────────────
	public JukeboxManagementScreen(JukeboxScreenHandler handler,
	                                PlayerInventory playerInventory,
	                                Text title) {
		super(handler, playerInventory, title);
		this.pos = handler.getPos();
		this.backgroundWidth  = BG_WIDTH;
		this.backgroundHeight = BG_HEIGHT;
	}

	public BlockPos getPos() { return pos; }

	public void updateData(JukeboxGuiPacket.Payload payload) {
		this.isPlaying       = payload.isPlaying();
		this.tickCount       = payload.tickCount();
		this.recordStartTick = payload.recordStartTick();
		this.songTitle       = payload.songTitle();
		this.totalSeconds    = payload.totalSeconds();
		boolean serverReportsFinished = payload.isFinished();

		ItemStack incoming = payload.discStack().orElse(ItemStack.EMPTY);
		boolean discChanged = !ItemStack.areItemsEqual(incoming, previousDisc);


		if (incoming.isEmpty()) {
			// No disc — stop and clear everything
			this.timerStartTime = 0;
			persistentElapsedTimes.remove(this.pos);
			guiCloseTimes.remove(this.pos);
	
		} else if (discChanged && initialDetectionDone
				&& !incoming.getItem().equals(previousDisc.getItem())) {
			// A different disc was swapped in — reset to 00:00
			this.timerStartTime = System.currentTimeMillis();
			persistentElapsedTimes.remove(this.pos);
			guiCloseTimes.remove(this.pos);
	
		} else if (this.timerStartTime == 0) {
			// Timer not running yet — figure out where we are.
			Float savedElapsed = persistentElapsedTimes.get(this.pos);
			Long closeTime = guiCloseTimes.get(this.pos);

			if (savedElapsed != null && closeTime != null) {
				// Check if the song started AFTER the GUI was closed — means disc was ejected
				// and a new one inserted while GUI was closed. Discard saved state.
				float serverElapsedCheck = this.tickCount > 0 ? this.tickCount / 20f : 0f;
				long songStartCheck = System.currentTimeMillis() - (long)(serverElapsedCheck * 1000);
				persistentElapsedTimes.remove(this.pos);
				guiCloseTimes.remove(this.pos);
				if (songStartCheck > closeTime) {
					// New disc inserted after GUI closed — treat as fresh insert
					if (songStartCheck >= worldJoinTime) {
						this.timerStartTime = songStartCheck;
					}
				} else {
					// Same disc still playing — restore saved time + time elapsed while closed
					long timePassed = System.currentTimeMillis() - closeTime;
					float totalElapsed = savedElapsed + (timePassed / 1000f);
					this.timerStartTime = System.currentTimeMillis() - (long)(totalElapsed * 1000);
				}

			} else if (!this.isPlaying) {
				// Disc not playing — freeze at totalSeconds if song finished
				if (this.totalSeconds > 0 && (songFinished || finishedPositions.contains(this.pos) || serverReportsFinished)) {
					this.timerStartTime = System.currentTimeMillis() - (long)(this.totalSeconds * 1000);
					this.songFinished = true;
					finishedPositions.add(this.pos);
				}
			} else {
				// Disc is playing. Compute when the song started in wall-clock time.
				float serverElapsed = this.tickCount > 0 ? this.tickCount / 20f : 0f;
				long songStartWallClock = System.currentTimeMillis() - (long)(serverElapsed * 1000);
				// Only start the timer if the song began AFTER we joined this session.
				// If it started before (world reload with disc already playing), ignore it.
				if (songStartWallClock >= worldJoinTime) {
					this.timerStartTime = songStartWallClock;
				}
			}
		}


		// Detect song finishing: disc present, not playing, timer was running, not already marked finished
		if (!incoming.isEmpty() && !this.isPlaying && this.timerStartTime > 0 && !songFinished) {
			// Freeze timer at totalSeconds
			this.timerStartTime = System.currentTimeMillis() - (long)(this.totalSeconds * 1000);
			songFinished = true;
		}

		// Reset songFinished when disc is ejected or a new disc comes in
		if (incoming.isEmpty() || (discChanged && initialDetectionDone && !incoming.getItem().equals(previousDisc.getItem()))) {
			songFinished = false;
		}

		if (!initialDetectionDone) {
			initialDetectionDone = true;
			// Restore songFinished state from previous GUI session
			if (finishedPositions.contains(this.pos)) {
				this.songFinished = true;
			}
		}

		previousDisc = incoming.isEmpty() ? ItemStack.EMPTY : incoming.copy();

		// Sync handler slot display
		if (discChanged) {
			this.handler.slots.get(0).setStack(incoming.isEmpty() ? ItemStack.EMPTY : incoming.copy());
		}
	}

	@Override
	protected void init() {
		super.init();
		// Hide vanilla title & player inventory label completely
		this.titleX = -1000; // Move title off-screen
		this.titleY = -1000;
		this.playerInventoryTitleX = -1000;
		this.playerInventoryTitleY = -1000;

		// Record when GUI opened for elapsed time calculation
		this.guiOpenTime = System.currentTimeMillis();
	}

	@Override
	public void close() {
		// Save elapsed time and close time when GUI closes
		if (this.timerStartTime > 0) {
			float elapsed = getElapsedSeconds();
			persistentElapsedTimes.put(this.pos, elapsed);
			guiCloseTimes.put(this.pos, System.currentTimeMillis());
		} else {
			// Clear data if timer is not running
			persistentElapsedTimes.remove(this.pos);
			guiCloseTimes.remove(this.pos);
		}
		if (this.songFinished) {
			finishedPositions.add(this.pos);
		} else {
			finishedPositions.remove(this.pos);
		}
		super.close();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// Auto-refresh every second (but not every frame)
		long now = System.currentTimeMillis();
		if (now - lastRefreshTime >= REFRESH_INTERVAL_MS) {
			ClientPlayNetworking.send(new JukeboxGuiRefreshPacket(pos));
			lastRefreshTime = now;
		}

		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int gx = this.x;
		int gy = this.y;

		// Main container background
		context.drawTexture(RenderLayer::getGuiTextured, CONTAINER_TEXTURE,
			gx, gy, 0f, 0f,
			BG_WIDTH, BG_HEIGHT,
			BG_WIDTH, BG_HEIGHT);

		// ── Progress bar area ──────────────────────────────────────────────
		// Background
		context.drawTexture(RenderLayer::getGuiTextured, PROGRESS_BG_TEXTURE,
			gx + PROG_BG_X, gy + PROG_BG_Y,
			0f, 0f,
			PROG_BG_W, PROG_BG_H,
			PROG_BG_W, PROG_BG_H);

		// Foreground (disc-specific progress texture)
		ItemStack disc = this.handler.slots.get(0).getStack();
		if (!disc.isEmpty() && totalSeconds > 0) {
			Identifier progTex = getProgressTexture(disc);
			if (progTex != null) {
				float elapsedSec = getElapsedSeconds();
				float ratio = Math.min(1f, elapsedSec / totalSeconds);
				int progWidth = Math.max(0, Math.round(ratio * PROG_W));
				if (progWidth > 0) {
					// Draw only the filled portion of the 84×3 texture
					context.drawTexture(RenderLayer::getGuiTextured, progTex,
						gx + PROG_X, gy + PROG_Y,
						0f, 0f,
						progWidth, PROG_H,
						progWidth, PROG_H,
						PROG_W, PROG_H);
				}
			}
		}

		// ── Song title ────────────────────────────────────────────────────
		if (!songTitle.isEmpty()) {
			int titleX = gx + (BG_WIDTH - this.textRenderer.getWidth(songTitle)) / 2;
			if (this.timerStartTime > 0 && !this.songFinished) {
				drawRainbowText(context, songTitle, titleX, gy + TITLE_Y);
			} else {
				context.drawText(this.textRenderer, songTitle, titleX, gy + TITLE_Y, TITLE_COLOR, false);
			}
		} else {
			// Show "INSERT DISC" when no disc is present
			String insertText = "INSERT DISC";
			int insertX = gx + (BG_WIDTH - this.textRenderer.getWidth(insertText)) / 2;
			context.drawText(this.textRenderer, insertText, insertX, gy + TITLE_Y, CLOCK_COLOR, false);
		}

		// ── Digital clock ─────────────────────────────────────────────────
		String elapsedStr = formatTime(disc.isEmpty() ? -1 : (int) getElapsedSeconds());
		String totalStr   = formatTime(disc.isEmpty() ? -1 : (int) totalSeconds);

		// Elapsed: 14px to the right
		context.drawText(this.textRenderer, elapsedStr,
			gx + PROG_X + 14, gy + CLOCK_Y, CLOCK_COLOR, false);

		// Total: 12px from right edge (right-aligned)
		int totalW = this.textRenderer.getWidth(totalStr);
		context.drawText(this.textRenderer, totalStr,
			gx + PROG_X + PROG_W - 12 - totalW, gy + CLOCK_Y, CLOCK_COLOR, false);
	}

// ── Helpers ─────────────────────────────────────────────────────────────

/** Returns elapsed seconds based on simple timer */
private float getElapsedSeconds() {
    if (timerStartTime == 0) return 0f;
    
    // Calculate elapsed time from timer start time in milliseconds
    long elapsedMs = System.currentTimeMillis() - timerStartTime;
    float elapsedSeconds = elapsedMs / 1000f;
    
    // Cap at total duration to prevent going beyond song length
    return Math.min(elapsedSeconds, totalSeconds);
}

	/** Format seconds as MM:SS; returns "xx:xx" for negative values */
	private static String formatTime(int totalSec) {
		if (totalSec < 0) return "xx:xx";
		int minutes = totalSec / 60;
		int seconds = totalSec % 60;
		return String.format("%02d:%02d", minutes, seconds);
	}

	/** Draws text with a single animated rainbow color, cycling every 4 seconds. */
	private void drawRainbowText(DrawContext context, String text, int x, int y) {
		if (text.isEmpty()) return;
		float hue = (System.currentTimeMillis() % 4000L) / 4000f;
		int rgb = hsbToRgb(hue) | 0xFF000000;
		context.drawText(this.textRenderer, text, x, y, rgb, false);
	}

	private static int hsbToRgb(float hue) {
		float h = hue * 6f;
		int sector = (int) h;
		float frac = h - sector;
		float q = 1f - frac;
		float r, g, b;
		switch (sector % 6) {
			case 0 -> { r = 1f;   g = frac; b = 0f;  }
			case 1 -> { r = q;    g = 1f;   b = 0f;  }
			case 2 -> { r = 0f;   g = 1f;   b = frac;}
			case 3 -> { r = 0f;   g = q;    b = 1f;  }
			case 4 -> { r = frac; g = 0f;   b = 1f;  }
			default-> { r = 1f;   g = 0f;   b = q;   }
		}
		return ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
	}

	/**
	 * Tries to match the disc item's registry path to one of the known progress textures.
	 * E.g. "music_disc_cat" → "cat_progress.png"
	 */
	private Identifier getProgressTexture(ItemStack disc) {
		String itemPath = disc.getItem().toString(); // e.g. "minecraft:music_disc_cat"
		for (String name : DISC_NAMES) {
			if (itemPath.contains(name)) {
				return Identifier.of(JukeboxGUI.MOD_ID,
					"textures/gui/" + name + "_progress.png");
			}
		}
		// Unknown disc — use the background bar filled uniformly (return null = no fill)
		return null;
	}

	/**
	 * Returns animated rainbow color for disc titles, similar to vanilla behavior
	 */
	private int getRainbowColor() {
		// Use system time for animation (not instance variable)
		long time = System.currentTimeMillis();
		
		// Calculate hue based on time (0-360 degrees)
		float hue = (time % 3000) / 3000.0f * 360.0f; // Full cycle every 3 seconds
		
		// Convert HSL to RGB (simplified version)
		float c = 1.0f; // Chroma (saturation)
		float x = c * (1 - Math.abs((hue / 60) % 2 - 1));
		float m = 0; // Lightness
		
		float r, g, b;
		if (hue < 60) { r = c; g = x; b = 0; }
		else if (hue < 120) { r = x; g = c; b = 0; }
		else if (hue < 180) { r = 0; g = c; b = x; }
		else if (hue < 240) { r = 0; g = x; b = c; }
		else if (hue < 300) { r = x; g = 0; b = c; }
		else { r = c; g = 0; b = x; }
		
		// Convert to 0-255 range and combine into hex color
		int red = Math.round((r + m) * 255);
		int green = Math.round((g + m) * 255);
		int blue = Math.round((b + m) * 255);
		
		return (red << 16) | (green << 8) | blue;
	}

	/**
	 * Returns colorful text color for disc titles, similar to vanilla behavior
	 */
	private int getDiscColor(ItemStack disc) {
		String itemPath = disc.getItem().toString(); // e.g. "minecraft:music_disc_cat"
		
		// Map disc names to colors similar to vanilla
		if (itemPath.contains("13")) return 0xC2C2C2; // Light gray
		if (itemPath.contains("cat")) return 0xA9A9A9; // Gray
		if (itemPath.contains("blocks")) return 0x87CEEB; // Sky blue
		if (itemPath.contains("chirp")) return 0x90EE90; // Light green
		if (itemPath.contains("far")) return 0xFFB6C1; // Light pink
		if (itemPath.contains("mall")) return 0xDDA0DD; // Plum
		if (itemPath.contains("mellohi")) return 0xF0E68C; // Khaki
		if (itemPath.contains("stal")) return 0xFF6347; // Tomato
		if (itemPath.contains("strad")) return 0x40E0D0; // Turquoise
		if (itemPath.contains("ward")) return 0x9370DB; // Medium purple
		if (itemPath.contains("11")) return 0xFFD700; // Gold
		if (itemPath.contains("wait")) return 0xFF8C00; // Dark orange
		if (itemPath.contains("5")) return 0x8B4513; // Saddle brown
		if (itemPath.contains("otherside")) return 0x4B0082; // Indigo
		if (itemPath.contains("pigstep")) return 0xFF1493; // Deep pink
		if (itemPath.contains("relic")) return 0x00CED1; // Dark turquoise
		if (itemPath.contains("creator_music_box")) return 0xFF69B4; // Hot pink
		if (itemPath.contains("creator")) return 0xFF4500; // Orange red
		if (itemPath.contains("precipice")) return 0x2E8B57; // Sea green
		if (itemPath.contains("lava_chicken")) return 0xDC143C; // Crimson
		if (itemPath.contains("tears")) return 0x483D8B; // Dark slate blue
		
		// Default white for unknown discs
		return 0xFFFFFF;
	}
}