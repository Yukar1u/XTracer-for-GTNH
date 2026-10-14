package com.yukar1n.x_tracer.network;
import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.storage.ItemKey;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import java.util.ArrayList;
import java.util.List;
public final class ItemNameLookupReplyMessage implements IMessage {
 public long nonce; public int start; public final List<ItemKey> keys=new ArrayList<>(); public final List<String> names=new ArrayList<>();
 public ItemNameLookupReplyMessage(){} public ItemNameLookupReplyMessage(long n,int s,List<ItemKey> k,List<String> v){nonce=n;start=s;if(k!=null)keys.addAll(k);if(v!=null)names.addAll(v);}
 public void fromBytes(ByteBuf b){nonce=b.readLong();start=b.readInt();int n=Math.min(32,b.readUnsignedByte());for(int i=0;i<n;i++)keys.add(ItemKey.readFromNBT(ByteBufUtils.readTag(b)));for(int i=0;i<n;i++){String v=ByteBufUtils.readUTF8String(b);names.add(v.length()>256?v.substring(0,256):v);}}
 public void toBytes(ByteBuf b){b.writeLong(nonce);b.writeInt(start);int n=Math.min(32,Math.min(keys.size(),names.size()));b.writeByte(n);for(int i=0;i<n;i++){NBTTagCompound t=new NBTTagCompound();keys.get(i).writeToNBT(t);ByteBufUtils.writeTag(b,t);}for(int i=0;i<n;i++)ByteBufUtils.writeUTF8String(b,names.get(i));}
 public static final class Handler implements IMessageHandler<ItemNameLookupReplyMessage,IMessage>{public IMessage onMessage(final ItemNameLookupReplyMessage m,MessageContext c){final java.util.UUID id=c.getServerHandler().playerEntity.getUniqueID();CommonProxy.serverTaskQueue.enqueue(new Runnable(){public void run(){com.yukar1n.x_tracer.server.LocalizedNameService.reply(id,m.nonce,m.start,m.keys,m.names);}});return null;}}
}
