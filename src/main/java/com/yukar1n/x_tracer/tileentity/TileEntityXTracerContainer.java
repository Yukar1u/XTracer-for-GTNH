package com.yukar1n.x_tracer.tileentity;

import com.yukar1n.x_tracer.storage.ItemKey;
import com.yukar1n.x_tracer.storage.LongItemStorage;
import com.yukar1n.x_tracer.storage.RoutingMatcher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TileEntityXTracerContainer extends TileEntity implements ISidedInventory {
    public static final int MAX_EXPRESSIONS = 32;
    private final LongItemStorage storage = new LongItemStorage();
    private final List<String> accepts = new ArrayList<>();
    private final List<String> excludes = new ArrayList<>();
    private String containerUuid = UUID.randomUUID().toString();
    private String ownerUuid = "";
    private String customName = "";
    private long revision;

    public String getContainerUuid() { return containerUuid; }
    public String getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(UUID owner) { ownerUuid = owner == null ? "" : owner.toString(); markDirty(); }
    public String getCustomName() { return customName; }
    public void setCustomName(String value) {
        String normalized = sanitize(value, 32);
        if (!customName.equals(normalized)) { customName = normalized; revision++; markDirty(); }
    }
    public List<String> getAccepts() { return Collections.unmodifiableList(new ArrayList<>(accepts)); }
    public List<String> getExcludes() { return Collections.unmodifiableList(new ArrayList<>(excludes)); }
    public long getRevision() { return revision; }
    public LongItemStorage storage() { return storage; }

    public boolean accepts(ItemKey key) { return RoutingMatcher.matches(key, accepts, excludes); }

    public boolean setExpressions(List<String> acceptValues, List<String> excludeValues, long expectedRevision) {
        if (expectedRevision != revision) return false;
        List<String> newAccepts = bounded(acceptValues);
        List<String> newExcludes = bounded(excludeValues);
        accepts.clear(); accepts.addAll(newAccepts);
        excludes.clear(); excludes.addAll(newExcludes);
        revision++;
        markDirty();
        return true;
    }

    public boolean canDeposit(ItemKey key, long amount) {
        if (key == null || amount <= 0L) return false;
        long current = storage.get(key);
        return Long.MAX_VALUE - current >= amount;
    }

    public boolean deposit(ItemKey key, long amount) {
        if (!storage.add(key, amount)) return false;
        revision++;
        markDirty();
        return true;
    }

    public long withdraw(ItemKey key, long amount) {
        long removed = storage.remove(key, amount);
        if (removed > 0L) { revision++; markDirty(); }
        return removed;
    }

    public LongItemStorage.Entry entryAt(int index) {
        List<LongItemStorage.Entry> entries = storage.entries();
        return index < 0 || index >= entries.size() ? null : entries.get(index);
    }

    public ItemStack withdrawSlot(int index, int amount) {
        LongItemStorage.Entry entry = entryAt(index);
        if (entry == null) return null;
        long removed = withdraw(entry.key(), Math.min(64L, Math.max(1L, amount)));
        return removed <= 0L ? null : entry.key().toStack((int) removed);
    }

    public long storedAmount(ItemKey key) { return storage.get(key); }

    private static String sanitize(String value, int max) {
        if (value == null) return "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length() && result.length() < max; i++) {
            char c = value.charAt(i);
            if (c >= 32 && c != '\n' && c != '\r') result.append(c);
        }
        return result.toString().trim();
    }

    private static List<String> bounded(List<String> values) {
        List<String> result = new ArrayList<>();
        if (values == null) return result;
        for (String value : values) {
            String normalized = sanitize(value, 64);
            if (!normalized.isEmpty() && !result.contains(normalized) && result.size() < MAX_EXPRESSIONS) result.add(normalized);
        }
        return result;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        containerUuid = compound.getString("ContainerUuid");
        if (containerUuid.isEmpty()) containerUuid = UUID.randomUUID().toString();
        ownerUuid = compound.getString("OwnerUuid");
        customName = sanitize(compound.getString("CustomName"), 32);
        revision = compound.getLong("Revision");
        accepts.clear(); excludes.clear();
        readExpressions(compound, "Accepts", accepts);
        readExpressions(compound, "Excludes", excludes);
        storage.readFromNBT(compound);
    }

    private void readExpressions(NBTTagCompound compound, String key, List<String> target) {
        if (!compound.hasKey(key, 9)) return;
        NBTTagList list = (NBTTagList) compound.getTag(key);
        for (int i = 0; i < list.tagCount() && target.size() < MAX_EXPRESSIONS; i++) {
            String value = sanitize(list.getStringTagAt(i), 64);
            if (!value.isEmpty() && !target.contains(value)) target.add(value);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("DataVersion", 1);
        compound.setString("ContainerUuid", containerUuid);
        compound.setString("OwnerUuid", ownerUuid);
        compound.setString("CustomName", customName);
        compound.setLong("Revision", revision);
        writeExpressions(compound, "Accepts", accepts);
        writeExpressions(compound, "Excludes", excludes);
        storage.writeToNBT(compound);
    }

    private void writeExpressions(NBTTagCompound compound, String key, List<String> values) {
        NBTTagList list = new NBTTagList();
        for (String value : values) list.appendTag(new net.minecraft.nbt.NBTTagString(value));
        compound.setTag(key, list);
    }

    @Override public int getSizeInventory() { return storage.entries().size(); }
    @Override public ItemStack getStackInSlot(int slot) {
        List<LongItemStorage.Entry> entries = storage.entries();
        return slot < 0 || slot >= entries.size() ? null : entries.get(slot).displayStack();
    }
    @Override public ItemStack decrStackSize(int slot, int count) {
        if (count <= 0) return null;
        List<LongItemStorage.Entry> entries = storage.entries();
        if (slot < 0 || slot >= entries.size()) return null;
        LongItemStorage.Entry entry = entries.get(slot);
        long removed = storage.remove(entry.key(), Math.min((long) count, 64L));
        if (removed > 0L) { revision++; markDirty(); return entry.key().toStack((int) removed); }
        return null;
    }
    @Override public ItemStack getStackInSlotOnClosing(int slot) { return null; }
    @Override public void setInventorySlotContents(int slot, ItemStack stack) { }
    @Override public String getInventoryName() { return customName.isEmpty() ? "container.x_tracer" : customName; }
    @Override public boolean hasCustomInventoryName() { return !customName.isEmpty(); }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public void markDirty() { super.markDirty(); }
    @Override public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj != null && worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
            && player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64.0D;
    }
    @Override public void openInventory() { }
    @Override public void closeInventory() { }
    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }
    @Override public int[] getAccessibleSlotsFromSide(int side) {
        int size = getSizeInventory();
        int[] slots = new int[size];
        for (int i = 0; i < size; i++) slots[i] = i;
        return slots;
    }
    @Override public boolean canInsertItem(int slot, ItemStack stack, int side) { return false; }
    @Override public boolean canExtractItem(int slot, ItemStack stack, int side) { return true; }
}
