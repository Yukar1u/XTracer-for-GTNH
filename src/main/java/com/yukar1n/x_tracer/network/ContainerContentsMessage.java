package com.yukar1n.x_tracer.network;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.storage.ItemKey;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

public class ContainerContentsMessage implements IMessage {
    public static final int PAGE_SIZE = 45;
    static final int LOOKUP_BATCH_SIZE = 32;

    public static final class Entry {
        public final int sourceIndex;
        public final ItemKey key;
        public final long count;

        public Entry(int sourceIndex, ItemKey key, long count) {
            this.sourceIndex = sourceIndex;
            this.key = key;
            this.count = count;
        }
    }

    private int x, y, z, page, totalEntries, requestId, lookupStart;
    private long revision, lookupNonce;
    private boolean nameLookup;
    private final List<Entry> entries = new ArrayList<>();

    public ContainerContentsMessage() {}

    public ContainerContentsMessage(int x, int y, int z, int page, long revision,
                                    int totalEntries, List<Entry> entries, int requestId) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.page = page;
        this.revision = revision;
        this.totalEntries = Math.max(0, totalEntries);
        this.requestId = requestId;
        if (entries != null) this.entries.addAll(entries);
    }

    static ContainerContentsMessage lookup(int x, int y, int z, int page, long revision,
                                           List<Entry> entries, int requestId, long nonce, int start) {
        ContainerContentsMessage message = new ContainerContentsMessage(
            x, y, z, page, revision, 0, entries, requestId);
        message.nameLookup = true;
        message.lookupNonce = nonce;
        message.lookupStart = start;
        return message;
    }

    @Override public void fromBytes(ByteBuf buf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt(); page = buf.readInt();
        revision = buf.readLong(); totalEntries = Math.max(0, buf.readInt());
        requestId = buf.readInt(); nameLookup = buf.readBoolean();
        lookupNonce = buf.readLong(); lookupStart = buf.readInt();
        int size = buf.readUnsignedByte();
        if (size > (nameLookup ? LOOKUP_BATCH_SIZE : PAGE_SIZE)) {
            throw new IllegalArgumentException("Invalid storage contents batch size");
        }
        entries.clear();
        for (int i = 0; i < size; i++) {
            int sourceIndex = buf.readInt();
            NBTTagCompound tag = ByteBufUtils.readTag(buf);
            if (tag == null) throw new IllegalArgumentException("Missing storage item key");
            entries.add(new Entry(sourceIndex, ItemKey.readFromNBT(tag), buf.readLong()));
        }
    }

    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z); buf.writeInt(page);
        buf.writeLong(revision); buf.writeInt(totalEntries); buf.writeInt(requestId);
        buf.writeBoolean(nameLookup); buf.writeLong(lookupNonce); buf.writeInt(lookupStart);
        int limit = nameLookup ? LOOKUP_BATCH_SIZE : PAGE_SIZE;
        if (entries.size() > limit) throw new IllegalArgumentException("Invalid storage contents batch size");
        buf.writeByte(entries.size());
        for (Entry entry : entries) {
            buf.writeInt(entry.sourceIndex);
            NBTTagCompound tag = new NBTTagCompound();
            entry.key.writeToNBT(tag);
            ByteBufUtils.writeTag(buf, tag);
            buf.writeLong(entry.count);
        }
    }

    public static class Handler implements IMessageHandler<ContainerContentsMessage, IMessage> {
        @Override public IMessage onMessage(final ContainerContentsMessage message, MessageContext ctx) {
            Minecraft.getMinecraft().func_152344_a(new Runnable() {
                @Override public void run() {
                    if (!(Minecraft.getMinecraft().currentScreen instanceof
                        com.yukar1n.x_tracer.client.gui.GuiXTracerStorage)) return;
                    com.yukar1n.x_tracer.client.gui.GuiXTracerStorage gui =
                        (com.yukar1n.x_tracer.client.gui.GuiXTracerStorage) Minecraft.getMinecraft().currentScreen;
                    if (!gui.isContainer(message.x, message.y, message.z)
                        || !gui.isContentsRequestCurrent(message.requestId)) return;
                    if (message.nameLookup) {
                        gui.contentsLookupProgress(message.requestId);
                        List<String> names = new ArrayList<>();
                        for (Entry entry : message.entries) {
                            String name = "";
                            try {
                                ItemStack stack = entry.key.toStack(1);
                                if (stack != null) name = stack.getDisplayName();
                            } catch (RuntimeException | LinkageError ignored) {
                                // Identifier and ore-name matching remain available for broken mod names.
                            }
                            names.add(name);
                        }
                        CommonProxy.network.sendToServer(ContainerContentsRequestMessage.names(
                            message.x, message.y, message.z, message.requestId, message.revision,
                            message.lookupNonce, message.lookupStart, names));
                    } else {
                        gui.applyContents(message.page, message.revision, message.totalEntries,
                            message.entries, message.requestId);
                    }
                }
            });
            return null;
        }
    }
}
