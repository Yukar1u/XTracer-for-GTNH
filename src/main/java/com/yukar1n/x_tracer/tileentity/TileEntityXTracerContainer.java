package com.yukar1n.x_tracer.tileentity;

import com.yukar1n.x_tracer.storage.ItemKey;
import com.yukar1n.x_tracer.storage.LongItemStorage;
import com.yukar1n.x_tracer.storage.RoutingMatcher;
import com.yukar1n.x_tracer.storage.RoutingExpression;
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
    private final List<OutputSlot> outputSlots = new ArrayList<>();

    private static final class OutputSlot {
        private final ItemKey key;
        private ItemStack stack;
        private int count;
        private int restorable;
        private OutputSlot(ItemKey key) { this.key = key; }
    }

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
    public LongItemStorage storage() { flushOutputChanges(); return storage; }
    public boolean isStorageEmpty() { flushOutputChanges(); return storage.keys().isEmpty(); }

    public RoutingMatcher.Decision evaluateRouting(ItemKey key) { return RoutingMatcher.evaluate(key, accepts, excludes); }
    public RoutingMatcher.Decision evaluateRouting(ItemKey key, String localizedDisplayName) {
        return RoutingMatcher.evaluate(key, accepts, excludes, localizedDisplayName);
    }
    public boolean accepts(ItemKey key) { return evaluateRouting(key).accepted; }

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
        flushOutputChanges();
        if (key == null || amount <= 0L) return false;
        long current = storage.get(key);
        return Long.MAX_VALUE - current >= amount;
    }

    public boolean deposit(ItemKey key, long amount) {
        flushOutputChanges();
        if (!storage.add(key, amount)) return false;
        revision++;
        refreshOutput(key);
        super.markDirty();
        return true;
    }

    public long withdraw(ItemKey key, long amount) {
        flushOutputChanges();
        long removed = storage.remove(key, amount);
        if (removed > 0L) { revision++; refreshOutput(key); super.markDirty(); }
        return removed;
    }

    public LongItemStorage.Entry entryAt(int index) {
        flushOutputChanges();
        List<LongItemStorage.Entry> entries = storage.entries();
        return index < 0 || index >= entries.size() ? null : entries.get(index);
    }

    public ItemStack withdrawSlot(int index, int amount) {
        LongItemStorage.Entry entry = entryAt(index);
        if (entry == null) return null;
        long removed = withdraw(entry.key(), Math.min(64L, Math.max(1L, amount)));
        return removed <= 0L ? null : entry.key().toStack((int) removed);
    }

    public long storedAmount(ItemKey key) { flushOutputChanges(); return storage.get(key); }

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
            String normalized = new RoutingExpression(sanitize(value, 64)).value();
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
        outputSlots.clear();
        storage.readFromNBT(compound);
    }

    private void readExpressions(NBTTagCompound compound, String key, List<String> target) {
        if (!compound.hasKey(key, 9)) return;
        NBTTagList list = (NBTTagList) compound.getTag(key);
        for (int i = 0; i < list.tagCount() && target.size() < MAX_EXPRESSIONS; i++) {
            String value = new RoutingExpression(sanitize(list.getStringTagAt(i), 64)).value();
            if (!value.isEmpty() && !target.contains(value)) target.add(value);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        flushOutputChanges();
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

    private void ensureOutputSlots() {
        flushOutputChanges();
        for (ItemKey key : storage.keys()) {
            boolean found = false;
            for (OutputSlot slot : outputSlots) if (slot.key.equals(key)) { found = true; break; }
            if (!found) {
                OutputSlot slot = new OutputSlot(key);
                refreshOutput(slot);
                outputSlots.add(slot);
            }
        }
    }

    private void refreshOutput(OutputSlot slot) {
        ItemStack sample = slot.key.toStack(1);
        int limit = sample == null ? 0 : Math.min(64, sample.getMaxStackSize());
        slot.count = (int) Math.min(storage.get(slot.key), (long) Math.max(0, limit));
        slot.stack = slot.count == 0 ? null : slot.key.toStack(slot.count);
        slot.restorable = 0;
    }

    private void refreshOutput(ItemKey key) {
        for (OutputSlot slot : outputSlots) if (slot.key.equals(key)) refreshOutput(slot);
    }

    // Some pipes mutate the returned stack and call markDirty; others write a reduced
    // copy back with setInventorySlotContents. Apply only the visible stack's delta.
    private void flushOutputChanges() {
        boolean changed = false;
        for (OutputSlot slot : outputSlots) {
            int remaining = slot.stack == null ? 0 : Math.max(0, slot.stack.stackSize);
            if (remaining < slot.count) {
                long removed = storage.remove(slot.key, slot.count - remaining);
                if (removed > 0L) { revision++; changed = true; }
                slot.count = remaining;
                slot.restorable = 0;
            } else if (remaining > slot.count) {
                slot.stack.stackSize = slot.count;
            }
        }
        if (changed) super.markDirty();
    }

    @Override public int getSizeInventory() {
        ensureOutputSlots();
        return Math.max(1, outputSlots.size());
    }
    @Override public ItemStack getStackInSlot(int slot) {
        ensureOutputSlots();
        if (slot < 0 || slot >= outputSlots.size()) return null;
        OutputSlot output = outputSlots.get(slot);
        refreshOutput(output);
        return output.stack;
    }
    @Override public ItemStack decrStackSize(int slot, int count) {
        ItemStack visible = getStackInSlot(slot);
        if (visible == null || count <= 0) return null;
        OutputSlot output = outputSlots.get(slot);
        int removed = (int) storage.remove(output.key, Math.min(count, visible.stackSize));
        if (removed <= 0) return null;
        // Keep this slot assigned until the next enumeration, including when it is
        // drained. A hopper may need to restore it if the destination rejects items.
        output.count -= removed;
        output.stack = output.count == 0 ? null : output.key.toStack(output.count);
        output.restorable = removed;
        revision++;
        super.markDirty();
        return output.key.toStack(removed);
    }
    @Override public ItemStack getStackInSlotOnClosing(int slot) { return null; }
    @Override public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot < 0 || slot >= outputSlots.size()) return;
        OutputSlot output = outputSlots.get(slot);
        if (stack != null && !output.key.equals(ItemKey.fromStack(stack))) return;
        int remaining = stack == null ? 0 : Math.max(0, stack.stackSize);
        int delta = remaining - output.count;
        if (delta < 0) {
            storage.remove(output.key, -(long) delta);
        } else if (delta > 0) {
            // Output-only: accept increases solely to undo a preceding decrStackSize.
            if (delta > output.restorable || !storage.add(output.key, delta)) return;
        }
        if (delta != 0) revision++;
        output.count = remaining;
        output.stack = remaining == 0 ? null : output.key.toStack(remaining);
        output.restorable = 0;
        super.markDirty();
    }
    @Override public String getInventoryName() { return customName.isEmpty() ? "container.x_tracer" : customName; }
    @Override public boolean hasCustomInventoryName() { return !customName.isEmpty(); }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public void markDirty() { flushOutputChanges(); super.markDirty(); }
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
    @Override public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return slot >= 0 && slot < outputSlots.size() && stack != null
            && outputSlots.get(slot).key.equals(ItemKey.fromStack(stack));
    }
}
