package com.yukar1n.x_tracer.client.catalog;

import com.yukar1n.x_tracer.SelectorLogic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class BlockCatalogEntry {
    public final String registryName;
    public final int metadata;
    public final String displayName;
    public final ItemStack stack;
    public final boolean dropItem;
    private final String searchText;

    public BlockCatalogEntry(String registryName, int metadata, ItemStack stack, boolean dropItem) {
        this.registryName = registryName;
        this.metadata = metadata;
        this.stack = stack;
        this.displayName = stack.getDisplayName();
        this.dropItem = dropItem;
        StringBuilder fields = new StringBuilder(displayName)
            .append(' ').append(registryName)
            .append(' ').append(stack.getItem().getUnlocalizedName())
            .append(' ').append(stack.getItemDamage())
            .append(' ').append(metadata);
        try {
            for (int oreId : OreDictionary.getOreIDs(stack)) {
                fields.append(' ').append(OreDictionary.getOreName(oreId));
            }
        } catch (Throwable ignored) {
            // Some third-party stacks expose invalid ore data; basic fields remain searchable.
        }
        this.searchText = SelectorLogic.matchKey(fields.toString());
    }

    public String target() {
        return dropItem ? "drop:" + Item.itemRegistry.getNameForObject(stack.getItem()) + "@" + stack.getItemDamage()
            : registryName + "@" + metadata;
    }

    public String searchText() {
        return searchText;
    }
}
