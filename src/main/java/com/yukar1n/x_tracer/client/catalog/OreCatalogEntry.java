package com.yukar1n.x_tracer.client.catalog;

import com.yukar1n.x_tracer.SelectorLogic;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OreCatalogEntry {
    private static final int PREVIEW_SIZE = 6;
    public final String name;
    public final String matchKey;
    private List<ItemStack> preview;
    private int memberCount;

    public OreCatalogEntry(String name) {
        this.name = name;
        this.matchKey = SelectorLogic.matchKey(name);
    }

    public String searchText() {
        return matchKey;
    }

    private void cacheMembers() {
        if (preview != null) return;
        List<ItemStack> members = OreDictionary.getOres(name);
        memberCount = members.size();
        List<ItemStack> icons = new ArrayList<>();
        for (ItemStack stack : members) {
            if (stack == null || stack.getItem() == null) continue;
            // Wildcards describe a registration, not a concrete renderable item variant.
            if (stack.getItemDamage() < 0 || stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) continue;
            icons.add(stack.copy());
            if (icons.size() == PREVIEW_SIZE) break;
        }
        preview = Collections.unmodifiableList(icons);
    }

    public int memberCount() {
        cacheMembers();
        return memberCount;
    }

    public List<ItemStack> preview() {
        cacheMembers();
        return preview;
    }

    public ItemStack representative() {
        cacheMembers();
        return preview.isEmpty() ? null : preview.get(0);
    }
}
