package com.yukar1n.x_tracer.container;

import com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerXTracerStorage extends Container {
    public final TileEntityXTracerContainer tile;

    public ContainerXTracerStorage(InventoryPlayer inventory, TileEntityXTracerContainer tile) {
        this.tile = tile;
        int size = Math.min(tile.getSizeInventory(), 54);
        for (int i = 0; i < size; i++) addSlotToContainer(new Slot(tile, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        int top = 18 + ((size + 8) / 9) * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, top + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(inventory, col, 8 + col * 18, top + 58));
    }

    @Override public boolean canInteractWith(EntityPlayer player) { return tile.isUseableByPlayer(player); }

    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index >= 0 && index < tile.getSizeInventory()) return null;
        return super.transferStackInSlot(player, index);
    }

    @Override public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        tile.closeInventory();
    }
}
