package net.gasharaujo.justgashloot;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class LootMenu extends VersionedMenu {
    public static final int UPGRADES = 36, PLAYER = 41, HOTBAR = 68, END = 77;
    public static final int WIDTH = 176, HEIGHT = 222;
    public final LootEntity machine;
    public final long[] counts = new long[36];
    private final Inventory inventory;
    private long lastRevision = Long.MIN_VALUE;

    public LootMenu(int id, Inventory inventory) { this(id, inventory, null); }
    public LootMenu(int id, Inventory inventory, LootEntity machine) {
        super(ModContent.MENU.get(), id);
        this.machine = machine; this.inventory = inventory;
        SimpleContainer view = new SimpleContainer(36) {
            @Override public ItemStack getItem(int slot) { return machine == null ? super.getItem(slot) : machine.template(slot); }
            @Override public ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }
            @Override public void setItem(int slot, ItemStack stack) { if (machine == null) super.setItem(slot, stack); }
        };
        for (int i = 0; i < 36; i++) addSlot(new Slot(view, i, 8 + i % 9 * 18, 18 + i / 9 * 18) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        SimpleContainer upgrades = machine == null ? new SimpleContainer(5) : machine.upgrades;
        int[] xs = {39, 57, 75, 93, 119};
        for (int i = 0; i < 5; i++) {
            final int upgradeIndex = i;
            addSlot(new Slot(upgrades, i, xs[i], 104) {
                @Override public boolean mayPlace(ItemStack stack) {
                    if (!(stack.getItem() instanceof UpgradeItem upgrade)) return false;
                    if ((upgradeIndex == 4) != (upgrade.category == UpgradeItem.Category.PLAYER)) return false;
                    if (machine != null) return machine.canInstall(upgradeIndex, stack);
                    for (int j = 0; j < 5; j++) if (j != upgradeIndex && upgrades.getItem(j).getItem() instanceof UpgradeItem other && other.category == upgrade.category) return false;
                    return true;
                }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 140 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 198));
    }
    @Override public boolean stillValid(Player player) {
        return machine == null || !machine.isRemoved() && player.level() == machine.getLevel()
                && player.distanceToSqr(machine.getBlockPos().getX() + .5, machine.getBlockPos().getY() + .5, machine.getBlockPos().getZ() + .5) <= 64;
    }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (machine != null && inventory.player instanceof ServerPlayer player && lastRevision != machine.outputs.revision()) {
            for (int i = 0; i < 36; i++) counts[i] = machine.count(i);
            UiNetwork.send(player, new CountState(containerId, counts.clone()));
            lastRevision = machine.outputs.revision();
        }
    }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.index >= UPGRADES && super.canTakeItemForPickAll(stack, slot); }
    @Override public void handleClick(int slotId, int button, String type, Player player) {
        long before = machine == null ? 0 : machine.playerSlotRevision;
        try { handleClickInternal(slotId, button, type, player); }
        finally { if (machine != null) machine.bindInstaller(player, before); }
    }
    private void handleClickInternal(int slotId, int button, String type, Player player) {
        if (slotId < 0 || slotId >= 36) { vanillaClicked(slotId, button, type, player); return; }
        // Output icons are views only. Never let vanilla mutate the large count through a Slot.
        if (machine == null || !stillValid(player) || player.isSpectator()) return;
        if (type.equals("QUICK_MOVE")) { quickMoveStack(player, slotId); return; }
        ItemStack template = machine.template(slotId);
        if (template.isEmpty()) return;
        if (type.equals("PICKUP") && (button == 0 || button == 1)) {
            ItemStack carried = getCarried();
            if (!carried.isEmpty() && !Platform.sameItem(template, carried)) return;
            int room = template.getMaxStackSize() - carried.getCount();
            int requested = button == 1 ? Math.min(1, room) : room;
            ItemStack taken = machine.extract(slotId, Math.max(0, requested), false);
            if (carried.isEmpty()) setCarried(taken); else carried.grow(taken.getCount());
        } else if (type.equals("SWAP") && (button >= 0 && button < 9 || button == 40)) {
            ItemStack target = inventory.getItem(button);
            if (!target.isEmpty() && !Platform.sameItem(template, target)) return;
            int room = template.getMaxStackSize() - target.getCount();
            ItemStack taken = machine.extract(slotId, Math.max(0, room), false);
            if (target.isEmpty()) inventory.setItem(button, taken); else target.grow(taken.getCount());
            inventory.setChanged();
        } else if (type.equals("THROW") && getCarried().isEmpty()) {
            player.drop(machine.extract(slotId, button == 0 ? 1 : template.getMaxStackSize(), false), true);
        } else if (type.equals("CLONE") && player.getAbilities().instabuild && getCarried().isEmpty()) {
            setCarried(template.copyWithCount(template.getMaxStackSize()));
        }
        broadcastChanges();
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        long before = machine == null ? 0 : machine.playerSlotRevision;
        try { return quickMoveInternal(player, index); }
        finally { if (machine != null) machine.bindInstaller(player, before); }
    }
    private ItemStack quickMoveInternal(Player player, int index) {
        if (index < 0 || index >= END || !stillValid(player)) return ItemStack.EMPTY;
        if (index < 36) {
            if (machine == null) return ItemStack.EMPTY;
            ItemStack result = ItemStack.EMPTY;
            // At most one pass per player slot plus a partial stack: bounded even at Long.MAX_VALUE.
            for (int pass = 0; pass < 37; pass++) {
                ItemStack offered = machine.extract(index, Integer.MAX_VALUE, true);
                if (offered.isEmpty()) break;
                int before = offered.getCount();
                if (!moveItemStackTo(offered, PLAYER, END, true)) break;
                int moved = before - offered.getCount();
                if (moved <= 0) break;
                result = machine.extract(index, moved, false);
            }
            return result;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        boolean moved;
        if (index < PLAYER) moved = moveItemStackTo(stack, PLAYER, END, true);
        else if (stack.getItem() instanceof UpgradeItem upgrade) {
            moved = upgrade.category == UpgradeItem.Category.PLAYER ? moveItemStackTo(stack, 40, 41, false) : moveItemStackTo(stack, 36, 40, false);
        } else moved = index < HOTBAR ? moveItemStackTo(stack, HOTBAR, END, false) : moveItemStackTo(stack, PLAYER, HOTBAR, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
