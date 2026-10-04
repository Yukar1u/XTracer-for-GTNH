package com.yukar1n.x_tracer.network;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.container.ContainerXTracerStorage;
import com.yukar1n.x_tracer.storage.LongItemStorage;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class ContainerContentsMessage implements IMessage {
    public static final int PAGE_SIZE = 18;
    public static final class Entry {
        public final String itemId;
        public final int damage;
        public final long count;
        public Entry(String itemId, int damage, long count) { this.itemId=itemId;this.damage=damage;this.count=count; }
    }
    private int x,y,z,page;
    private long revision;
    private List<Entry> entries = new ArrayList<>();
    public ContainerContentsMessage() {}
    public ContainerContentsMessage(int x,int y,int z,int page,long revision,List<Entry> entries){this.x=x;this.y=y;this.z=z;this.page=page;this.revision=revision;if(entries!=null)this.entries.addAll(entries);}
    @Override public void fromBytes(ByteBuf buf){x=buf.readInt();y=buf.readInt();z=buf.readInt();page=buf.readInt();revision=buf.readLong();int n=Math.min(PAGE_SIZE,buf.readUnsignedByte());for(int i=0;i<n;i++)entries.add(new Entry(read(buf),buf.readInt(),buf.readLong()));}
    @Override public void toBytes(ByteBuf buf){buf.writeInt(x);buf.writeInt(y);buf.writeInt(z);buf.writeInt(page);buf.writeLong(revision);int n=Math.min(PAGE_SIZE,entries.size());buf.writeByte(n);for(int i=0;i<n;i++){Entry e=entries.get(i);write(buf,e.itemId);buf.writeInt(e.damage);buf.writeLong(e.count);}}
    private static void write(ByteBuf buf,String value){byte[] b=value.getBytes(Charset.forName("UTF-8"));int n=Math.min(256,b.length);buf.writeShort(n);buf.writeBytes(b,0,n);}
    private static String read(ByteBuf buf){int n=Math.min(256,buf.readUnsignedShort());byte[] b=new byte[n];buf.readBytes(b);return new String(b,Charset.forName("UTF-8"));}

    public static class RequestHandler implements IMessageHandler<ContainerContentsMessage,IMessage>{
        @Override public IMessage onMessage(final ContainerContentsMessage message,MessageContext ctx){
            final java.util.UUID playerId=ctx.getServerHandler().playerEntity.getUniqueID();
            CommonProxy.serverTaskQueue.enqueue(new Runnable(){@Override public void run(){
                EntityPlayerMP player=CommonProxy.findOnlinePlayer(playerId); if(player==null||!(player.openContainer instanceof ContainerXTracerStorage))return;
                ContainerXTracerStorage open=(ContainerXTracerStorage)player.openContainer;
                if(open.tile.xCoord!=message.x||open.tile.yCoord!=message.y||open.tile.zCoord!=message.z||!open.tile.isUseableByPlayer(player))return;
                List<LongItemStorage.Entry> all=open.tile.storage().entries();int start=Math.max(0,message.page)*PAGE_SIZE;List<Entry> page=new ArrayList<>();
                for(int i=start;i<all.size()&&i<start+PAGE_SIZE;i++){LongItemStorage.Entry e=all.get(i);page.add(new Entry(e.key().itemId(),e.key().damage(),e.count()));}
                CommonProxy.network.sendTo(new ContainerContentsMessage(message.x,message.y,message.z,Math.max(0,message.page),open.tile.getRevision(),page),player);
            }});return null;
        }
    }

    public static class Handler implements IMessageHandler<ContainerContentsMessage,IMessage>{
        @Override public IMessage onMessage(final ContainerContentsMessage message,MessageContext ctx){Minecraft.getMinecraft().func_152344_a(new Runnable(){@Override public void run(){if(Minecraft.getMinecraft().currentScreen instanceof com.yukar1n.x_tracer.client.gui.GuiXTracerStorage){com.yukar1n.x_tracer.client.gui.GuiXTracerStorage gui=(com.yukar1n.x_tracer.client.gui.GuiXTracerStorage)Minecraft.getMinecraft().currentScreen;if(gui.isContainer(message.x,message.y,message.z))gui.applyContents(message.page,message.revision,message.entries);}}});return null;}
    }
}
