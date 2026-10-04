package com.yukar1n.x_tracer.container;

import com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerXTracerStorage extends Container {
    public static final int PLAYER_INVENTORY_START = 0;
    public final TileEntityXTracerContainer tile;

    public ContainerXTracerStorage(InventoryPlayer inventory, TileEntityXTracerContainer tile) {
        this.tile = tile;
        // The storage is virtual and read-only. Keep only the player's real inventory
        // in the vanilla container so client/server slot layouts never drift.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 196 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(inventory, col, 8 + col * 18, 254));
    }

    @Override public boolean canInteractWith(EntityPlayer player) { return tile.isUseableByPlayer(player); }

    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        // There is no destination slot: this container intentionally cannot accept input.
        return null;
    }

    @Override public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        tile.closeInventory();
    }
}
