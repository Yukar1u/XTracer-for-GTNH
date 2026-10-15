package com.yukar1n.x_tracer.network;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.item.ItemXTracerTerminal;
import com.yukar1n.x_tracer.server.ChatFeedback;
import com.yukar1n.x_tracer.storage.ContainerReference;
import com.yukar1n.x_tracer.storage.TerminalProfile;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import java.nio.charset.Charset;

/** Server-authoritative edits to a specific terminal binding row. */
public class TerminalContainerEditMessage implements IMessage {
    public static final byte REMOVE = 0;
    public static final byte SET_ENABLED = 1;
    private byte action;
    private int dimension, x, y, z;
    private boolean enabled;
    private String uuid = "";

    public TerminalContainerEditMessage() {}

    public TerminalContainerEditMessage(byte action, ContainerReference reference, boolean enabled) {
        this.action = action;
        this.dimension = reference.dimension;
        this.x = reference.x;
        this.y = reference.y;
        this.z = reference.z;
        this.enabled = enabled;
        this.uuid = reference.uuid;
    }

    @Override public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
        dimension = buf.readInt(); x = buf.readInt(); y = buf.readInt(); z = buf.readInt();
        enabled = buf.readBoolean();
        int length = Math.min(buf.readUnsignedShort(), 128);
        byte[] bytes = new byte[length]; buf.readBytes(bytes);
        uuid = new String(bytes, Charset.forName("UTF-8"));
    }

    @Override public void toBytes(ByteBuf buf) {
        buf.writeByte(action); buf.writeInt(dimension); buf.writeInt(x); buf.writeInt(y); buf.writeInt(z);
        buf.writeBoolean(enabled);
        byte[] bytes = uuid.getBytes(Charset.forName("UTF-8"));
        int length = Math.min(bytes.length, 128); buf.writeShort(length); buf.writeBytes(bytes, 0, length);
    }

    public static class Handler implements IMessageHandler<TerminalContainerEditMessage, IMessage> {
        @Override public IMessage onMessage(final TerminalContainerEditMessage message, MessageContext ctx) {
            final java.util.UUID playerId = ctx.getServerHandler().playerEntity.getUniqueID();
            CommonProxy.serverTaskQueue.enqueue(new Runnable() {
                @Override public void run() {
                    EntityPlayerMP player = CommonProxy.findOnlinePlayer(playerId);
                    if (player == null) return;
                    ItemStack held = player.getHeldItem();
                    if (held == null || held.getItem() != ItemXTracerTerminal.INSTANCE) {
                        ChatFeedback.send(player, "xtracer.error.hold_terminal");
                        return;
                    }
                    ContainerReference expected = new ContainerReference(message.dimension, message.x, message.y,
                        message.z, message.uuid);
                    boolean changed;
                    if (message.action == REMOVE) {
                        changed = TerminalProfile.removeBinding(held, expected);
                        ChatFeedback.send(player, changed ? "xtracer.binding.unbound_selected" : "xtracer.binding.stale");
                    } else if (message.action == SET_ENABLED) {
                        changed = TerminalProfile.updateBinding(held, expected, message.enabled);
                        ChatFeedback.send(player, changed ? "xtracer.binding.route_changed" : "xtracer.binding.stale");
                    } else {
                        ChatFeedback.send(player, "xtracer.binding.invalid_edit");
                        return;
                    }
                    if (changed) {
                        player.sendContainerToPlayer(player.inventoryContainer);
                        TerminalRequestMessage.Handler.sendContainers(player, held);
                    }
                }
            });
            return null;
        }
    }
}
