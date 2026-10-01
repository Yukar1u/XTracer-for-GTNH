package com.yukar1n.x_tracer.client.catalog;

import com.yukar1n.x_tracer.SelectorLogic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class BlockCatalogEntry {
    public final String registryName;
    public final int metadata;
    public final String displayName;
    public final ItemStack stack;
    public final boolean dropItem;

    public BlockCatalogEntry(String registryName, int metadata, ItemStack stack, boolean dropItem) {
        this.registryName = registryName;
        this.metadata = metadata;
        this.stack = stack;
        this.displayName = stack.getDisplayName();
        this.dropItem = dropItem;
    }

    public String target() {
        return dropItem ? "drop:" + Item.itemRegistry.getNameForObject(stack.getItem()) + "@" + stack.getItemDamage()
            : registryName + "@" + metadata;
    }

    public String searchText() {
        return SelectorLogic.matchKey(displayName + " " + registryName + " " + stack.getItemDamage() + " " + metadata);
    }
}
