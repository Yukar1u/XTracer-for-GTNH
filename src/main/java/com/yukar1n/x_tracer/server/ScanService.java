package com.yukar1n.x_tracer.server;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.Config;
import com.yukar1n.x_tracer.Util;
import com.yukar1n.x_tracer.network.FoundBlocksMessage;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ScanService {
    private ScanService() {}

    public static void enqueue(EntityPlayerMP player) {
        final java.util.UUID playerId = player.getUniqueID();
        CommonProxy.serverTaskQueue.enqueue(new Runnable() {
            @Override public void run() {
                EntityPlayerMP current = CommonProxy.findOnlinePlayer(playerId);
                if (current != null) enqueueOnServerThread(current);
            }
        });
    }

    public static void enqueueOnServerThread(EntityPlayerMP player) {
        java.util.UUID playerId = player.getUniqueID();
        scan(player, ScanRequest.from(playerId, Config.getPlayerConfig(playerId)));
    }

    public static void scan(EntityPlayerMP player, ScanRequest request) {
        World world = player.worldObj;
        int centerX = MathHelper.floor_double(player.posX);
        int centerY = MathHelper.floor_double(player.posY);
        int centerZ = MathHelper.floor_double(player.posZ);
        int radiusSquared = request.radius * request.radius;
        List<int[]> found = new ArrayList<>();
        for (int x = centerX - request.radius; x <= centerX + request.radius; x++) {
            for (int y = Math.max(0, centerY - request.radius); y <= Math.min(255, centerY + request.radius); y++) {
                for (int z = centerZ - request.radius; z <= centerZ + request.radius; z++) {
                    int dx = x - centerX, dy = y - centerY, dz = z - centerZ;
                    if (dx * dx + dy * dy + dz * dz > radiusSquared) continue;
                    if (matches(world, x, y, z, request)) found.add(new int[]{x, y, z});
                }
            }
        }
        player.addChatMessage(new ChatComponentText("§b§k:" + "§bTrace Out: " + found.size() + " §bObject§b§k:"));
        CommonProxy.network.sendTo(new FoundBlocksMessage(found), player);
        if (request.drop) DropService.remove(player, found);
    }

    private static boolean matches(World world, int x, int y, int z, ScanRequest request) {
        Block block = world.getBlock(x, y, z);
        if ("gtName".equals(request.mode)) {
            if (!Util.isGregTechBlock(block)) return false;
            try {
                int meta = world.getBlockMetadata(x, y, z);
                for (ItemStack drop : block.getDrops(world, x, y, z, meta, 0)) {
                    if (drop != null && drop.getItem() != null
                        && (drop.getItem().getUnlocalizedName() + "@" + drop.getItemDamage()).equals(request.gtTarget)) return true;
                }
            } catch (Throwable ignored) {}
            return false;
        }
        if ("block".equals(request.mode)) {
            String id = Block.blockRegistry.getNameForObject(block);
            String target = id + "@" + world.getBlockMetadata(x, y, z);
            return request.blockTargets.contains(target);
        }
        if ("ore".equals(request.mode)) {
            List<String> names = Util.getBlockOreNames(world, x, y, z);
            for (String target : request.oreTargets) if (names.contains(target)) return true;
        }
        return false;
    }
}
