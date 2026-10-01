package com.yukar1n.x_tracer.server;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DropService {
    private DropService() {}

    public static void remove(EntityPlayerMP player, List<int[]> positions) {
        World world = player.worldObj;
        Map<String, ItemStack> merged = new HashMap<>();
        int itemCount = 0;
        for (int[] pos : positions) {
            int x = pos[0], y = pos[1], z = pos[2];
            int meta = world.getBlockMetadata(x, y, z);
            Block block = world.getBlock(x, y, z);
            for (ItemStack drop : block.getDrops(world, x, y, z, meta, 0)) {
                if (drop == null || drop.getItem() == null) continue;
                String key = Item.itemRegistry.getNameForObject(drop.getItem()) + ":" + drop.getItemDamage();
                ItemStack existing = merged.get(key);
                if (existing == null) merged.put(key, drop.copy());
                else existing.stackSize += drop.stackSize;
                itemCount += drop.stackSize;
            }
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile != null) world.removeTileEntity(x, y, z);
            if (world.setBlock(x, y, z, Blocks.air, 0, 2)) {
                world.notifyBlockChange(x, y, z, Blocks.air);
                world.markBlockForUpdate(x, y, z);
            }
        }
        for (ItemStack stack : merged.values()) {
            world.spawnEntityInWorld(new EntityItem(world, player.posX, player.posY, player.posZ, stack));
        }
        player.addChatMessage(new ChatComponentText("§6" + itemCount + " Drops generated"));
    }
}
