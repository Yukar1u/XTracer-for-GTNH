package com.yukar1n.x_tracer.network;

import com.yukar1n.x_tracer.Config;
import com.yukar1n.x_tracer.item.ItemXTracerTerminal;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.oredict.OreDictionary;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class TerminalRequestMessage implements IMessage {
    public static final byte SET_BLOCK = 0;
    public static final byte SET_ORE = 1;
    public static final byte SET_RADIUS = 2;
    public static final byte SET_DROP = 3;
    public static final byte RAY = 4;
    public static final byte SCAN = 5;
    public static final byte CURRENT = 6;

    private byte action;
    private int radius;
    private boolean drop;
    private String target;

    public TerminalRequestMessage() {}

    public TerminalRequestMessage(byte action, String target, int radius, boolean drop) {
        this.action = action;
        this.target = target == null ? "" : target;
        this.radius = radius;
        this.drop = drop;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
        radius = buf.readInt();
        drop = buf.readBoolean();
        int length = Math.min(buf.readUnsignedShort(), 128);
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        target = new String(bytes, Charset.forName("UTF-8"));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
        buf.writeInt(radius);
        buf.writeBoolean(drop);
        byte[] bytes = target.getBytes(Charset.forName("UTF-8"));
        int length = Math.min(bytes.length, 128);
        buf.writeShort(length);
        buf.writeBytes(bytes, 0, length);
    }

    public static class Handler implements IMessageHandler<TerminalRequestMessage, IMessage> {
        @Override
        public IMessage onMessage(TerminalRequestMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            ItemStack held = player.getHeldItem();
            if (held == null || held.getItem() != ItemXTracerTerminal.INSTANCE) {
                player.addChatMessage(new ChatComponentText("§cXTracer: Hold the terminal first."));
                return null;
            }
            if (message.action == SCAN) {
                new KeyPressMessage.Handler().onMessage(new KeyPressMessage(), ctx);
                return null;
            }
            handle(message, player);
            return null;
        }

        private void handle(TerminalRequestMessage message, EntityPlayerMP player) {
            if (message.action == SET_BLOCK) {
                String[] parts = message.target.split("@", -1);
                String[] id = parts.length == 2 ? parts[0].split(":", -1) : new String[0];
                int meta;
                try { meta = parts.length == 2 ? Integer.parseInt(parts[1]) : -1; }
                catch (NumberFormatException e) { meta = -1; }
                Block block = id.length == 2 ? GameRegistry.findBlock(id[0], id[1]) : null;
                if (block == null || meta < 0 || meta > 15) {
                    player.addChatMessage(new ChatComponentText("§cXTracer: Invalid block target."));
                    return;
                }
                List<String> target = new ArrayList<>();
                target.add(message.target);
                Config.getPlayerConfig(player.getUniqueID()).curMatchMode = "block";
                Config.getPlayerConfig(player.getUniqueID()).setTargetBlockID(target);
            } else if (message.action == SET_ORE) {
                if (!OreDictionary.doesOreNameExist(message.target) || OreDictionary.getOres(message.target).isEmpty()) {
                    player.addChatMessage(new ChatComponentText("§cXTracer: Invalid ore dictionary name."));
                    return;
                }
                List<String> target = new ArrayList<>();
                target.add(message.target.toLowerCase());
                Config.getPlayerConfig(player.getUniqueID()).curMatchMode = "ore";
                Config.getPlayerConfig(player.getUniqueID()).setTargetOreName(target);
            } else if (message.action == SET_RADIUS) {
                if (message.radius < 1 || message.radius > Config.getPlayerConfig(player.getUniqueID()).maxRadius) return;
                Config.getPlayerConfig(player.getUniqueID()).radius = message.radius;
            } else if (message.action == SET_DROP) {
                Config.getPlayerConfig(player.getUniqueID()).getDrop = message.drop ? "True" : "False";
            } else if (message.action == RAY) {
                if (com.yukar1n.x_tracer.Util.setRayTrace(player) == 0) return;
            }
            player.addChatMessage(new ChatComponentText("§aXTracer terminal: setting updated."));
        }
    }
}
