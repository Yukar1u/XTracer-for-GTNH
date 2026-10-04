package com.yukar1n.x_tracer.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class ContainerStateMessage implements IMessage {
    private int x, y, z;
    private long revision;
    private String name = "";
    private List<String> accepts = new ArrayList<>();
    private List<String> excludes = new ArrayList<>();
    public ContainerStateMessage() {}
    public ContainerStateMessage(int x, int y, int z, long revision, String name,
                                 List<String> accepts, List<String> excludes) {
        this.x=x; this.y=y; this.z=z; this.revision=revision; this.name=name == null ? "" : name;
        if (accepts != null) this.accepts.addAll(accepts);
        if (excludes != null) this.excludes.addAll(excludes);
    }
    @Override public void fromBytes(ByteBuf buf) {
        x=buf.readInt(); y=buf.readInt(); z=buf.readInt(); revision=buf.readLong(); name=read(buf);
        accepts=readList(buf); excludes=readList(buf);
    }
    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z); buf.writeLong(revision); write(buf,name); writeList(buf,accepts); writeList(buf,excludes);
    }
    private static void write(ByteBuf buf,String value){byte[] b=value.getBytes(Charset.forName("UTF-8"));int n=Math.min(1024,b.length);buf.writeShort(n);buf.writeBytes(b,0,n);}
    private static String read(ByteBuf buf){int n=Math.min(1024,buf.readUnsignedShort());byte[] b=new byte[n];buf.readBytes(b);return new String(b,Charset.forName("UTF-8"));}
    private static void writeList(ByteBuf buf,List<String> values){int n=Math.min(32,values.size());buf.writeByte(n);for(int i=0;i<n;i++)write(buf,values.get(i));}
    private static List<String> readList(ByteBuf buf){int n=Math.min(32,buf.readUnsignedByte());List<String> out=new ArrayList<>();for(int i=0;i<n;i++)out.add(read(buf));return out;}
    public static class Handler implements IMessageHandler<ContainerStateMessage,IMessage>{
        @Override public IMessage onMessage(final ContainerStateMessage message,MessageContext ctx){
            Minecraft.getMinecraft().func_152344_a(new Runnable(){@Override public void run(){
                if(Minecraft.getMinecraft().currentScreen instanceof com.yukar1n.x_tracer.client.gui.GuiXTracerStorage){
                    ((com.yukar1n.x_tracer.client.gui.GuiXTracerStorage)Minecraft.getMinecraft().currentScreen).applyState(message.name,message.revision,message.accepts,message.excludes);
                }
            }}); return null;
        }
    }
}
