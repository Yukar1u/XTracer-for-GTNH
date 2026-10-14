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

public final class ItemNameLookupMessage implements IMessage {
    public long nonce; public int start; public final List<ItemKey> keys=new ArrayList<>();
    public ItemNameLookupMessage() {}
    public ItemNameLookupMessage(long nonce,int start,List<ItemKey> keys){this.nonce=nonce;this.start=start;if(keys!=null)this.keys.addAll(keys);}
    @Override public void fromBytes(ByteBuf b){nonce=b.readLong();start=b.readInt();int n=Math.min(32,b.readUnsignedByte());for(int i=0;i<n;i++)keys.add(ItemKey.readFromNBT(ByteBufUtils.readTag(b)));}
    @Override public void toBytes(ByteBuf b){b.writeLong(nonce);b.writeInt(start);int n=Math.min(32,keys.size());b.writeByte(n);for(int i=0;i<n;i++){NBTTagCompound t=new NBTTagCompound();keys.get(i).writeToNBT(t);ByteBufUtils.writeTag(b,t);}}
    public static final class Handler implements IMessageHandler<ItemNameLookupMessage,IMessage>{
      @Override public IMessage onMessage(final ItemNameLookupMessage m,MessageContext c){Minecraft.getMinecraft().func_152344_a(new Runnable(){public void run(){List<String> names=new ArrayList<>();for(ItemKey k:m.keys){String n="";try{ItemStack s=k.toStack(1);if(s!=null)n=s.getDisplayName();}catch(Throwable ignored){}names.add(n);}CommonProxy.network.sendToServer(new ItemNameLookupReplyMessage(m.nonce,m.start,m.keys,names));}});return null;}
    }
}
