package com.yukar1n.x_tracer.server;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.network.ItemNameLookupMessage;
import com.yukar1n.x_tracer.storage.ItemKey;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-owned pending extraction state. Display names are supplied by the initiating client only. */
public final class LocalizedNameService {
    private static final long TIMEOUT_MS = 10000L;
    private static final int MAX_KEYS = 256;
    private static final int BATCH = 32;
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private LocalizedNameService() {}

    public static synchronized boolean hasPending(UUID id) { return PENDING.containsKey(id); }

    public static void begin(EntityPlayerMP player, DropService.Prepared prepared, ScanRequest request) {
        List<ItemKey> keys = prepared.keys();
        if (keys.size() > MAX_KEYS) { ChatFeedback.send(player, "xtracer.names.limit"); return; }
        if (keys.isEmpty()) { DropService.removePrepared(player, prepared, request, new HashMap<ItemKey,String>()); return; }
        Pending pending = new Pending(player.getUniqueID(), nextNonce(), prepared, request, keys);
        synchronized (LocalizedNameService.class) { PENDING.put(player.getUniqueID(), pending); }
        ChatFeedback.send(player, "xtracer.names.waiting", keys.size());
        sendBatch(player, pending, 0);
    }

    private static void sendBatch(EntityPlayerMP player, Pending pending, int start) {
        int end = Math.min(pending.keys.size(), start + BATCH);
        CommonProxy.network.sendTo(new ItemNameLookupMessage(pending.nonce, start,
            pending.keys.subList(start, end)), player);
    }

    public static void reply(final UUID id, final long nonce, final int start, final List<ItemKey> replyKeys,
                             final List<String> names) {
        Pending pending;
        synchronized (LocalizedNameService.class) { pending = PENDING.get(id); }
        if (pending == null || pending.nonce != nonce || expired(pending)) { fail(id, "xtracer.names.timeout"); return; }
        if (start != pending.received || replyKeys.size() != names.size() || replyKeys.size() > BATCH) { fail(id, "xtracer.names.failed"); return; }
        for (int i=0; i<replyKeys.size(); i++) {
            if (!pending.keys.get(start + i).equals(replyKeys.get(i)) || names.get(i) == null || names.get(i).trim().isEmpty()) {
                fail(id, "xtracer.names.failed"); return;
            }
            pending.names.put(replyKeys.get(i), names.get(i));
        }
        pending.received += replyKeys.size();
        EntityPlayerMP player = CommonProxy.findOnlinePlayer(id);
        if (player == null) { fail(id, "xtracer.names.failed"); return; }
        if (pending.received < pending.keys.size()) { sendBatch(player, pending, pending.received); return; }
        synchronized (LocalizedNameService.class) { PENDING.remove(id); }
        if (player.getHeldItem() == null || player.getHeldItem().getItem() != com.yukar1n.x_tracer.item.ItemXTracerTerminal.INSTANCE
            || player.worldObj.provider.dimensionId != pending.dimension) {
            ChatFeedback.send(player, "xtracer.names.cancelled"); return;
        }
        DropService.removePrepared(player, pending.prepared, pending.request, pending.names);
    }

    public static void reply(UUID id, long nonce, int start, List<ItemKey> keys) {
        // A request sent by a client is never a valid completion message.
        fail(id, "xtracer.names.failed");
    }

    public static synchronized void tick() {
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Pending> entry : PENDING.entrySet()) if (LocalizedNameService.expired(entry.getValue())) expired.add(entry.getKey());
        for (UUID id : expired) {
            PENDING.remove(id);
            EntityPlayerMP player = CommonProxy.findOnlinePlayer(id);
            if (player != null) ChatFeedback.send(player, "xtracer.names.timeout");
        }
    }

    private static void fail(UUID id, String key) {
        Pending pending;
        synchronized (LocalizedNameService.class) { pending = PENDING.remove(id); }
        if (pending != null) { EntityPlayerMP player=CommonProxy.findOnlinePlayer(id); if(player!=null)ChatFeedback.send(player,key); }
    }
    private static boolean expired(Pending pending) { return System.currentTimeMillis() - pending.createdAt > TIMEOUT_MS; }
    private static long nextNonce() { return System.nanoTime() ^ (long) System.identityHashCode(PENDING); }

    private static final class Pending {
        private final UUID id; private final long nonce; private final long createdAt=System.currentTimeMillis();
        private final int dimension; private final DropService.Prepared prepared; private final ScanRequest request;
        private final List<ItemKey> keys; private final Map<ItemKey,String> names=new HashMap<>(); private int received;
        private Pending(UUID id,long nonce,DropService.Prepared prepared,ScanRequest request,List<ItemKey> keys){this.id=id;this.nonce=nonce;this.dimension=CommonProxy.findOnlinePlayer(id).worldObj.provider.dimensionId;this.prepared=prepared;this.request=request;this.keys=new ArrayList<>(keys);}
    }
}
