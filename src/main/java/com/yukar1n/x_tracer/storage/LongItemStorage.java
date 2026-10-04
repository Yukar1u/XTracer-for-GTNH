package com.yukar1n.x_tracer.storage;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LongItemStorage {
    private final Map<ItemKey, Long> amounts = new LinkedHashMap<>();

    public long get(ItemKey key) {
        Long value = amounts.get(key);
        return value == null ? 0L : value;
    }

    public boolean add(ItemKey key, long amount) {
        if (key == null || amount < 0) return false;
        if (amount == 0) return true;
        long old = get(key);
        if (Long.MAX_VALUE - old < amount) return false;
        amounts.put(key, old + amount);
        return true;
    }

    public long remove(ItemKey key, long amount) {
        if (key == null || amount <= 0) return 0L;
        long old = get(key);
        long removed = Math.min(old, amount);
        long left = old - removed;
        if (left == 0L) amounts.remove(key);
        else amounts.put(key, left);
        return removed;
    }

    public List<ItemKey> keys() {
        return new ArrayList<>(amounts.keySet());
    }

    public List<Entry> entries() {
        List<Entry> result = new ArrayList<>();
        for (Map.Entry<ItemKey, Long> entry : amounts.entrySet()) {
            result.add(new Entry(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    public void clear() { amounts.clear(); }

    public void writeToNBT(NBTTagCompound out) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<ItemKey, Long> entry : amounts.entrySet()) {
            if (entry.getValue() <= 0L) continue;
            NBTTagCompound item = new NBTTagCompound();
            entry.getKey().writeToNBT(item);
            item.setLong("Count", entry.getValue());
            list.appendTag(item);
        }
        out.setTag("Items", list);
    }

    public void readFromNBT(NBTTagCompound in) {
        amounts.clear();
        if (!in.hasKey("Items", 9)) return;
        NBTTagList list = (NBTTagList) in.getTag("Items");
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound item = list.getCompoundTagAt(i);
            ItemKey key = ItemKey.readFromNBT(item);
            long count = item.getLong("Count");
            if (key.itemId().isEmpty() || count <= 0L || key.toStack(1) == null) continue;
            if (!add(key, count)) break;
        }
    }

    public static final class Entry {
        private final ItemKey key;
        private final long count;
        private Entry(ItemKey key, long count) { this.key = key; this.count = count; }
        public ItemKey key() { return key; }
        public long count() { return count; }
        public ItemStack displayStack() { return key.toStack((int) Math.min(64L, count)); }
    }
}
