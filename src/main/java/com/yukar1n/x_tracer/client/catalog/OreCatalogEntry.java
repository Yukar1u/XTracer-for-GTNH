package com.yukar1n.x_tracer.client.catalog;

import net.minecraft.item.ItemStack;

public class OreCatalogEntry {
    public final String name;
    public final ItemStack representative;

    public OreCatalogEntry(String name, ItemStack representative) {
        this.name = name;
        this.representative = representative;
    }

    public String searchText() {
        return name.toLowerCase();
    }
}
