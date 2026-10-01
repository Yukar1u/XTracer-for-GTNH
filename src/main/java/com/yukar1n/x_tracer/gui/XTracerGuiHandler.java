package com.yukar1n.x_tracer.gui;

import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class XTracerGuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != 0) return null;
        try {
            Class<?> guiClass = Class.forName("com.yukar1n.x_tracer.client.gui.GuiXTracerTerminal");
            return guiClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create XTracer terminal GUI", e);
        }
    }
}
