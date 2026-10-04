package com.yukar1n.x_tracer.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;

import java.nio.charset.Charset;

public class TerminalStateMessage implements IMessage {
    private int radius;
    private boolean drop;
    private String mode = "";
    private String blockTarget = "";
    private String oreTargets = "";
    private boolean remoteInput;

    public TerminalStateMessage() {}

    public TerminalStateMessage(int radius, boolean drop, String mode, String blockTarget, String oreTargets) {
        this(radius, drop, mode, blockTarget, oreTargets, false);
    }

    public TerminalStateMessage(int radius, boolean drop, String mode, String blockTarget, String oreTargets, boolean remoteInput) {
        this.radius = radius;
        this.drop = drop;
        this.mode = mode == null ? "" : mode;
        this.blockTarget = blockTarget == null ? "" : blockTarget;
        this.oreTargets = oreTargets == null ? "" : oreTargets;
        this.remoteInput = remoteInput;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        radius = buf.readInt();
        drop = buf.readBoolean();
        mode = readString(buf);
        blockTarget = readString(buf);
        oreTargets = readString(buf);
        remoteInput = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(radius);
        buf.writeBoolean(drop);
        writeString(buf, mode);
        writeString(buf, blockTarget);
        writeString(buf, oreTargets);
        buf.writeBoolean(remoteInput);
    }

    private static String readString(ByteBuf buf) {
        int length = Math.min(buf.readUnsignedShort(), 4096);
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, Charset.forName("UTF-8"));
    }

    private static void writeString(ByteBuf buf, String value) {
        byte[] bytes = value.getBytes(Charset.forName("UTF-8"));
        int length = Math.min(bytes.length, 4096);
        buf.writeShort(length);
        buf.writeBytes(bytes, 0, length);
    }

    public static class Handler implements IMessageHandler<TerminalStateMessage, IMessage> {
        @Override
        public IMessage onMessage(final TerminalStateMessage message, MessageContext ctx) {
            Minecraft.getMinecraft().func_152344_a(new Runnable() {
                @Override public void run() {
                    if (Minecraft.getMinecraft().currentScreen instanceof com.yukar1n.x_tracer.client.gui.GuiXTracerTerminal) {
                        ((com.yukar1n.x_tracer.client.gui.GuiXTracerTerminal) Minecraft.getMinecraft().currentScreen)
                            .applyServerState(message.radius, message.drop, message.mode, message.blockTarget, message.oreTargets, message.remoteInput);
                    }
                }
            });
            return null;
        }
    }

    public int getRadius() { return radius; }
    public boolean isDrop() { return drop; }
    public String getMode() { return mode; }
    public String getBlockTarget() { return blockTarget; }
    public String getOreTargets() { return oreTargets; }
    public boolean isRemoteInput() { return remoteInput; }
}
