package com.yukar1n.x_tracer.client.catalog;

import net.minecraft.item.ItemStack;

public class BlockCatalogEntry {
    public final String registryName;
    public final int metadata;
    public final String displayName;
    public final ItemStack stack;

    public BlockCatalogEntry(String registryName, int metadata, ItemStack stack) {
        this.registryName = registryName;
        this.metadata = metadata;
        this.stack = stack;
        this.displayName = stack.getDisplayName();
    }

    public String target() {
        return registryName + "@" + metadata;
    }

    public String searchText() {
        return (displayName + " " + registryName + " " + metadata).toLowerCase();
    }
}
