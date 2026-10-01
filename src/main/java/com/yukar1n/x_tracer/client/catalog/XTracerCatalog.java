package com.yukar1n.x_tracer.client.catalog;

import com.yukar1n.x_tracer.MyMod;
import com.yukar1n.x_tracer.SelectorLogic;
import com.yukar1n.x_tracer.Util;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import java.util.*;

public final class XTracerCatalog {
    private static List<BlockCatalogEntry> blocks;
    private static List<OreCatalogEntry> ores;
    private XTracerCatalog() {}

    public static synchronized void ensureBuilt() {
        if (blocks != null && ores != null) return;
        blocks = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        int failures = 0, skipped = 0, dropCount = 0;
        for (Object value : Block.blockRegistry) {
            Block block = (Block) value;
            String id = Block.blockRegistry.getNameForObject(block);
            Item item = Item.getItemFromBlock(block);
            if (id == null || block == net.minecraft.init.Blocks.air || item == null) { skipped++; continue; }
            List<ItemStack> variants = new ArrayList<>();
            try {
                for (CreativeTabs tab : item.getCreativeTabs()) {
                    if (tab != null) item.getSubItems(item, tab, variants);
                }
                if (variants.isEmpty()) block.getSubBlocks(item, CreativeTabs.tabAllSearch, variants);
            } catch (Throwable error) {
                failures++;
                MyMod.LOG.warn("Catalog enumeration failed: " + id, error);
            }
            if (variants.isEmpty()) variants.add(new ItemStack(item, 1, 0));
            for (ItemStack stack : variants) {
                if (stack == null || stack.getItem() != item || stack.getItemDamage() < 0
                    || stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) continue;
                try {
                    if (Block.getBlockFromItem(item) != block) continue;
                    int meta = item.getMetadata(stack.getItemDamage());
                    boolean special = SelectorLogic.needsDropTarget(Util.isSpecialVariantBlock(block), stack.getItemDamage(), meta);
                    BlockCatalogEntry entry = new BlockCatalogEntry(id, meta, stack.copy(), special);
                    if (keys.add(entry.target())) { blocks.add(entry); if (special) dropCount++; }
                } catch (Throwable error) {
                    failures++;
                    MyMod.LOG.warn("Catalog variant failed: " + id + "@" + stack.getItemDamage(), error);
                }
            }
        }
        Collections.sort(blocks, (a, b) -> a.searchText().compareTo(b.searchText()));
        ores = new ArrayList<>();
        for (String name : OreDictionary.getOreNames()) {
            if (!SelectorLogic.matchKey(name).startsWith("ore")) continue;
            if (!OreDictionary.getOres(name).isEmpty()) ores.add(new OreCatalogEntry(name));
        }
        Collections.sort(ores, (a, b) -> a.name.compareTo(b.name));
        MyMod.LOG.info("Catalog coverage: " + blocks.size() + " targets; " + dropCount
            + " DROP_ITEM; " + ores.size() + " ore names; " + failures + " API errors; " + skipped
            + " blocks without item/air. Creative/API exposed variants only, not exhaustive.");
    }
    public static List<BlockCatalogEntry> blocks() { ensureBuilt(); return blocks; }
    public static List<OreCatalogEntry> ores() { ensureBuilt(); return ores; }
}
