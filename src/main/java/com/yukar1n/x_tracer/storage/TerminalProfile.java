package com.yukar1n.x_tracer.storage;

import com.yukar1n.x_tracer.ClientConfig;
import com.yukar1n.x_tracer.block.BlockXTracerContainer;
import com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;

public final class TerminalProfile {
    private static final String ROOT = "XTracerProfile";
    public static final int MAX_BINDINGS = 16;

    private TerminalProfile() {}

    public static NBTTagCompound root(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        NBTTagCompound root = stack.getTagCompound().getCompoundTag(ROOT);
        stack.getTagCompound().setTag(ROOT, root);
        return root;
    }

    public static void initializeFromConfig(ItemStack stack, ClientConfig config) {
        NBTTagCompound root = root(stack);
        if (root.hasKey("Initialized")) return;
        root.setBoolean("Initialized", true);
        root.setInteger("Radius", config.radius);
        root.setString("Mode", config.curMatchMode);
        root.setBoolean("Drop", "True".equals(config.getDrop));
        root.setString("GtTarget", config.targetGtName == null ? "" : config.targetGtName);
        writeStrings(root, "Blocks", config.targetList);
        writeStrings(root, "Ores", config.targetOreNameList);
    }

    public static int radius(ItemStack stack, ClientConfig fallback) {
        NBTTagCompound root = root(stack);
        return root.hasKey("Radius") ? root.getInteger("Radius") : fallback.radius;
    }

    public static void setRadius(ItemStack stack, int radius) { root(stack).setInteger("Radius", radius); }
    public static boolean drop(ItemStack stack, ClientConfig fallback) {
        NBTTagCompound root = root(stack);
        return root.hasKey("Drop") ? root.getBoolean("Drop") : "True".equals(fallback.getDrop);
    }
    public static void setDrop(ItemStack stack, boolean drop) { root(stack).setBoolean("Drop", drop); }
    public static boolean remoteInput(ItemStack stack) { return root(stack).getBoolean("RemoteInput"); }
    public static void setRemoteInput(ItemStack stack, boolean value) { root(stack).setBoolean("RemoteInput", value); }
    public static int activeBinding(ItemStack stack) { return root(stack).getInteger("ActiveBinding"); }
    public static void setActiveBinding(ItemStack stack, int value) { root(stack).setInteger("ActiveBinding", Math.max(0, value)); }

    public static List<ContainerReference> bindings(ItemStack stack) {
        List<ContainerReference> result = new ArrayList<>();
        NBTTagCompound root = root(stack);
        if (!root.hasKey("Bindings", 9)) return result;
        NBTTagList list = (NBTTagList) root.getTag("Bindings");
        for (int i = 0; i < list.tagCount() && result.size() < MAX_BINDINGS; i++) {
            result.add(ContainerReference.readFromNBT(list.getCompoundTagAt(i)));
        }
        return result;
    }

    /** Looks up only loaded worlds; worldServerForDimension can initialize an unavailable dimension. */
    public static WorldServer loadedWorld(int dimension) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.worldServers == null) return null;
        for (WorldServer world : server.worldServers) {
            if (world != null && !world.isRemote && world.provider.dimensionId == dimension) return world;
        }
        return null;
    }

    /** Server-thread only: keep unchecked references until their world and chunk are loaded. */
    public static List<ContainerReference> pruneBindings(ItemStack stack) {
        List<ContainerReference> previous = bindings(stack);
        List<ContainerReference> retained = new ArrayList<>();
        int active = activeBinding(stack);
        int nextActive = -1;
        for (int i = 0; i < previous.size(); i++) {
            ContainerReference reference = previous.get(i);
            WorldServer world = loadedWorld(reference.dimension);
            if (world != null && world.getChunkProvider().chunkExists(reference.x >> 4, reference.z >> 4)) {
                if (world.getBlock(reference.x, reference.y, reference.z) != BlockXTracerContainer.INSTANCE) continue;
                TileEntity tile = world.getTileEntity(reference.x, reference.y, reference.z);
                if (!(tile instanceof TileEntityXTracerContainer)
                    || !reference.uuid.equals(((TileEntityXTracerContainer) tile).getContainerUuid())) continue;
            }
            if (i == active) nextActive = retained.size();
            retained.add(reference);
        }
        if (retained.size() != previous.size()) setBindings(stack, retained);
        // Preserve the selected node as preceding entries disappear; fall back safely if it was removed.
        if (nextActive < 0) nextActive = Math.max(0, Math.min(active, retained.size() - 1));
        if (nextActive != active) setActiveBinding(stack, nextActive);
        return retained;
    }

    public static void setBindings(ItemStack stack, List<ContainerReference> bindings) {
        NBTTagList list = new NBTTagList();
        int count = 0;
        for (ContainerReference reference : bindings) {
            if (reference == null || count++ >= MAX_BINDINGS) break;
            NBTTagCompound value = new NBTTagCompound();
            reference.writeToNBT(value);
            list.appendTag(value);
        }
        root(stack).setTag("Bindings", list);
    }

    public static void writeStrings(NBTTagCompound root, String key, List<String> values) {
        NBTTagList list = new NBTTagList();
        if (values != null) for (String value : values) if (value != null) list.appendTag(new net.minecraft.nbt.NBTTagString(value));
        root.setTag(key, list);
    }
}
