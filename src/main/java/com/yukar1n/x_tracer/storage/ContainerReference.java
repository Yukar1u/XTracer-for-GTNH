package com.yukar1n.x_tracer.storage;

import com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public final class ContainerReference {
    public final int dimension;
    public final int x;
    public final int y;
    public final int z;
    public final String uuid;
    public final boolean routeEnabled;

    public ContainerReference(int dimension, int x, int y, int z, String uuid) {
        this(dimension, x, y, z, uuid, true);
    }

    public ContainerReference(int dimension, int x, int y, int z, String uuid, boolean routeEnabled) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z == 0 ? z : z;
        this.uuid = uuid == null ? "" : uuid;
        this.routeEnabled = routeEnabled;
    }

    public ContainerReference withRouteEnabled(boolean enabled) {
        return new ContainerReference(dimension, x, y, z, uuid, enabled);
    }

    public static ContainerReference from(World world, int x, int y, int z, TileEntityXTracerContainer tile) {
        return new ContainerReference(world.provider.dimensionId, x, y, z, tile.getContainerUuid());
    }

    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("Dimension", dimension);
        tag.setInteger("X", x);
        tag.setInteger("Y", y);
        tag.setInteger("Z", z);
        tag.setString("Uuid", uuid);
        tag.setBoolean("Enabled", routeEnabled);
    }

    public static ContainerReference readFromNBT(NBTTagCompound tag) {
        boolean enabled = !tag.hasKey("Enabled") || tag.getBoolean("Enabled");
        return new ContainerReference(tag.getInteger("Dimension"), tag.getInteger("X"),
            tag.getInteger("Y"), tag.getInteger("Z"), tag.getString("Uuid"), enabled);
    }

    public boolean sameLocation(ContainerReference other) {
        return other != null && dimension == other.dimension && x == other.x && y == other.y && z == other.z;
    }
}
