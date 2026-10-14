package com.yukar1n.x_tracer.network;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.item.ItemXTracerTerminal;
import com.yukar1n.x_tracer.server.ChatFeedback;
import com.yukar1n.x_tracer.storage.ContainerReference;
import com.yukar1n.x_tracer.storage.TerminalProfile;
import com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

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
                EntityPlayerMP player=CommonProxy.findOnlinePlayer(playerId);
                if(player==null)return;
                ItemStack held=player.getHeldItem();
                if(held==null||held.getItem()!=ItemXTracerTerminal.INSTANCE){
                    ChatFeedback.send(player,"xtracer.error.hold_terminal");
                    return;
                }
                TileEntity target=player.worldObj.getTileEntity(message.x,message.y,message.z);
                if(!(target instanceof TileEntityXTracerContainer)){
                    ChatFeedback.send(player,"xtracer.binding.invalid_target",player.worldObj.provider.dimensionId,
                        message.x,message.y,message.z);
                    return;
                }
                TileEntityXTracerContainer tile=(TileEntityXTracerContainer)target;
                ContainerReference ref=ContainerReference.from(player.worldObj,message.x,message.y,message.z,tile);
                if(player.getDistanceSq(message.x+.5D,message.y+.5D,message.z+.5D)>64D){
                    ChatFeedback.send(player,"xtracer.binding.too_far",ChatFeedback.node(ref,tile.getCustomName()),8);
                    return;
                }
                List<ContainerReference> refs=TerminalProfile.bindings(held);
                List<ContainerReference> next=new ArrayList<>();
                boolean found=false;
                boolean replaced=false;
                for(ContainerReference current:refs){
                    if(current.sameLocation(ref)){
                        if(message.bind&&!found)next.add(ref);
                        found=true;
                        if(!current.uuid.equals(ref.uuid))replaced=true;
                    }else next.add(current);
                }
                String key;
                if(message.bind){
                    if(!found){
                        if(refs.size()>=TerminalProfile.MAX_BINDINGS){
                            ChatFeedback.send(player,"xtracer.binding.limit",ChatFeedback.node(ref,tile.getCustomName()),
                                refs.size(),TerminalProfile.MAX_BINDINGS);
                            return;
                        }
                        next.add(ref);
                        key="xtracer.binding.bound";
                    }else key=replaced?"xtracer.binding.rebound":"xtracer.binding.already_bound";
                }else key=found?"xtracer.binding.unbound":"xtracer.binding.not_bound";
                TerminalProfile.setBindings(held,next);
                ChatFeedback.send(player,key,ChatFeedback.node(ref,tile.getCustomName()),TerminalProfile.bindings(held).size());
            }});
            return null;
        }
    }
}
