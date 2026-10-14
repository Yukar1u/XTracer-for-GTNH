package com.yukar1n.x_tracer.network;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.container.ContainerXTracerStorage;
import com.yukar1n.x_tracer.server.ChatFeedback;
import com.yukar1n.x_tracer.storage.ContainerReference;
import com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class ContainerEditMessage implements IMessage {
    public static final byte SAVE = 0;
    public static final byte REQUEST_STATE = 1;
    private static final int MAX_TEXT = 1024;
    private int x, y, z;
    private byte action;
    private long revision;
    private String name = "";
    private List<String> accepts = new ArrayList<>();
    private List<String> excludes = new ArrayList<>();

    public ContainerEditMessage() {}
    public ContainerEditMessage(byte action, int x, int y, int z, long revision,
                                String name, List<String> accepts, List<String> excludes) {
        this.action = action; this.x = x; this.y = y; this.z = z; this.revision = revision;
        this.name = bounded(name);
        this.accepts = boundedList(accepts);
        this.excludes = boundedList(excludes);
    }
    public static ContainerEditMessage request(int x, int y, int z) {
        return new ContainerEditMessage(REQUEST_STATE, x, y, z, 0L, "", null, null);
    }
    @Override public void fromBytes(ByteBuf buf) {
        action = buf.readByte(); x = buf.readInt(); y = buf.readInt(); z = buf.readInt(); revision = buf.readLong();
        name = read(buf);
        accepts = readList(buf); excludes = readList(buf);
    }
    @Override public void toBytes(ByteBuf buf) {
        buf.writeByte(action); buf.writeInt(x); buf.writeInt(y); buf.writeInt(z); buf.writeLong(revision);
        write(buf, name); writeList(buf, accepts); writeList(buf, excludes);
    }
    private static String bounded(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length() && out.length() < 128; i++) {
            char c = value.charAt(i);
            if (c >= 32 && c != '\n' && c != '\r') out.append(c);
        }
        return out.toString().trim();
    }
    private static List<String> boundedList(List<String> values) {
        List<String> out = new ArrayList<>();
        if (values != null) for (String value : values) if (out.size() < 32) out.add(bounded(value));
        return out;
    }
    private static void write(ByteBuf buf, String value) {
        byte[] bytes = bounded(value).getBytes(Charset.forName("UTF-8"));
        int length = Math.min(bytes.length, MAX_TEXT); buf.writeShort(length); buf.writeBytes(bytes, 0, length);
    }
    private static String read(ByteBuf buf) {
        int length = Math.min(buf.readUnsignedShort(), MAX_TEXT); byte[] bytes = new byte[length]; buf.readBytes(bytes);
        return bounded(new String(bytes, Charset.forName("UTF-8")));
    }
    private static void writeList(ByteBuf buf, List<String> values) {
        int size = Math.min(values == null ? 0 : values.size(), 32); buf.writeByte(size);
        for (int i = 0; i < size; i++) write(buf, values.get(i));
    }
    private static List<String> readList(ByteBuf buf) {
        int size = Math.min(buf.readUnsignedByte(), 32); List<String> out = new ArrayList<>();
        for (int i = 0; i < size; i++) out.add(read(buf)); return out;
    }

    public static class Handler implements IMessageHandler<ContainerEditMessage, IMessage> {
        @Override public IMessage onMessage(final ContainerEditMessage message, MessageContext ctx) {
            final java.util.UUID playerId = ctx.getServerHandler().playerEntity.getUniqueID();
            CommonProxy.serverTaskQueue.enqueue(new Runnable() {
                @Override public void run() {
                    EntityPlayerMP player = CommonProxy.findOnlinePlayer(playerId);
                    if (player == null) return;
                    if (!(player.openContainer instanceof ContainerXTracerStorage)) {
                        if (message.action == SAVE) ChatFeedback.send(player, "xtracer.container.edit_not_open");
                        return;
                    }
                    ContainerXTracerStorage open = (ContainerXTracerStorage) player.openContainer;
                    TileEntityXTracerContainer tile = open.tile;
                    if (tile.xCoord != message.x || tile.yCoord != message.y || tile.zCoord != message.z) {
                        if (message.action == SAVE) ChatFeedback.send(player, "xtracer.container.edit_wrong_target");
                        return;
                    }
                    ContainerReference reference = ContainerReference.from(player.worldObj, tile.xCoord, tile.yCoord, tile.zCoord, tile);
                    if (!tile.isUseableByPlayer(player)) {
                        if (message.action == SAVE) ChatFeedback.send(player, "xtracer.container.edit_unavailable",
                            ChatFeedback.node(reference, tile.getCustomName()));
                        return;
                    }
                    if (message.action == REQUEST_STATE) {
                        CommonProxy.network.sendTo(new ContainerStateMessage(message.x, message.y, message.z,
                            tile.getRevision(), tile.getCustomName(), tile.getAccepts(), tile.getExcludes()), player);
                        return;
                    }
                    if (!tile.getOwnerUuid().isEmpty() && !tile.getOwnerUuid().equals(playerId.toString())) {
                        if (message.action == SAVE) {
                            ChatFeedback.send(player, "xtracer.container.edit_owner_only", ChatFeedback.node(reference, tile.getCustomName()));
                            CommonProxy.network.sendTo(new ContainerStateMessage(message.x, message.y, message.z,
                                tile.getRevision(), tile.getCustomName(), tile.getAccepts(), tile.getExcludes()), player);
                        }
                        return;
                    }
                    if (message.action == SAVE) {
                        if (!tile.setExpressions(message.accepts, message.excludes, message.revision)) {
                            ChatFeedback.send(player, "xtracer.container.edit_conflict", ChatFeedback.node(reference, tile.getCustomName()));
                            CommonProxy.network.sendTo(new ContainerStateMessage(message.x, message.y, message.z,
                                tile.getRevision(), tile.getCustomName(), tile.getAccepts(), tile.getExcludes()), player);
                            return;
                        }
                        tile.setCustomName(message.name);
                        CommonProxy.network.sendTo(new ContainerStateMessage(message.x, message.y, message.z,
                            tile.getRevision(), tile.getCustomName(), tile.getAccepts(), tile.getExcludes()), player);
                        ChatFeedback.send(player, "xtracer.container.saved", ChatFeedback.node(reference, tile.getCustomName()),
                            tile.getAccepts().size(), tile.getExcludes().size(), new ChatComponentTranslation(
                                tile.getAccepts().isEmpty() ? "xtracer.container.accept_all" : "xtracer.container.accept_matches"));
                    }
                }
            });
            return null;
        }
    }
}
