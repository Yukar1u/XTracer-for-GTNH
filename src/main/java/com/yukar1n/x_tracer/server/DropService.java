package com.yukar1n.x_tracer.server;

import com.yukar1n.x_tracer.storage.ItemKey;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DropService {
    private DropService() {}

    public static void remove(EntityPlayerMP player, List<int[]> positions) {
        remove(player, positions, null);
    }

    public static void remove(EntityPlayerMP player, List<int[]> positions, ScanRequest request) {
        removeInternal(player, positions, request, null, null);
    }

    public static void removePrepared(EntityPlayerMP player, Prepared prepared, ScanRequest request, Map<ItemKey, String> names) {
        List<int[]> positions = new ArrayList<>();
        for (Source source : prepared.sources) positions.add(new int[]{source.x, source.y, source.z});
        removeInternal(player, positions, request, prepared, names);
    }

    private static void removeInternal(EntityPlayerMP player, List<int[]> positions, ScanRequest request,
                                       Prepared prepared, Map<ItemKey, String> names) {
        World world = player.worldObj;
        if (prepared != null) {
            for (Source source : prepared.sources) {
                if (!source.matches(world, source.x, source.y, source.z)) {
                    ChatFeedback.send(player, "xtracer.names.source_changed");
                    return;
                }
            }
        }
        boolean remoteEnabled = request != null && request.remoteInput;
        boolean routeRequested = remoteEnabled && !request.bindings.isEmpty();
        boolean routeSafe = routeRequested;
        List<ItemStack> removedDrops = new ArrayList<>();
        int removedBlocks = 0;
        boolean hasTileEntity = false;
        boolean removalSucceeded = true;

        // Remove first and collect drops from blocks that actually existed. Neighbor updates can
        // invalidate later scan results, so routing a precomputed batch would duplicate items.
        for (int[] pos : positions) {
            int x = pos[0], y = pos[1], z = pos[2];
            Block block = world.getBlock(x, y, z);
            if (block == Blocks.air) continue;
            if (world.getTileEntity(x, y, z) != null) hasTileEntity = true;
            List<ItemStack> drops = new ArrayList<>();
            if (prepared != null) {
                Source source = prepared.sourceAt(x, y, z);
                if (source == null || !source.matches(world, x, y, z)) {
                    ChatFeedback.send(player, "xtracer.drop.source_changed", x, y, z);
                    continue;
                }
                for (ItemStack drop : source.drops) drops.add(drop.copy());
            } else {
                int meta = world.getBlockMetadata(x, y, z);
                for (ItemStack drop : block.getDrops(world, x, y, z, meta, 0)) {
                    if (drop != null && drop.getItem() != null && drop.stackSize > 0) drops.add(drop.copy());
                }
            }
            if (!world.setBlock(x, y, z, Blocks.air, 0, 2)) {
                removalSucceeded = false;
                ChatFeedback.send(player, "xtracer.drop.removal_failed", x, y, z, removedBlocks, positions.size());
                break;
            }
            removedBlocks++;
            removedDrops.addAll(drops);
            world.notifyBlockChange(x, y, z, Blocks.air);
            world.markBlockForUpdate(x, y, z);
        }
        if (hasTileEntity) routeSafe = false;
        if (remoteEnabled && request.bindings.isEmpty()) ChatFeedback.send(player, "xtracer.route.no_bindings");
        else if (remoteEnabled && hasTileEntity) ChatFeedback.send(player, "xtracer.route.source_tile");

        ContainerRoutingService.Result routed = null;
        if (routeSafe && !removedDrops.isEmpty()) {
            routed = ContainerRoutingService.route(removedDrops, request.bindings, names);
            reportMatches(player, routed, names);
            routeSafe = routed.success;
            if (!routeSafe) {
                if (routed.failedNode != null) {
                    ChatFeedback.send(player, routed.failureReason, ChatFeedback.node(routed.failedNode, routed.failedName),
                        routed.failedItem.displayText());
                } else ChatFeedback.send(player, routed.failureReason);
            } else if (!routed.commit()) {
                routeSafe = false;
                ChatFeedback.send(player, "xtracer.route.commit_failed");
            }
        } else {
            routeSafe = false;
            if (routeRequested && removedDrops.isEmpty()) ChatFeedback.send(player, "xtracer.route.empty_batch");
        }

        List<ItemStack> output = routeSafe ? routed.fallback : removedDrops;
        long groundCount = 0L;
        long failedSpawnCount = 0L;
        for (ItemStack stack : output) {
            int remaining = stack.stackSize;
            int limit = Math.max(1, Math.min(64, stack.getMaxStackSize()));
            while (remaining > 0) {
                ItemStack part = stack.copy();
                part.stackSize = Math.min(limit, remaining);
                if (world.spawnEntityInWorld(new EntityItem(world, player.posX, player.posY, player.posZ, part))) groundCount += part.stackSize;
                else failedSpawnCount += part.stackSize;
                remaining -= part.stackSize;
            }
        }
        long storedCount = 0L;
        if (routeSafe) {
            for (ContainerRoutingService.Deposit deposit : routed.deposits()) {
                storedCount += deposit.count;
                ChatFeedback.send(player, "xtracer.route.deposit", deposit.key.displayText(), deposit.count,
                    ChatFeedback.node(deposit.reference, deposit.name));
            }
            for (Map.Entry<ItemKey, Long> entry : routed.unmatched.entrySet()) {
                ChatFeedback.send(player, "xtracer.route.unmatched", display(entry.getKey(), names), entry.getValue());
            }
        }
        long generated = 0L;
        for (ItemStack stack : removedDrops) generated += stack.stackSize;
        if (routeSafe && storedCount == 0L && generated > 0L) ChatFeedback.send(player, "xtracer.route.none_written");
        if (generated == 0L) ChatFeedback.send(player, "xtracer.drop.empty");
        ChatFeedback.send(player, "xtracer.drop.summary", removedBlocks, generated, storedCount, groundCount);
        if (failedSpawnCount > 0L) ChatFeedback.send(player, "xtracer.drop.spawn_failed", failedSpawnCount);
        if (!removalSucceeded && routeSafe) ChatFeedback.send(player, "xtracer.drop.partial_route");
    }

    public static Prepared prepare(EntityPlayerMP player, List<int[]> positions) {
        Prepared prepared = new Prepared();
        World world = player.worldObj;
        for (int[] pos : positions) {
            Block block = world.getBlock(pos[0], pos[1], pos[2]);
            if (block == Blocks.air) continue;
            int meta = world.getBlockMetadata(pos[0], pos[1], pos[2]);
            List<ItemStack> drops = new ArrayList<>();
            for (ItemStack drop : block.getDrops(world, pos[0], pos[1], pos[2], meta, 0)) {
                if (drop != null && drop.getItem() != null && drop.stackSize > 0) drops.add(drop.copy());
            }
            prepared.sources.add(new Source(pos[0], pos[1], pos[2], block, meta, drops));
            for (ItemStack drop : drops) {
                ItemKey key = ItemKey.fromStack(drop);
                if (key != null) prepared.keys.add(key);
            }
        }
        return prepared;
    }

    public static final class Prepared {
        private final List<Source> sources = new ArrayList<>();
        private final java.util.LinkedHashSet<ItemKey> keys = new java.util.LinkedHashSet<>();
        public List<ItemKey> keys() { return new ArrayList<>(keys); }
        public int sourceCount() { return sources.size(); }
        private Source sourceAt(int x, int y, int z) {
            for (Source source : sources) if (source.x == x && source.y == y && source.z == z) return source;
            return null;
        }
    }

    private static final class Source {
        private final int x, y, z, meta;
        private final Block block;
        private final List<ItemStack> drops;
        private Source(int x, int y, int z, Block block, int meta, List<ItemStack> drops) {
            this.x=x; this.y=y; this.z=z; this.block=block; this.meta=meta; this.drops=drops;
        }
        private boolean matches(World world, int x, int y, int z) {
            return world.getBlock(x, y, z) == block && world.getBlockMetadata(x, y, z) == meta;
        }
    }

    private static String display(ItemKey key, Map<ItemKey, String> names) {
        String localized = names == null ? null : names.get(key);
        return localized == null || localized.isEmpty() ? key.displayText() : localized;
    }

    private static void reportMatches(EntityPlayerMP player, ContainerRoutingService.Result result, Map<ItemKey, String> names) {
        ChatFeedback.send(player, "xtracer.route.matches", result.availableNodes, result.matchedNodes, result.unavailable.size());
        for (ContainerRoutingService.Unavailable unavailable : result.unavailable) {
            ChatFeedback.send(player, "xtracer.route.unavailable", ChatFeedback.node(unavailable.reference, ""),
                new ChatComponentTranslation(unavailable.reason));
        }
        for (Map.Entry<ItemKey, Integer> entry : result.matches.entrySet()) {
            ChatFeedback.send(player, "xtracer.route.item_matches", display(entry.getKey(), names), entry.getValue());
        }
        for (ContainerRoutingService.Rejection rejected : result.rejected) {
            String reasonKey = rejected.decision.reason == com.yukar1n.x_tracer.storage.RoutingMatcher.Reason.EXCLUDED
                ? "xtracer.route.rejected_exclude" : "xtracer.route.rejected_accept";
            ChatFeedback.send(player, reasonKey, ChatFeedback.node(rejected.reference, rejected.name),
                display(rejected.item, names), rejected.item.itemId() + "@" + rejected.item.damage(), rejected.decision.rule);
            ChatFeedback.send(player, "xtracer.route.active_rules",
                String.join(", ", rejected.decision.accepts), String.join(", ", rejected.decision.excludes), rejected.decision.text);
        }
    }
}
