package com.yukar1n.x_tracer.client.catalog;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class XTracerCatalog {
    private static List<BlockCatalogEntry> blocks;
    private static List<OreCatalogEntry> ores;

    private XTracerCatalog() {}

    public static synchronized void ensureBuilt() {
        if (blocks != null && ores != null) return;
        blocks = new ArrayList<>();
        Set<String> blockKeys = new HashSet<>();
        for (Object value : Block.blockRegistry) {
            Block block = (Block) value;
            String registryName = Block.blockRegistry.getNameForObject(block);
            if (registryName == null || block == net.minecraft.init.Blocks.air) continue;
            Item item = Item.getItemFromBlock(block);
            if (item == null) continue;
            List<ItemStack> variants = new ArrayList<>();
            item.getSubItems(item, null, variants);
            if (variants.isEmpty()) {
                variants.add(new ItemStack(item, 1, 0));
            }
            for (ItemStack stack : variants) {
                if (stack == null || Item.getItemFromBlock(block) != stack.getItem()) continue;
                String key = registryName + "@" + stack.getItemDamage();
                if (blockKeys.add(key)) blocks.add(new BlockCatalogEntry(registryName, stack.getItemDamage(), stack.copy()));
            }
        }
        Collections.sort(blocks, new Comparator<BlockCatalogEntry>() {
            @Override public int compare(BlockCatalogEntry a, BlockCatalogEntry b) {
                return a.searchText().compareTo(b.searchText());
            }
        });

        ores = new ArrayList<>();
        for (String name : OreDictionary.getOreNames()) {
            List<ItemStack> entries = OreDictionary.getOres(name);
            if (entries == null || entries.isEmpty()) continue;
            ores.add(new OreCatalogEntry(name.toLowerCase(), entries.get(0).copy()));
        }
        Collections.sort(ores, new Comparator<OreCatalogEntry>() {
            @Override public int compare(OreCatalogEntry a, OreCatalogEntry b) {
                return a.name.compareTo(b.name);
            }
        });
    }

    public static List<BlockCatalogEntry> blocks() {
        ensureBuilt();
        return blocks;
    }

    public static List<OreCatalogEntry> ores() {
        ensureBuilt();
        return ores;
    }
}
