package net.gasharaujo.justgashloot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class LootEntity extends VersionedEntity implements MenuProvider {
    final BulkInventory<ItemStack> outputs = new BulkInventory<>(36, Platform::sameItem, stack -> stack.copyWithCount(1));
    public final SimpleContainer upgrades = new SimpleContainer(5) {
        @Override public boolean canPlaceItem(int index, ItemStack stack) { return canInstall(index, stack); }
        @Override public void setItem(int index, ItemStack stack) {
            if (stack.isEmpty() || canInstall(index, stack)) super.setItem(index, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        }
        @Override public void setChanged() { super.setChanged(); LootEntity.this.setChanged(); }
        @Override public int getMaxStackSize() { return 1; }
    };
    int lava, energy;
    long revision;
    private boolean contentsDropped;
    private UUID warnedPlayer;
    private long warningTick = Long.MIN_VALUE;

    public LootEntity(BlockPos pos, BlockState state) { super(pos, state); }
    @Override public void setChanged() { revision++; super.setChanged(); }
    public long count(int slot) { return outputs.count(slot); }
    public ItemStack template(int slot) {
        ItemStack type = outputs.type(slot);
        return type == null ? ItemStack.EMPTY : type.copyWithCount(1);
    }
    /** Server-only insertion point for the future loot generator. Returns leftover count. */
    public long addGeneratedLoot(ItemStack template, long amount) {
        if (level != null && Platform.clientSide(level)) throw new IllegalStateException("Server only");
        if (amount < 0) throw new IllegalArgumentException("Negative amount");
        if (template.isEmpty()) return amount;
        long remaining = outputs.insert(template, amount);
        if (remaining != amount) setChanged();
        return remaining;
    }
    public ItemStack extract(int slot, int requested, boolean simulate) {
        if (requested < 0) throw new IllegalArgumentException("Negative amount");
        ItemStack template = template(slot);
        if (template.isEmpty() || requested == 0) return ItemStack.EMPTY;
        int amount = (int) Math.min(count(slot), Math.min(requested, template.getMaxStackSize()));
        ItemStack result = template.copyWithCount(amount);
        if (!simulate && amount > 0) { outputs.extract(slot, amount); setChanged(); }
        return result;
    }
    public boolean canInstall(int slot, ItemStack stack) {
        if (slot < 0 || slot >= 5) return false;
        if (stack.isEmpty()) return true;
        if (!(stack.getItem() instanceof UpgradeItem upgrade)) return false;
        if ((slot == 4) != (upgrade.category == UpgradeItem.Category.PLAYER)) return false;
        for (int i = 0; i < 5; i++) {
            if (i != slot && upgrades.getItem(i).getItem() instanceof UpgradeItem other && other.category == upgrade.category) return false;
        }
        return true;
    }
    public int dropStacks(boolean includeBlock) {
        int piles = includeBlock ? 1 : 0;
        for (int i = 0; i < 5; i++) if (!upgrades.getItem(i).isEmpty()) piles = LootRules.addDropStacks(piles, 1, 1);
        for (int i = 0; i < 36 && piles <= 300; i++) {
            ItemStack type = outputs.type(i);
            if (type != null) piles = LootRules.addDropStacks(piles, count(i), type.getMaxStackSize());
        }
        return piles;
    }
    public boolean guarded() { return dropStacks(true) > LootRules.SAFE_DROP_STACKS; }
    public void warn(Player player) {
        if (level == null || Platform.clientSide(level)) return;
        long now = level.getGameTime();
        if (!player.getUUID().equals(warnedPlayer) || now - warningTick >= 40 || warningTick == Long.MIN_VALUE) {
            if (player instanceof net.minecraft.server.level.ServerPlayer server) server.sendSystemMessage(Component.translatable("message.justgashloot.large_drop"));
            warnedPlayer = player.getUUID(); warningTick = now;
        }
    }
    public void dropContents() {
        if (contentsDropped || level == null || Platform.clientSide(level)) return;
        contentsDropped = true;
        for (int i = 0; i < 36; i++) {
            while (count(i) > 0) Block.popResource(level, worldPosition, extract(i, Integer.MAX_VALUE, false));
        }
        for (int i = 0; i < 5; i++) {
            ItemStack stack = upgrades.removeItemNoUpdate(i);
            if (!stack.isEmpty()) Block.popResource(level, worldPosition, stack);
        }
        lava = energy = 0;
    }
    List<StoredLoot> savedLoot() {
        List<StoredLoot> saved = new ArrayList<>();
        for (int i = 0; i < 36; i++) if (count(i) > 0) saved.add(new StoredLoot(i, template(i), count(i)));
        return saved;
    }
    void loadLoot(List<StoredLoot> saved) {
        outputs.clear(); contentsDropped = false;
        for (StoredLoot entry : saved) if (!entry.item().isEmpty()) outputs.restore(entry.slot(), entry.item(), entry.count());
    }
    List<StoredLoot> savedUpgrades() {
        List<StoredLoot> saved = new ArrayList<>();
        for (int i = 0; i < 5; i++) if (!upgrades.getItem(i).isEmpty()) saved.add(new StoredLoot(i, upgrades.getItem(i).copyWithCount(1), 1));
        return saved;
    }
    void loadUpgrades(List<StoredLoot> saved) {
        upgrades.clearContent();
        for (StoredLoot entry : saved) if (entry.slot() < 5 && !entry.item().isEmpty()) upgrades.setItem(entry.slot(), entry.item());
        sanitizeUpgrades();
    }
    void sanitizeUpgrades() {
        for (int i = 0; i < 5; i++) {
            ItemStack stack = upgrades.getItem(i);
            if (!stack.isEmpty() && !canInstall(i, stack)) upgrades.removeItemNoUpdate(i);
            else if (!stack.isEmpty()) stack.setCount(1);
        }
    }
    @Override public Component getDisplayName() { return Component.translatable("block.justgashloot.loot_generator"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new LootMenu(id, inventory, this); }
}
