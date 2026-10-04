package com.yukar1n.x_tracer.server;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import com.yukar1n.x_tracer.storage.ContainerReference;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DropService {
    private DropService() {}

    public static void remove(EntityPlayerMP player, List<int[]> positions) {
        remove(player, positions, null);
    }

    public static void remove(EntityPlayerMP player, List<int[]> positions, ScanRequest request) {
        World world = player.worldObj;
        Map<String, ItemStack> merged = new HashMap<>();
        List<ItemStack> allDrops = new java.util.ArrayList<>();
        int itemCount = 0;
        boolean routePossible = request != null && request.remoteInput && !request.bindings.isEmpty();
        boolean routeSafe = routePossible;
        for (int[] pos : positions) {
            int x = pos[0], y = pos[1], z = pos[2];
            int meta = world.getBlockMetadata(x, y, z);
            Block block = world.getBlock(x, y, z);
            if (routePossible && world.getTileEntity(x, y, z) != null) routeSafe = false;
            for (ItemStack drop : block.getDrops(world, x, y, z, meta, 0)) {
                if (drop == null || drop.getItem() == null) continue;
                allDrops.add(drop.copy());
                String key = Item.itemRegistry.getNameForObject(drop.getItem()) + ":" + drop.getItemDamage();
                ItemStack existing = merged.get(key);
                if (existing == null) merged.put(key, drop.copy());
                else existing.stackSize += drop.stackSize;
                itemCount += drop.stackSize;
            }
        }
        ContainerRoutingService.Result routed = null;
        List<ItemStack> output = new java.util.ArrayList<>();
        if (routeSafe) {
            routed = ContainerRoutingService.route(allDrops, request.bindings);
            if (routed.success) output.addAll(routed.fallback);
            else routeSafe = false;
        }
        if (!routeSafe) output.addAll(merged.values());
        if (routeSafe && routed != null && !routed.commit()) {
            routeSafe = false;
            output.clear();
            output.addAll(merged.values());
            player.addChatMessage(new ChatComponentText("§cXTracer: remote route failed; normal drops generated."));
        }
        boolean removalSucceeded = true;
        for (int[] pos : positions) {
            int x = pos[0], y = pos[1], z = pos[2];
            Block block = world.getBlock(x, y, z);
            if (block == Blocks.air) continue;
            if (!world.setBlock(x, y, z, Blocks.air, 0, 2)) {
                removalSucceeded = false;
                break;
            }
            world.notifyBlockChange(x, y, z, Blocks.air);
            world.markBlockForUpdate(x, y, z);
        }
        if (!removalSucceeded && routeSafe && routed != null) {
            routed.rollback();
            output.clear();
            output.addAll(merged.values());
            player.addChatMessage(new ChatComponentText("§cXTracer: remote route rolled back because a source block could not be removed."));
        }
        for (ItemStack stack : output) world.spawnEntityInWorld(new EntityItem(world, player.posX, player.posY, player.posZ, stack));
        player.addChatMessage(new ChatComponentText("§6" + itemCount + " Drops generated" + (routeSafe && removalSucceeded ? " (remote routed)" : "")));
    }
}
