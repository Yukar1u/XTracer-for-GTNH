package com.yukar1n.x_tracer.gui;

import com.yukar1n.x_tracer.MyMod;
import com.yukar1n.x_tracer.container.ContainerXTracerStorage;
import com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.world.World;

public class XTracerGuiHandler implements cpw.mods.fml.common.network.IGuiHandler {
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != MyMod.CONTAINER_GUI_ID) return null;
        if (world.getTileEntity(x, y, z) instanceof TileEntityXTracerContainer) {
            return new ContainerXTracerStorage(player.inventory,
                (TileEntityXTracerContainer) world.getTileEntity(x, y, z));
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == MyMod.TERMINAL_GUI_ID) {
            try {
                Class<?> guiClass = Class.forName("com.yukar1n.x_tracer.client.gui.GuiXTracerTerminal");
                return guiClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Unable to create XTracer terminal GUI", e);
            }
        }
        if (id == MyMod.CONTAINER_GUI_ID) {
            if (world.getTileEntity(x, y, z) instanceof TileEntityXTracerContainer) {
                return new com.yukar1n.x_tracer.client.gui.GuiXTracerStorage(
                    player.inventory, (TileEntityXTracerContainer) world.getTileEntity(x, y, z));
            }
        }
        return null;
    }
}
