package com.yukar1n.x_tracer.network;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.Config;
import com.yukar1n.x_tracer.item.ItemXTracerTerminal;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.registry.GameRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import com.yukar1n.x_tracer.OreNames;
import net.minecraftforge.oredict.OreDictionary;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TerminalRequestMessage implements IMessage {
    public static final byte SET_BLOCK = 0;
    public static final byte SET_ORE = 1;
    public static final byte SET_RADIUS = 2;
    public static final byte SET_DROP = 3;
    public static final byte RAY = 4;
    public static final byte SCAN = 5;
    public static final byte CURRENT = 6;
    public static final byte SET_ORE_TARGETS = 7;
    public static final byte REQUEST_STATE = 8;
    private static final int MAX_TARGETS = 64;
    private static final int MAX_TARGET_LENGTH = 128;

    private byte action;
    private int radius;
    private boolean drop;
    private List<String> targets = new ArrayList<>();

    public TerminalRequestMessage() {}

    public TerminalRequestMessage(byte action, String target, int radius, boolean drop) {
        this.action = action;
        this.radius = radius;
        this.drop = drop;
        if (target != null) targets.add(target);
    }

    public TerminalRequestMessage(List<String> targets, boolean drop) {
        this(SET_ORE_TARGETS, targets, 0, drop);
    }

    private TerminalRequestMessage(byte action, List<String> targets, int radius, boolean drop) {
        this.action = action;
        this.radius = radius;
        this.drop = drop;
        this.targets = new ArrayList<>(targets);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
        radius = buf.readInt();
        drop = buf.readBoolean();
        int count = Math.min(buf.readUnsignedShort(), MAX_TARGETS);
        for (int i = 0; i < count; i++) {
            int length = Math.min(buf.readUnsignedShort(), MAX_TARGET_LENGTH);
            byte[] bytes = new byte[length];
            buf.readBytes(bytes);
            targets.add(new String(bytes, Charset.forName("UTF-8")));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
        buf.writeInt(radius);
        buf.writeBoolean(drop);
        int count = Math.min(targets.size(), MAX_TARGETS);
        buf.writeShort(count);
        for (int i = 0; i < count; i++) {
            byte[] bytes = targets.get(i).getBytes(Charset.forName("UTF-8"));
            int length = Math.min(bytes.length, MAX_TARGET_LENGTH);
            buf.writeShort(length);
            buf.writeBytes(bytes, 0, length);
        }
    }

    public static class Handler implements IMessageHandler<TerminalRequestMessage, IMessage> {
        @Override
        public IMessage onMessage(TerminalRequestMessage message, MessageContext ctx) {
            final java.util.UUID playerId = ctx.getServerHandler().playerEntity.getUniqueID();
            final byte action = message.action;
            final int radius = message.radius;
            final boolean drop = message.drop;
            final List<String> targets = new ArrayList<>(message.targets);
            com.yukar1n.x_tracer.CommonProxy.serverTaskQueue.enqueue(new Runnable() {
                @Override public void run() {
                    EntityPlayerMP player = com.yukar1n.x_tracer.CommonProxy.findOnlinePlayer(playerId);
                    if (player == null) return;
                    ItemStack held = player.getHeldItem();
                    if (held == null || held.getItem() != ItemXTracerTerminal.INSTANCE) {
                        error(player, "Hold the terminal first.");
                        return;
                    }
                    if (action == REQUEST_STATE) {
                        sendState(player);
                        return;
                    }
                    TerminalRequestMessage request = new TerminalRequestMessage(action, targets, radius, drop);
                    if (action == SCAN) com.yukar1n.x_tracer.server.ScanService.enqueueOnServerThread(player);
                    else handle(request, player);
                }
            });
            return null;
        }

        private void sendState(EntityPlayerMP player) {
            com.yukar1n.x_tracer.ClientConfig config = Config.getPlayerConfig(player.getUniqueID());
            String block = config.targetList.isEmpty() ? "" : config.targetList.get(0);
            String ores = join(config.targetOreNameList);
            CommonProxy.network.sendTo(new TerminalStateMessage(config.radius, "True".equals(config.getDrop),
                config.curMatchMode, block, ores), player);
        }

        private String join(List<String> values) {
            StringBuilder result = new StringBuilder();
            for (String value : values) {
                if (result.length() > 0) result.append('\n');
                result.append(value);
            }
            return result.toString();
        }

        private void handle(TerminalRequestMessage message, EntityPlayerMP player) {
            if (message.action == SET_BLOCK) {
                if (message.targets.size() != 1) {
                    error(player, "Invalid block target.");
                    return;
                }
                String target = message.targets.get(0);
                if (target.startsWith("drop:")) {
                    String[] parts = target.substring(5).split("@", -1);
                    String itemName = parts.length == 2 ? parts[0] : "";
                    int damage;
                    try { damage = parts.length == 2 ? Integer.parseInt(parts[1]) : -1; }
                    catch (NumberFormatException e) { damage = -1; }
                    Item item = itemName.length() == 0 ? null : (Item) Item.itemRegistry.getObject(itemName);
                    if (item == null || damage < 0 || damage > 32767) {
                        error(player, "Invalid drop item target.");
                        return;
                    }
                    Config.getPlayerConfig(player.getUniqueID()).curMatchMode = "gtName";
                    Config.getPlayerConfig(player.getUniqueID()).targetGtName = item.getUnlocalizedName() + "@" + damage;
                    player.addChatMessage(new ChatComponentText("§aXTracer terminal: setting updated."));
                    return;
                }
                String[] parts = target.split("@", -1);
                String[] id = parts.length == 2 ? parts[0].split(":", -1) : new String[0];
                int meta;
                try { meta = parts.length == 2 ? Integer.parseInt(parts[1]) : -1; }
                catch (NumberFormatException e) { meta = -1; }
                Block block = id.length == 2 ? GameRegistry.findBlock(id[0], id[1]) : null;
                Item blockItem = block == null ? null : Item.getItemFromBlock(block);
                if (block == null || blockItem == null || meta < 0 || meta > 15 || blockItem.getMetadata(meta) < 0) {
                    error(player, "Invalid block target.");
                    return;
                }
                List<String> targetList = new ArrayList<>();
                targetList.add(target);
                Config.getPlayerConfig(player.getUniqueID()).curMatchMode = "block";
                Config.getPlayerConfig(player.getUniqueID()).setTargetBlockID(targetList);
            } else if (message.action == SET_ORE || message.action == SET_ORE_TARGETS) {
                if (message.targets.isEmpty() || message.targets.size() > MAX_TARGETS) {
                    error(player, "Select at least one ore dictionary name.");
                    return;
                }
                Set<String> normalized = new LinkedHashSet<>();
                for (String value : message.targets) {
                    String name = OreNames.resolve(value);
                    if (name == null || name.length() > MAX_TARGET_LENGTH) {
                        error(player, "Invalid ore dictionary name: " + value);
                        return;
                    }
                    normalized.add(name);
                }
                Config.getPlayerConfig(player.getUniqueID()).curMatchMode = "ore";
                Config.getPlayerConfig(player.getUniqueID()).setTargetOreName(new ArrayList<>(normalized));
            } else if (message.action == SET_RADIUS) {
                if (message.radius < 1 || message.radius > Config.getPlayerConfig(player.getUniqueID()).maxRadius) return;
                Config.getPlayerConfig(player.getUniqueID()).radius = message.radius;
            } else if (message.action == SET_DROP) {
                Config.getPlayerConfig(player.getUniqueID()).getDrop = message.drop ? "True" : "False";
            } else if (message.action == RAY) {
                final java.util.UUID playerId = player.getUniqueID();
                com.yukar1n.x_tracer.CommonProxy.serverTaskQueue.enqueue(new Runnable() {
                    @Override public void run() {
                        EntityPlayerMP current = com.yukar1n.x_tracer.CommonProxy.findOnlinePlayer(playerId);
                        if (current != null) com.yukar1n.x_tracer.Util.setRayTrace(current);
                    }
                });
                return;
            }
            player.addChatMessage(new ChatComponentText("§aXTracer terminal: setting updated."));
        }

        private static void error(EntityPlayerMP player, String message) {
            player.addChatMessage(new ChatComponentText("§cXTracer: " + message));
        }
    }
}
