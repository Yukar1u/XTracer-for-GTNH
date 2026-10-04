package com.yukar1n.x_tracer.network;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.item.ItemXTracerTerminal;
import com.yukar1n.x_tracer.storage.ContainerReference;
import com.yukar1n.x_tracer.storage.TerminalProfile;
import com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayList;
import java.util.List;

public class TerminalBindingMessage implements IMessage {
    private boolean bind;
    private int x, y, z;
    public TerminalBindingMessage() {}
    public TerminalBindingMessage(boolean bind, int x, int y, int z) { this.bind=bind;this.x=x;this.y=y;this.z=z; }
    @Override public void fromBytes(ByteBuf buf){bind=buf.readBoolean();x=buf.readInt();y=buf.readInt();z=buf.readInt();}
    @Override public void toBytes(ByteBuf buf){buf.writeBoolean(bind);buf.writeInt(x);buf.writeInt(y);buf.writeInt(z);}
    public static class Handler implements IMessageHandler<TerminalBindingMessage,IMessage>{
        @Override public IMessage onMessage(final TerminalBindingMessage message,MessageContext ctx){
            final java.util.UUID playerId=ctx.getServerHandler().playerEntity.getUniqueID();
            CommonProxy.serverTaskQueue.enqueue(new Runnable(){@Override public void run(){
                EntityPlayerMP player=CommonProxy.findOnlinePlayer(playerId); if(player==null)return;
                ItemStack held=player.getHeldItem();
                if(held==null||held.getItem()!=ItemXTracerTerminal.INSTANCE){error(player,"Hold the terminal first.");return;}
                if(player.worldObj.getTileEntity(message.x,message.y,message.z) instanceof TileEntityXTracerContainer){
                    TileEntityXTracerContainer tile=(TileEntityXTracerContainer)player.worldObj.getTileEntity(message.x,message.y,message.z);
                    if(player.getDistanceSq(message.x+.5D,message.y+.5D,message.z+.5D)>64D){error(player,"Container is too far away.");return;}
                    List<ContainerReference> refs=TerminalProfile.bindings(held); ContainerReference ref=ContainerReference.from(player.worldObj,message.x,message.y,message.z,tile);
                    List<ContainerReference> next=new ArrayList<>(); boolean found=false;
                    for(ContainerReference current:refs){if(current.sameLocation(ref)){found=true;if(message.bind)next.add(current);}else next.add(current);}
                    if(message.bind&&!found)next.add(ref); TerminalProfile.setBindings(held,next);
                    player.addChatMessage(new ChatComponentText("§aXTracer terminal: container " + (message.bind ? "bound." : "unbound.")));
                } else error(player,"Target is not an XTracer container.");
            }}); return null;
        }
        private static void error(EntityPlayerMP player,String text){player.addChatMessage(new ChatComponentText("§cXTracer: "+text));}
    }
}
