package com.yukar1n.x_tracer.network;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.container.ContainerXTracerStorage;
import com.yukar1n.x_tracer.storage.ItemKey;
import com.yukar1n.x_tracer.storage.LongItemStorage;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ContainerContentsRequestMessage implements IMessage {
    private static final long LOOKUP_TIMEOUT_NANOS = 30_000_000_000L;
    private static final int MAX_QUERY_LENGTH = 256;
    private static final int MAX_NAME_LENGTH = 1024;
    // Accessed only from the server task queue. One current request per player.
    private static final Map<UUID, SearchSession> SESSIONS = new HashMap<>();
    private static long nextNonce;

    private int x, y, z, page, requestId, lookupStart;
    private String query = "";
    private boolean namesReply;
    private long revision, lookupNonce;
    private final List<String> names = new ArrayList<>();

    public ContainerContentsRequestMessage() {}

    public ContainerContentsRequestMessage(int x, int y, int z, int page, String query, int requestId) {
        this.x = x; this.y = y; this.z = z; this.page = Math.max(0, page);
        this.query = bounded(query, MAX_QUERY_LENGTH).trim();
        this.requestId = requestId;
    }

    public ContainerContentsRequestMessage(int x, int y, int z, int page) {
        this(x, y, z, page, "", 0);
    }

    static ContainerContentsRequestMessage names(int x, int y, int z, int requestId,
                                                long revision, long nonce, int start, List<String> names) {
        ContainerContentsRequestMessage message = new ContainerContentsRequestMessage(x, y, z, 0, "", requestId);
        message.namesReply = true;
        message.revision = revision;
        message.lookupNonce = nonce;
        message.lookupStart = start;
        for (String name : names) message.names.add(bounded(name, MAX_NAME_LENGTH));
        return message;
    }

    @Override public void fromBytes(ByteBuf buf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt(); page = Math.max(0, buf.readInt());
        requestId = buf.readInt(); query = readString(buf, MAX_QUERY_LENGTH).trim();
        namesReply = buf.readBoolean(); revision = buf.readLong();
        lookupNonce = buf.readLong(); lookupStart = buf.readInt();
        int size = buf.readUnsignedByte();
        if (size > ContainerContentsMessage.LOOKUP_BATCH_SIZE || (!namesReply && size != 0)) {
            throw new IllegalArgumentException("Invalid storage names batch size");
        }
        names.clear();
        for (int i = 0; i < size; i++) names.add(readString(buf, MAX_NAME_LENGTH));
    }

    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z); buf.writeInt(page); buf.writeInt(requestId);
        ByteBufUtils.writeUTF8String(buf, bounded(query, MAX_QUERY_LENGTH));
        buf.writeBoolean(namesReply); buf.writeLong(revision); buf.writeLong(lookupNonce); buf.writeInt(lookupStart);
        if (names.size() > ContainerContentsMessage.LOOKUP_BATCH_SIZE) {
            throw new IllegalArgumentException("Invalid storage names batch size");
        }
        buf.writeByte(names.size());
        for (String name : names) ByteBufUtils.writeUTF8String(buf, bounded(name, MAX_NAME_LENGTH));
    }

    private static String bounded(String value, int limit) {
        return value == null ? "" : value.substring(0, Math.min(value.length(), limit));
    }

    private static String readString(ByteBuf buf, int limit) {
        String value = ByteBufUtils.readUTF8String(buf);
        if (value.length() > limit) throw new IllegalArgumentException("Storage search string too long");
        return value;
    }

    private static final class SearchSession {
        final ContainerXTracerStorage container;
        final int page, requestId;
        final String query;
        final long revision, nonce;
        final List<LongItemStorage.Entry> entries;
        final List<String> names = new ArrayList<>();
        int next;
        long touched = System.nanoTime();

        SearchSession(ContainerXTracerStorage container, int page, String query, int requestId) {
            this.container = container;
            this.page = page;
            this.query = query.toLowerCase(Locale.ROOT);
            this.requestId = requestId;
            // storage() flushes pipe mutations before capturing revision and source indices.
            this.entries = container.tile.storage().entries();
            this.revision = container.tile.getRevision();
            this.nonce = ++nextNonce;
        }
    }

    private static void pruneSessions(UUID activePlayer) {
        long now = System.nanoTime();
        Iterator<Map.Entry<UUID, SearchSession>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SearchSession> entry = iterator.next();
            EntityPlayerMP player = CommonProxy.findOnlinePlayer(entry.getKey());
            SearchSession session = entry.getValue();
            if (player == null || player.openContainer != session.container
                || (!entry.getKey().equals(activePlayer)
                    && now - session.touched > LOOKUP_TIMEOUT_NANOS)) iterator.remove();
        }
    }

    private static void sendLookup(EntityPlayerMP player, SearchSession session) {
        List<ContainerContentsMessage.Entry> batch = new ArrayList<>();
        int size = Math.min(ContainerContentsMessage.LOOKUP_BATCH_SIZE, session.entries.size() - session.next);
        for (int offset = 0; offset < size; offset++) {
            int index = session.next + offset;
            LongItemStorage.Entry entry = session.entries.get(index);
            batch.add(new ContainerContentsMessage.Entry(index, entry.key(), entry.count()));
        }
        session.touched = System.nanoTime();
        CommonProxy.network.sendTo(ContainerContentsMessage.lookup(
            session.container.tile.xCoord, session.container.tile.yCoord, session.container.tile.zCoord,
            session.page, session.revision, batch, session.requestId, session.nonce, session.next), player);
    }

    private static boolean matches(ItemKey key, String name, String query) {
        if (query.isEmpty() || key.matchText(name).contains(query)) return true;
        try {
            ItemStack stack = key.toStack(1);
            if (stack != null) {
                for (int oreId : OreDictionary.getOreIDs(stack)) {
                    if (OreDictionary.getOreName(oreId).toLowerCase(Locale.ROOT).contains(query)) return true;
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Registry IDs and client display names still match if a mod's ore lookup fails.
        }
        return false;
    }

    private static void sendPage(EntityPlayerMP player, SearchSession session) {
        List<ContainerContentsMessage.Entry> pageEntries = new ArrayList<>();
        int total = 0;
        // Clamp only the page, never the inventory. Use long arithmetic for hostile page numbers.
        long start = (long) session.page * ContainerContentsMessage.PAGE_SIZE;
        for (int index = 0; index < session.entries.size(); index++) {
            LongItemStorage.Entry entry = session.entries.get(index);
            String name = session.query.isEmpty() ? "" : session.names.get(index);
            if (!matches(entry.key(), name, session.query)) continue;
            if (total >= start && pageEntries.size() < ContainerContentsMessage.PAGE_SIZE) {
                pageEntries.add(new ContainerContentsMessage.Entry(index, entry.key(), entry.count()));
            }
            total++;
        }
        // If storage shrank, return its last valid page directly.
        int lastPage = total == 0 ? 0 : (total - 1) / ContainerContentsMessage.PAGE_SIZE;
        int page = Math.min(session.page, lastPage);
        if (page != session.page) {
            pageEntries.clear();
            int skipped = 0;
            int first = page * ContainerContentsMessage.PAGE_SIZE;
            for (int index = 0; index < session.entries.size(); index++) {
                LongItemStorage.Entry entry = session.entries.get(index);
                String name = session.query.isEmpty() ? "" : session.names.get(index);
                if (!matches(entry.key(), name, session.query)) continue;
                if (skipped++ >= first) pageEntries.add(new ContainerContentsMessage.Entry(index, entry.key(), entry.count()));
                if (pageEntries.size() == ContainerContentsMessage.PAGE_SIZE) break;
            }
        }
        CommonProxy.network.sendTo(new ContainerContentsMessage(
            session.container.tile.xCoord, session.container.tile.yCoord, session.container.tile.zCoord,
            page, session.revision, total, pageEntries, session.requestId), player);
    }

    public static class Handler implements IMessageHandler<ContainerContentsRequestMessage, IMessage> {
        @Override public IMessage onMessage(final ContainerContentsRequestMessage message, MessageContext ctx) {
            final UUID id = ctx.getServerHandler().playerEntity.getUniqueID();
            final net.minecraft.inventory.Container receivedContainer = ctx.getServerHandler().playerEntity.openContainer;
            CommonProxy.serverTaskQueue.enqueue(new Runnable() {
                @Override public void run() {
                    pruneSessions(id);
                    EntityPlayerMP player = CommonProxy.findOnlinePlayer(id);
                    if (player == null || player.openContainer != receivedContainer
                        || !(player.openContainer instanceof ContainerXTracerStorage)) return;
                    ContainerXTracerStorage container = (ContainerXTracerStorage) player.openContainer;
                    if (container.tile.xCoord != message.x || container.tile.yCoord != message.y
                        || container.tile.zCoord != message.z || !container.tile.isUseableByPlayer(player)) return;
                    if (!message.namesReply) {
                        SearchSession session = new SearchSession(container, message.page, message.query, message.requestId);
                        SESSIONS.remove(id);
                        if (session.query.isEmpty() || session.entries.isEmpty()) {
                            sendPage(player, session);
                        } else {
                            SESSIONS.put(id, session);
                            sendLookup(player, session);
                        }
                        return;
                    }
                    SearchSession session = SESSIONS.get(id);
                    if (session == null || session.container != container || session.requestId != message.requestId
                        || session.revision != message.revision || session.nonce != message.lookupNonce
                        || session.next != message.lookupStart) return;
                    int expected = Math.min(ContainerContentsMessage.LOOKUP_BATCH_SIZE, session.entries.size() - session.next);
                    if (message.names.size() != expected) return;
                    container.tile.storage();
                    if (container.tile.getRevision() != session.revision
                        || System.nanoTime() - session.touched > LOOKUP_TIMEOUT_NANOS) {
                        // A changed inventory invalidates every cached name and source index.
                        session = new SearchSession(container, session.page, session.query, session.requestId);
                        if (session.entries.isEmpty()) {
                            SESSIONS.remove(id);
                            sendPage(player, session);
                        } else {
                            SESSIONS.put(id, session);
                            sendLookup(player, session);
                        }
                        return;
                    }
                    session.names.addAll(message.names);
                    session.next += expected;
                    if (session.next == session.entries.size()) {
                        SESSIONS.remove(id);
                        sendPage(player, session);
                    } else {
                        sendLookup(player, session);
                    }
                }
            });
            return null;
        }
    }
}
