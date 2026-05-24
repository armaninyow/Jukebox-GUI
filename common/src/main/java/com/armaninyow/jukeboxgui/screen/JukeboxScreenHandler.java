package com.armaninyow.jukeboxgui.screen;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class JukeboxScreenHandler extends AbstractContainerMenu {

    private final Container jukeboxInventory;
    private final ContainerLevelAccess context;
    private final BlockPos pos;

    /** Client-side constructor */
    public JukeboxScreenHandler(int syncId, Inventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, new SimpleContainer(1), ContainerLevelAccess.NULL, pos);
    }

    /** Server-side constructor */
    public JukeboxScreenHandler(int syncId, Inventory playerInventory,
                                Container jukeboxInventory, ContainerLevelAccess context,
                                BlockPos pos) {
        super(JukeboxGUI.JUKEBOX_SCREEN_HANDLER, syncId);
        checkContainerSize(jukeboxInventory, 1);
        this.jukeboxInventory = jukeboxInventory;
        this.context = context;
        this.pos = pos;
        jukeboxInventory.startOpen(playerInventory.player);

        // Disc slot — only accepts music discs
        this.addSlot(new Slot(jukeboxInventory, 0, 80, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.has(DataComponents.JUKEBOX_PLAYABLE);
            }
            @Override
            public int getMaxStackSize() { return 1; }
        });

        // Player inventory (27 slots)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));

        // Hotbar (9 slots)
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }

    public BlockPos getPos() { return pos; }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (slotIndex == 0) {
                if (!this.moveItemStackTo(originalStack, 1, this.slots.size(), true))
                    return ItemStack.EMPTY;
            } else {
                if (originalStack.has(DataComponents.JUKEBOX_PLAYABLE)) {
                    if (!this.moveItemStackTo(originalStack, 0, 1, false))
                        return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
            }
            if (originalStack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.jukeboxInventory.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.jukeboxInventory.stopOpen(player);
    }
}
