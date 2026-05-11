package com.armaninyow.jukeboxgui.screen;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

public class JukeboxScreenHandler extends ScreenHandler {

	private final Inventory jukeboxInventory;
	private final ScreenHandlerContext context;
	private final BlockPos pos;

	/** Client-side constructor — BlockPos is sent via ExtendedScreenHandlerType codec */
	public JukeboxScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
		this(syncId, playerInventory, new SimpleInventory(1), ScreenHandlerContext.EMPTY, pos);
	}

	/** Server-side constructor — jukeboxInventory wraps the real jukebox block entity */
	public JukeboxScreenHandler(int syncId, PlayerInventory playerInventory,
	                             Inventory jukeboxInventory, ScreenHandlerContext context,
	                             BlockPos pos) {
		super(JukeboxGUI.JUKEBOX_SCREEN_HANDLER, syncId);
		checkSize(jukeboxInventory, 1);
		this.jukeboxInventory = jukeboxInventory;
		this.context = context;
		this.pos = pos;
		jukeboxInventory.onOpen(playerInventory.player);

		// ── Disc slot: x=80, y=17 — only accepts music discs ──────────────
		this.addSlot(new Slot(jukeboxInventory, 0, 80, 17) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.contains(DataComponentTypes.JUKEBOX_PLAYABLE);
			}

			@Override
			public int getMaxItemCount() { return 1; }
		});

		// ── Player inventory (27 slots), y=84 ─────────────────────────────
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
					8 + col * 18, 84 + row * 18));
			}
		}

		// ── Hotbar (9 slots), y=142 ────────────────────────────────────────
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
		}
	}

	public BlockPos getPos() { return pos; }

	@Override
	public ItemStack quickMove(PlayerEntity player, int slotIndex) {
		ItemStack newStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);

		if (slot.hasStack()) {
			ItemStack originalStack = slot.getStack();
			newStack = originalStack.copy();

			if (slotIndex == 0) {
				// Disc slot → player inventory
				if (!this.insertItem(originalStack, 1, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else {
				// Player inventory → disc slot (only music discs)
				if (originalStack.contains(DataComponentTypes.JUKEBOX_PLAYABLE)) {
					if (!this.insertItem(originalStack, 0, 1, false)) {
						return ItemStack.EMPTY;
					}
				} else {
					return ItemStack.EMPTY;
				}
			}

			if (originalStack.isEmpty()) {
				slot.setStack(ItemStack.EMPTY);
			} else {
				slot.markDirty();
			}
		}

		return newStack;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return this.jukeboxInventory.canPlayerUse(player);
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		this.jukeboxInventory.onClose(player);
	}
}