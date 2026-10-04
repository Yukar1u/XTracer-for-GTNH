package com.yukar1n.x_tracer.network;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.container.ContainerXTracerStorage;
import com.yukar1n.x_tracer.storage.LongItemStorage;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import java.util.ArrayList;
import java.util.List;

public class ContainerContentsRequestMessage implements IMessage {
    private int x,y,z,page;
    public ContainerContentsRequestMessage() {}
    public ContainerContentsRequestMessage(int x,int y,int z,int page){this.x=x;this.y=y;this.z=z;this.page=page;}
    @Override public void fromBytes(ByteBuf b){x=b.readInt();y=b.readInt();z=b.readInt();page=b.readInt();}
    @Override public void toBytes(ByteBuf b){b.writeInt(x);b.writeInt(y);b.writeInt(z);b.writeInt(page);}
    public static class Handler implements IMessageHandler<ContainerContentsRequestMessage,IMessage>{
        @Override public IMessage onMessage(final ContainerContentsRequestMessage message,MessageContext ctx){
            final java.util.UUID id=ctx.getServerHandler().playerEntity.getUniqueID();
            CommonProxy.serverTaskQueue.enqueue(new Runnable(){@Override public void run(){
                EntityPlayerMP player=CommonProxy.findOnlinePlayer(id);if(player==null||!(player.openContainer instanceof ContainerXTracerStorage))return;
                ContainerXTracerStorage c=(ContainerXTracerStorage)player.openContainer;
                if(c.tile.xCoord!=message.x||c.tile.yCoord!=message.y||c.tile.zCoord!=message.z||!c.tile.isUseableByPlayer(player))return;
                List<LongItemStorage.Entry> all=c.tile.storage().entries();int start=Math.max(0,message.page)*ContainerContentsMessage.PAGE_SIZE;List<ContainerContentsMessage.Entry> page=new ArrayList<>();
                for(int i=start;i<all.size()&&i<start+ContainerContentsMessage.PAGE_SIZE;i++){LongItemStorage.Entry e=all.get(i);page.add(new ContainerContentsMessage.Entry(e.key().itemId(),e.key().damage(),e.count()));}
                CommonProxy.network.sendTo(new ContainerContentsMessage(message.x,message.y,message.z,Math.max(0,message.page),c.tile.getRevision(),all.size(),page),player);
            }});return null;
        }
    }
}
