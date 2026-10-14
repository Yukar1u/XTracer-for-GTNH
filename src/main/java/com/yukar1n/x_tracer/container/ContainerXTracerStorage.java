package com.yukar1n.x_tracer.container;

import com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer;
import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.network.ContainerStateMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ICrafting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerXTracerStorage extends Container {
    public static final int PLAYER_INVENTORY_START = 0;
    public static final int INVENTORY_X = 29;
    public static final int INVENTORY_Y = 190;
    public static final int HOTBAR_Y = 246;
    public final TileEntityXTracerContainer tile;
    private String sentName;
    private List<String> sentAccepts = new ArrayList<>();
    private List<String> sentExcludes = new ArrayList<>();

    public ContainerXTracerStorage(InventoryPlayer inventory, TileEntityXTracerContainer tile) {
        this.tile = tile;
        // The storage is virtual and read-only. Keep only the player's real inventory
        // in the vanilla container so client/server slot layouts never drift.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(inventory, col, INVENTORY_X + col * 18, HOTBAR_Y));
    }

    @Override public void addCraftingToCrafters(ICrafting listener) {
        super.addCraftingToCrafters(listener);
        sendRules(listener);
    }

    private void sendRules(ICrafting listener) {
        if (listener instanceof EntityPlayerMP) {
            CommonProxy.network.sendTo(new ContainerStateMessage(tile.xCoord, tile.yCoord, tile.zCoord,
                tile.getRevision(), tile.getCustomName(), tile.getAccepts(), tile.getExcludes()), (EntityPlayerMP) listener);
        }
    }

    @Override public void detectAndSendChanges() {
        super.detectAndSendChanges();
        String name = tile.getCustomName();
        List<String> accepts = tile.getAccepts();
        List<String> excludes = tile.getExcludes();
        if (!name.equals(sentName) || !accepts.equals(sentAccepts) || !excludes.equals(sentExcludes)) {
            sentName = name;
            sentAccepts = new ArrayList<>(accepts);
            sentExcludes = new ArrayList<>(excludes);
            for (Object listener : crafters) sendRules((ICrafting) listener);
        }
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
