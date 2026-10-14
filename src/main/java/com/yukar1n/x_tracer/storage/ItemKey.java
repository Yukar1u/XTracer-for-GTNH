package com.yukar1n.x_tracer.storage;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Locale;
import java.util.Objects;

public final class ItemKey {
    private final String itemId;
    private final int damage;
    private final NBTTagCompound tag;
    private final String tagText;

    public ItemKey(String itemId, int damage, NBTTagCompound tag) {
        this.itemId = itemId == null ? "" : itemId;
        this.damage = damage;
        this.tag = tag == null ? null : (NBTTagCompound) tag.copy();
        this.tagText = this.tag == null ? "" : this.tag.toString();
    }

    public static ItemKey fromStack(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        String id = Item.itemRegistry.getNameForObject(stack.getItem());
        return new ItemKey(id, stack.getItemDamage(), stack.getTagCompound());
    }

    public ItemStack toStack(int count) {
        Item item = (Item) Item.itemRegistry.getObject(itemId);
        if (item == null || count <= 0) return null;
        ItemStack stack = new ItemStack(item, count, damage);
        if (tag != null) stack.setTagCompound((NBTTagCompound) tag.copy());
        return stack;
    }

    public String itemId() { return itemId; }
    public int damage() { return damage; }
    public NBTTagCompound tag() { return tag == null ? null : (NBTTagCompound) tag.copy(); }

    public String displayText() {
        try {
            ItemStack stack = toStack(1);
            if (stack != null) {
                String name = stack.getItem().getItemStackDisplayName(stack);
                if (name != null && !name.isEmpty()) return name;
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Some mod names require client-only classes or an initialized translation system.
        }
        return itemId;
    }

    public String matchText() {
        return matchText(displayText());
    }

    /** The initiating client's actual display name replaces the server's translation. */
    public String matchText(String localizedDisplayName) {
        StringBuilder text = new StringBuilder(itemId).append(' ').append(itemId).append('@').append(damage);
        try {
            ItemStack stack = toStack(1);
            if (stack != null) {
                String name = stack.getItem().getUnlocalizedName(stack);
                if (name != null && !name.isEmpty()) text.append(' ').append(name).append('@').append(damage);
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Keep registry identifiers matchable if this optional name is unavailable.
        }
        if (localizedDisplayName != null && !localizedDisplayName.isEmpty()) text.append(' ').append(localizedDisplayName);
        return text.toString().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ItemKey)) return false;
        ItemKey key = (ItemKey) other;
        return damage == key.damage && itemId.equals(key.itemId) && tagText.equals(key.tagText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, damage, tagText);
    }

    public void writeToNBT(NBTTagCompound out) {
        out.setString("Item", itemId);
        out.setInteger("Damage", damage);
        if (tag != null) out.setTag("Tag", tag.copy());
    }

    public static ItemKey readFromNBT(NBTTagCompound in) {
        NBTTagCompound tag = in.hasKey("Tag", 10) ? (NBTTagCompound) in.getTag("Tag") : null;
        return new ItemKey(in.getString("Item"), in.getInteger("Damage"), tag);
    }
}
