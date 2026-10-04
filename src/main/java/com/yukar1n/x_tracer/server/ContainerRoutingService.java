package com.yukar1n.x_tracer.server;

import com.yukar1n.x_tracer.storage.ContainerReference;
import com.yukar1n.x_tracer.storage.ItemKey;
import com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ContainerRoutingService {
    private ContainerRoutingService() {}

    public static Result route(List<ItemStack> drops, List<ContainerReference> references) {
        Map<ItemKey, Long> totals = new LinkedHashMap<>();
        Map<ItemKey, ItemStack> samples = new LinkedHashMap<>();
        for (ItemStack drop : drops) {
            if (drop == null || drop.getItem() == null || drop.stackSize <= 0) continue;
            ItemKey key = ItemKey.fromStack(drop);
            if (key == null) continue;
            long old = totals.containsKey(key) ? totals.get(key) : 0L;
            if (Long.MAX_VALUE - old < drop.stackSize) return Result.failure();
            totals.put(key, old + drop.stackSize);
            samples.put(key, drop.copy());
        }

        List<Destination> destinations = resolve(references);
        Map<Destination, Map<ItemKey, Long>> plan = new LinkedHashMap<>();
        List<ItemStack> fallback = new ArrayList<>();
        for (Map.Entry<ItemKey, Long> entry : totals.entrySet()) {
            List<Destination> candidates = new ArrayList<>();
            for (Destination destination : destinations) if (destination.tile.accepts(entry.getKey())) candidates.add(destination);
            if (candidates.isEmpty()) {
                addChunked(fallback, samples.get(entry.getKey()), entry.getValue());
                continue;
            }
            long base = entry.getValue() / candidates.size();
            int remainder = (int) (entry.getValue() % candidates.size());
            for (int i = 0; i < candidates.size(); i++) {
                long count = base + (i < remainder ? 1L : 0L);
                if (count == 0L) continue;
                Destination destination = candidates.get(i);
                Map<ItemKey, Long> values = plan.get(destination);
                if (values == null) { values = new LinkedHashMap<>(); plan.put(destination, values); }
                values.put(entry.getKey(), count);
            }
        }

        List<Deposit> deposits = new ArrayList<>();
        for (Map.Entry<Destination, Map<ItemKey, Long>> destination : plan.entrySet()) {
            for (Map.Entry<ItemKey, Long> item : destination.getValue().entrySet()) {
                if (!destination.getKey().tile.canDeposit(item.getKey(), item.getValue())) return Result.failure();
                deposits.add(new Deposit(destination.getKey().tile, item.getKey(), item.getValue()));
            }
        }
        return new Result(true, fallback, deposits);
    }

    private static List<Destination> resolve(List<ContainerReference> references) {
        List<Destination> result = new ArrayList<>();
        if (references == null || MinecraftServer.getServer() == null) return result;
        for (ContainerReference reference : references) {
            WorldServer world = MinecraftServer.getServer().worldServerForDimension(reference.dimension);
            if (world == null || !world.getChunkProvider().chunkExists(reference.x >> 4, reference.z >> 4)) continue;
            if (world.getBlock(reference.x, reference.y, reference.z) != com.yukar1n.x_tracer.block.BlockXTracerContainer.INSTANCE) continue;
            if (!(world.getTileEntity(reference.x, reference.y, reference.z) instanceof TileEntityXTracerContainer)) continue;
            TileEntityXTracerContainer tile = (TileEntityXTracerContainer) world.getTileEntity(reference.x, reference.y, reference.z);
            if (!reference.uuid.equals(tile.getContainerUuid())) continue;
            boolean duplicate = false;
            for (Destination existing : result) if (existing.tile == tile) duplicate = true;
            if (!duplicate) result.add(new Destination(tile));
        }
        return result;
    }

    private static void addChunked(List<ItemStack> output, ItemStack sample, long count) {
        if (sample == null) return;
        int limit = Math.max(1, Math.min(64, sample.getMaxStackSize()));
        while (count > 0L) {
            int amount = (int) Math.min((long) limit, count);
            ItemStack stack = sample.copy(); stack.stackSize = amount; output.add(stack); count -= amount;
        }
    }

    public static final class Result {
        public final boolean success;
        public final List<ItemStack> fallback;
        private final List<Deposit> deposits;
        private Result(boolean success, List<ItemStack> fallback, List<Deposit> deposits) { this.success=success;this.fallback=fallback;this.deposits=deposits; }
        private static Result failure() { return new Result(false, new ArrayList<ItemStack>(), new ArrayList<Deposit>()); }
        public boolean commit() {
            List<Deposit> applied = new ArrayList<>();
            for (Deposit deposit : deposits) {
                if (!deposit.tile.deposit(deposit.key, deposit.count)) {
                    for (Deposit rollback : applied) rollback.tile.withdraw(rollback.key, rollback.count);
                    return false;
                }
                applied.add(deposit);
            }
            return true;
        }
        public void rollback() { for (Deposit deposit : deposits) deposit.tile.withdraw(deposit.key, deposit.count); }
    }
    private static final class Destination { private final TileEntityXTracerContainer tile; private Destination(TileEntityXTracerContainer tile){this.tile=tile;} }
    private static final class Deposit { private final TileEntityXTracerContainer tile; private final ItemKey key; private final long count; private Deposit(TileEntityXTracerContainer tile,ItemKey key,long count){this.tile=tile;this.key=key;this.count=count;} }
}
