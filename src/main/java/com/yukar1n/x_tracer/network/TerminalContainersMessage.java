package com.yukar1n.x_tracer.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class TerminalContainersMessage implements IMessage {
    private List<String> summaries = new ArrayList<>();
    public TerminalContainersMessage() {}
    public TerminalContainersMessage(List<String> summaries) { if (summaries != null) this.summaries.addAll(summaries); }
    @Override public void fromBytes(ByteBuf buf) { int n=Math.min(16,buf.readUnsignedByte());for(int i=0;i<n;i++)summaries.add(read(buf)); }
    @Override public void toBytes(ByteBuf buf) { int n=Math.min(16,summaries.size());buf.writeByte(n);for(int i=0;i<n;i++)write(buf,summaries.get(i)); }
    private static void write(ByteBuf buf,String value){byte[] b=value.getBytes(Charset.forName("UTF-8"));int n=Math.min(1024,b.length);buf.writeShort(n);buf.writeBytes(b,0,n);}
    private static String read(ByteBuf buf){int n=Math.min(1024,buf.readUnsignedShort());byte[] b=new byte[n];buf.readBytes(b);return new String(b,Charset.forName("UTF-8"));}
    public static class Handler implements IMessageHandler<TerminalContainersMessage,IMessage>{
        @Override public IMessage onMessage(final TerminalContainersMessage message,MessageContext ctx){Minecraft.getMinecraft().func_152344_a(new Runnable(){@Override public void run(){if(Minecraft.getMinecraft().currentScreen instanceof com.yukar1n.x_tracer.client.gui.GuiXTracerTerminal)((com.yukar1n.x_tracer.client.gui.GuiXTracerTerminal)Minecraft.getMinecraft().currentScreen).applyContainerSummaries(message.summaries);}});return null;}
    }
}
