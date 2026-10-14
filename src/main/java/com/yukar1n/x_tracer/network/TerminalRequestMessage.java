package com.yukar1n.x_tracer.network;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.Config;
import com.yukar1n.x_tracer.item.ItemXTracerTerminal;
import com.yukar1n.x_tracer.server.ChatFeedback;
import com.yukar1n.x_tracer.storage.ContainerReference;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.registry.GameRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import com.yukar1n.x_tracer.OreNames;
import com.yukar1n.x_tracer.storage.TerminalProfile;

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
    public static final byte SET_REMOTE_INPUT = 9;
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
                        ChatFeedback.send(player, "xtracer.error.hold_terminal");
                        return;
                    }
                    if (action == REQUEST_STATE) {
                        sendState(player);
                        sendContainers(player, held);
                        return;
                    }
                    if (action == SET_REMOTE_INPUT) {
                        TerminalProfile.setRemoteInput(held, drop);
                        int bindingCount = TerminalProfile.bindings(held).size();
                        ChatFeedback.send(player, drop ? "xtracer.remote.enabled" : "xtracer.remote.disabled", bindingCount);
                        if (drop) {
                            if (bindingCount == 0) ChatFeedback.send(player, "xtracer.remote.bind_first");
                            if (!TerminalProfile.drop(held, Config.getPlayerConfig(playerId))) {
                                ChatFeedback.send(player, "xtracer.remote.scan_only");
                            }
                        }
                        return;
                    }
                    TerminalRequestMessage request = new TerminalRequestMessage(action, targets, radius, drop);
                    if (action == SCAN) {
                        com.yukar1n.x_tracer.server.ScanService.enqueueOnServerThread(player);
                        sendContainers(player, held);
                    } else handle(request, player);
                }
            });
            return null;
        }

        private void sendState(EntityPlayerMP player) {
            com.yukar1n.x_tracer.ClientConfig config = Config.getPlayerConfig(player.getUniqueID());
            ItemStack held = player.getHeldItem();
            if (held != null && held.getItem() == ItemXTracerTerminal.INSTANCE) TerminalProfile.initializeFromConfig(held, config);
            String block = config.targetList.isEmpty() ? "" : config.targetList.get(0);
            String ores = join(config.targetOreNameList);
            CommonProxy.network.sendTo(new TerminalStateMessage(TerminalProfile.radius(held, config), TerminalProfile.drop(held, config),
                config.curMatchMode, block, ores, TerminalProfile.remoteInput(held)), player);
        }

        private void sendContainers(EntityPlayerMP player, ItemStack held) {
            List<ContainerReference> bindings = TerminalProfile.pruneBindings(held);
            // GUI rows read item NBT by index: synchronize the pruned list before its summaries.
            player.sendContainerToPlayer(player.inventoryContainer);
            List<String> summaries = new ArrayList<>();
            for (ContainerReference reference : bindings) {
                String location = " | dimension " + reference.dimension + " @ " + reference.x + "," + reference.y + "," + reference.z;
                String defaultName = "Hyperspace Quantum Containment Node";
                net.minecraft.world.WorldServer world = TerminalProfile.loadedWorld(reference.dimension);
                if (world == null) {
                    summaries.add(defaultName + location + " | dimension unavailable; node status not checked");
                    continue;
                }
                if (!world.getChunkProvider().chunkExists(reference.x >> 4, reference.z >> 4)) {
                    summaries.add(defaultName + location + " | chunk not loaded; node status not checked");
                    continue;
                }
                net.minecraft.tileentity.TileEntity tile = world.getTileEntity(reference.x, reference.y, reference.z);
                if (!(tile instanceof com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer)) {
                    summaries.add(defaultName + location + " | node missing");
                    continue;
                }
                com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer container = (com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer) tile;
                String name = container.getCustomName().isEmpty() ? defaultName : container.getCustomName();
                if (!reference.uuid.equals(container.getContainerUuid())) {
                    summaries.add(name + location + " | UUID mismatch; node replaced, rebind required");
                    continue;
                }
                summaries.add(name + location
                    + " | accept: " + (container.getAccepts().isEmpty() ? "all items" : join(container.getAccepts()))
                    + " | exclude (takes priority): " + join(container.getExcludes()));
            }
            CommonProxy.network.sendTo(new TerminalContainersMessage(summaries), player);
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
                    ChatFeedback.send(player, "xtracer.target.invalid_block");
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
                        ChatFeedback.send(player, "xtracer.target.invalid_drop", target);
                        return;
                    }
                    Config.getPlayerConfig(player.getUniqueID()).curMatchMode = "gtName";
                    Config.getPlayerConfig(player.getUniqueID()).targetGtName = item.getUnlocalizedName() + "@" + damage;
                    ChatFeedback.send(player, "xtracer.setting.drop_target", target.substring(5));
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
                    ChatFeedback.send(player, "xtracer.target.invalid_block");
                    return;
                }
                List<String> targetList = new ArrayList<>();
                targetList.add(target);
                Config.getPlayerConfig(player.getUniqueID()).curMatchMode = "block";
                Config.getPlayerConfig(player.getUniqueID()).setTargetBlockID(targetList);
                ChatFeedback.send(player, "xtracer.setting.block_target", target);
            } else if (message.action == SET_ORE || message.action == SET_ORE_TARGETS) {
                if (message.targets.isEmpty() || message.targets.size() > MAX_TARGETS) {
                    ChatFeedback.send(player, "xtracer.target.ore_count", MAX_TARGETS);
                    return;
                }
                Set<String> normalized = new LinkedHashSet<>();
                for (String value : message.targets) {
                    String name = OreNames.resolve(value);
                    if (name == null || name.length() > MAX_TARGET_LENGTH) {
                        ChatFeedback.send(player, "xtracer.target.invalid_ore", value);
                        return;
                    }
                    normalized.add(name);
                }
                Config.getPlayerConfig(player.getUniqueID()).curMatchMode = "ore";
                Config.getPlayerConfig(player.getUniqueID()).setTargetOreName(new ArrayList<>(normalized));
                ChatFeedback.send(player, "xtracer.setting.ore_targets", normalized.size(), join(new ArrayList<>(normalized)));
            } else if (message.action == SET_RADIUS) {
                int maxRadius = Config.getPlayerConfig(player.getUniqueID()).maxRadius;
                if (message.radius < 1 || message.radius > maxRadius) {
                    ChatFeedback.send(player, "xtracer.setting.invalid_radius", message.radius, 1, maxRadius);
                    return;
                }
                Config.getPlayerConfig(player.getUniqueID()).radius = message.radius;
                TerminalProfile.setRadius(player.getHeldItem(), message.radius);
                ChatFeedback.send(player, "xtracer.setting.radius", message.radius);
            } else if (message.action == SET_DROP) {
                Config.getPlayerConfig(player.getUniqueID()).getDrop = message.drop ? "True" : "False";
                TerminalProfile.setDrop(player.getHeldItem(), message.drop);
                ChatFeedback.send(player, "xtracer.setting.drop", new ChatComponentTranslation(
                    message.drop ? "xtracer.state.enabled" : "xtracer.state.disabled"));
                if (!message.drop) ChatFeedback.send(player, "xtracer.remote.scan_only");
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
        }
    }
}
