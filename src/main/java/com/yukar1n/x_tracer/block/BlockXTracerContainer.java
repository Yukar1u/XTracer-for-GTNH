package com.yukar1n.x_tracer.block;

import com.yukar1n.x_tracer.MyMod;
import com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public class BlockXTracerContainer extends BlockContainer {
    public static final BlockXTracerContainer INSTANCE = new BlockXTracerContainer();

    @SideOnly(Side.CLIENT)
    private IIcon topIcon;
    @SideOnly(Side.CLIENT)
    private IIcon bottomIcon;

    private BlockXTracerContainer() {
        super(Material.iron);
        setBlockName("xtracer_container");
        setBlockTextureName(MyMod.MODID + ":xtracer_container");
        setCreativeTab(CreativeTabs.tabTools);
        setHardness(3.0F);
        setResistance(10.0F);
    }

    public static void register() {
        GameRegistry.registerBlock(INSTANCE, "xtracer_container");
        GameRegistry.registerTileEntity(TileEntityXTracerContainer.class, "x_tracer_container");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(MyMod.MODID + ":xtracer_container_side");
        topIcon = register.registerIcon(MyMod.MODID + ":xtracer_container_top");
        bottomIcon = register.registerIcon(MyMod.MODID + ":xtracer_container_bottom");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (side == 0) return bottomIcon;
        if (side == 1) return topIcon;
        return blockIcon;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityXTracerContainer();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player,
                                    int side, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) player.openGui(MyMod.INSTANCE, MyMod.CONTAINER_GUI_ID, world, x, y, z);
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, placer, stack);
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityXTracerContainer && placer instanceof EntityPlayer) {
            ((TileEntityXTracerContainer) tile).setOwnerUuid(((EntityPlayer) placer).getUniqueID());
        }
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityXTracerContainer
            && !((TileEntityXTracerContainer) tile).isStorageEmpty()) {
            player.addChatMessage(new ChatComponentTranslation("xtracer.node.remove_not_empty"));
            return false;
        }
        return super.removedByPlayer(world, player, x, y, z, willHarvest);
    }
}
