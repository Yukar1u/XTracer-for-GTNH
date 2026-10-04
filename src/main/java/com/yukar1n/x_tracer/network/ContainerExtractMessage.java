package com.yukar1n.x_tracer.network;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.container.ContainerXTracerStorage;
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
            CommonProxy.serverTaskQueue.enqueue(new Runnable() {
                @Override public void run() {
                    EntityPlayerMP player=CommonProxy.findOnlinePlayer(id);
                    if (player == null || !(player.openContainer instanceof ContainerXTracerStorage)) return;
                    ContainerXTracerStorage container=(ContainerXTracerStorage) player.openContainer;
                    if (container.tile.xCoord != message.x || container.tile.yCoord != message.y
                        || container.tile.zCoord != message.z || container.tile.getRevision() != message.revision
                        || !container.tile.isUseableByPlayer(player)) return;
                    ItemStack stack=container.tile.withdrawSlot(message.index, Math.min(64, Math.max(1,message.count)));
                    if (stack != null && !player.inventory.addItemStackToInventory(stack)) player.dropPlayerItemWithRandomChoice(stack, false);
                }
            });
            return null;
        }
    }
}
