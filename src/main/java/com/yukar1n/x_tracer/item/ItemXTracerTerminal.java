package com.yukar1n.x_tracer.item;

import com.yukar1n.x_tracer.MyMod;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemXTracerTerminal extends Item {

    public static final ItemXTracerTerminal INSTANCE = new ItemXTracerTerminal();

    private ItemXTracerTerminal() {
        setUnlocalizedName("xtracer_terminal");
        setTextureName(MyMod.MODID + ":xtracer_terminal");
        setCreativeTab(CreativeTabs.tabTools);
        setMaxStackSize(1);
    }

    public static void register() {
        GameRegistry.registerItem(INSTANCE, "xtracer_terminal");
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            player.openGui(MyMod.INSTANCE, MyMod.TERMINAL_GUI_ID, world,
                (int) player.posX, (int) player.posY, (int) player.posZ);
        }
        return stack;
    }
}
