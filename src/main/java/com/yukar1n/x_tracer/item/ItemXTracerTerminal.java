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

    private ItemStack openTerminal(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            MyMod.proxy.openTerminalGui();
        }
        return stack;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        return openTerminal(stack, world, player);
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world,
                                  int x, int y, int z, int side,
                                  float hitX, float hitY, float hitZ) {
        openTerminal(stack, world, player);
        return true;
    }
}
