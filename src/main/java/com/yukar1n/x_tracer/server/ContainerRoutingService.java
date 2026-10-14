package com.yukar1n.x_tracer.server;

import com.yukar1n.x_tracer.storage.ContainerReference;
import com.yukar1n.x_tracer.storage.ItemKey;
import com.yukar1n.x_tracer.storage.RoutingMatcher;
import com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ContainerRoutingService {
    private ContainerRoutingService() {}

    public static Result route(List<ItemStack> drops, List<ContainerReference> references) {
        return route(drops, references, null);
    }

    public static Result route(List<ItemStack> drops, List<ContainerReference> references, Map<ItemKey, String> localizedNames) {
        Result result = new Result();
        Map<ItemKey, Long> totals = new LinkedHashMap<>();
        Map<ItemKey, ItemStack> samples = new LinkedHashMap<>();
        for (ItemStack drop : drops) {
            if (drop == null || drop.getItem() == null || drop.stackSize <= 0) continue;
            ItemKey key = ItemKey.fromStack(drop);
            if (key == null) continue;
            long old = totals.containsKey(key) ? totals.get(key) : 0L;
            if (Long.MAX_VALUE - old < drop.stackSize) {
                result.success = false;
                result.failureReason = "xtracer.route.amount_overflow";
                return result;
            }
            totals.put(key, old + drop.stackSize);
            samples.put(key, drop.copy());
        }

        List<Destination> destinations = resolve(references, result);
        result.availableNodes = destinations.size();
        Map<Destination, Map<ItemKey, Long>> plan = new LinkedHashMap<>();
        List<Destination> matched = new ArrayList<>();
        for (Map.Entry<ItemKey, Long> entry : totals.entrySet()) {
            List<Destination> candidates = new ArrayList<>();
            for (Destination destination : destinations) {
                RoutingMatcher.Decision decision = localizedNames == null
                    ? destination.tile.evaluateRouting(entry.getKey())
                    : destination.tile.evaluateRouting(entry.getKey(), localizedNames.get(entry.getKey()));
                if (decision.accepted) {
                    candidates.add(destination);
                    if (!matched.contains(destination)) matched.add(destination);
                } else {
                    result.rejected.add(new Rejection(destination, entry.getKey(), decision));
                }
            }
            result.matches.put(entry.getKey(), candidates.size());
            if (candidates.isEmpty()) {
                result.unmatched.put(entry.getKey(), entry.getValue());
                addChunked(result.fallback, samples.get(entry.getKey()), entry.getValue());
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
        result.matchedNodes = matched.size();

        for (Map.Entry<Destination, Map<ItemKey, Long>> destination : plan.entrySet()) {
            for (Map.Entry<ItemKey, Long> item : destination.getValue().entrySet()) {
                if (!destination.getKey().tile.canDeposit(item.getKey(), item.getValue())) {
                    result.success = false;
                    result.failureReason = "xtracer.route.node_overflow";
                    result.failedNode = destination.getKey().reference;
                    result.failedName = destination.getKey().tile.getCustomName();
                    result.failedItem = item.getKey();
                    return result;
                }
                result.deposits.add(new Deposit(destination.getKey(), item.getKey(), item.getValue()));
            }
        }
        return result;
    }

    private static List<Destination> resolve(List<ContainerReference> references, Result report) {
        List<Destination> result = new ArrayList<>();
        if (references == null) return result;
        for (ContainerReference reference : references) {
            if (MinecraftServer.getServer() == null) {
                report.unavailable.add(new Unavailable(reference, "xtracer.route.dimension_unavailable"));
                continue;
            }
            WorldServer world = com.yukar1n.x_tracer.storage.TerminalProfile.loadedWorld(reference.dimension);
            if (world == null) {
                report.unavailable.add(new Unavailable(reference, "xtracer.route.dimension_unavailable"));
                continue;
            }
            int chunkX = reference.x >> 4;
            int chunkZ = reference.z >> 4;
            boolean wasLoaded = world.getChunkProvider().chunkExists(chunkX, chunkZ);
            Chunk chunk = null;
            try {
                if (!wasLoaded) chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
                if (world.getBlock(reference.x, reference.y, reference.z) != com.yukar1n.x_tracer.block.BlockXTracerContainer.INSTANCE
                    || !(world.getTileEntity(reference.x, reference.y, reference.z) instanceof TileEntityXTracerContainer)) {
                    report.unavailable.add(new Unavailable(reference, "xtracer.route.node_missing"));
                    continue;
                }
                TileEntityXTracerContainer tile = (TileEntityXTracerContainer) world.getTileEntity(reference.x, reference.y, reference.z);
                if (!reference.uuid.equals(tile.getContainerUuid())) {
                    report.unavailable.add(new Unavailable(reference, "xtracer.route.node_replaced"));
                    continue;
                }
                boolean duplicate = false;
                for (Destination existing : result) if (existing.tile == tile) duplicate = true;
                if (!duplicate) result.add(new Destination(tile, reference));
            } finally {
                if (!wasLoaded && chunk != null) world.getChunkProvider().unloadQueuedChunks();
            }
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
        public boolean success = true;
        public final List<ItemStack> fallback = new ArrayList<>();
        public final List<Unavailable> unavailable = new ArrayList<>();
        public final List<Rejection> rejected = new ArrayList<>();
        public final Map<ItemKey, Integer> matches = new LinkedHashMap<>();
        public final Map<ItemKey, Long> unmatched = new LinkedHashMap<>();
        public int availableNodes;
        public int matchedNodes;
        public String failureReason;
        public ContainerReference failedNode;
        public String failedName;
        public ItemKey failedItem;
        private final List<Deposit> deposits = new ArrayList<>();
        private boolean committed;

        public List<Deposit> deposits() { return java.util.Collections.unmodifiableList(deposits); }

        public boolean commit() {
            if (!success || committed) return false;
            List<Deposit> applied = new ArrayList<>();
            for (Deposit deposit : deposits) {
                if (!deposit.tile.deposit(deposit.key, deposit.count)) {
                    for (Deposit rollback : applied) rollback.tile.withdraw(rollback.key, rollback.count);
                    return false;
                }
                applied.add(deposit);
            }
            committed = true;
            return true;
        }
        public void rollback() {
            if (!committed) return;
            for (Deposit deposit : deposits) deposit.tile.withdraw(deposit.key, deposit.count);
            committed = false;
        }
    }

    public static final class Rejection {
        public final ContainerReference reference;
        public final String name;
        public final ItemKey item;
        public final RoutingMatcher.Decision decision;
        private Rejection(Destination destination, ItemKey item, RoutingMatcher.Decision decision) {
            this.reference = destination.reference;
            this.name = destination.tile.getCustomName();
            this.item = item;
            this.decision = decision;
        }
    }

    public static final class Unavailable {
        public final ContainerReference reference;
        public final String reason;
        private Unavailable(ContainerReference reference, String reason) { this.reference = reference; this.reason = reason; }
    }
    private static final class Destination {
        private final TileEntityXTracerContainer tile;
        private final ContainerReference reference;
        private Destination(TileEntityXTracerContainer tile, ContainerReference reference) { this.tile = tile; this.reference = reference; }
    }
    public static final class Deposit {
        private final TileEntityXTracerContainer tile;
        public final ContainerReference reference;
        public final String name;
        public final ItemKey key;
        public final long count;
        private Deposit(Destination destination, ItemKey key, long count) {
            this.tile = destination.tile;
            this.reference = destination.reference;
            this.name = tile.getCustomName();
            this.key = key;
            this.count = count;
        }
    }
}
