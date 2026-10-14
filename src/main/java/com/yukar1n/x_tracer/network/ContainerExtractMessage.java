package com.yukar1n.x_tracer.network;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.container.ContainerXTracerStorage;
import com.yukar1n.x_tracer.server.ChatFeedback;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

public class ContainerExtractMessage implements IMessage {
    private int x, y, z, index, count;
    private long revision;
    public ContainerExtractMessage() {}
    public ContainerExtractMessage(int x, int y, int z, int index, int count, long revision) {
        this.x=x; this.y=y; this.z=z; this.index=index; this.count=count; this.revision=revision;
    }
    @Override public void fromBytes(ByteBuf b) {
        x=b.readInt(); y=b.readInt(); z=b.readInt(); index=b.readInt(); count=b.readInt(); revision=b.readLong();
    }
    @Override public void toBytes(ByteBuf b) {
        b.writeInt(x); b.writeInt(y); b.writeInt(z); b.writeInt(index); b.writeInt(Math.min(64, Math.max(1,count))); b.writeLong(revision);
    }
    public static class Handler implements IMessageHandler<ContainerExtractMessage,IMessage> {
        @Override public IMessage onMessage(final ContainerExtractMessage message, MessageContext ctx) {
            final java.util.UUID id=ctx.getServerHandler().playerEntity.getUniqueID();
            final net.minecraft.inventory.Container receivedContainer = ctx.getServerHandler().playerEntity.openContainer;
            CommonProxy.serverTaskQueue.enqueue(new Runnable() {
                @Override public void run() {
                    EntityPlayerMP player=CommonProxy.findOnlinePlayer(id);
                    if (player == null || player.openContainer != receivedContainer
                        || !(player.openContainer instanceof ContainerXTracerStorage)) return;
                    ContainerXTracerStorage container=(ContainerXTracerStorage) player.openContainer;
                    if (container.tile.xCoord != message.x || container.tile.yCoord != message.y
                        || container.tile.zCoord != message.z || !container.tile.isUseableByPlayer(player)) return;
                    // Pipes can change their returned stacks before marking the tile dirty.
                    // Flush those changes before validating the snapshot's source index.
                    com.yukar1n.x_tracer.storage.LongItemStorage storage = container.tile.storage();
                    if (container.tile.getRevision() != message.revision) {
                        ChatFeedback.send(player, "xtracer.node.extract_stale");
                        return;
                    }
                    java.util.List<com.yukar1n.x_tracer.storage.LongItemStorage.Entry> entries = storage.entries();
                    if (message.index < 0 || message.index >= entries.size()) {
                        ChatFeedback.send(player, "xtracer.node.extract_empty");
                        return;
                    }
                    com.yukar1n.x_tracer.storage.ItemKey key = entries.get(message.index).key();
                    ItemStack stack = key.toStack(1);
                    if (stack == null || stack.getMaxStackSize() <= 0) {
                        ChatFeedback.send(player, "xtracer.node.extract_empty");
                        return;
                    }
                    int limit = Math.min(64, stack.getMaxStackSize());
                    long removed = container.tile.withdraw(key, Math.min(limit, Math.max(1, message.count)));
                    if (removed <= 0L) {
                        ChatFeedback.send(player, "xtracer.node.extract_empty");
                        return;
                    }
                    stack.stackSize = (int) removed;
                    int amount = stack.stackSize;
                    String itemName = stack.getDisplayName();
                    player.inventory.addItemStackToInventory(stack);
                    int remaining = stack.stackSize;
                    if (remaining > 0) player.dropPlayerItemWithRandomChoice(stack, false);
                    ChatFeedback.send(player, "xtracer.node.extracted", itemName, amount, amount - remaining, remaining);
                }
            });
            return null;
        }
    }
}
