package net.gasharaujo.justgashloot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.energy.IEnergyStorage;

final class PlatformCapabilities {
    private final LootEntity machine;
    PlatformCapabilities(LootEntity machine) { this.machine = machine; }
    final IItemHandler items = new IItemHandler() {
        public int getSlots() { return 36; }
        public ItemStack getStackInSlot(int slot) {
            ItemStack template = machine.template(slot);
            return template.isEmpty() ? ItemStack.EMPTY : template.copyWithCount((int) Math.min(Integer.MAX_VALUE, machine.count(slot)));
        }
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { java.util.Objects.checkIndex(slot, 36); return stack; }
        public ItemStack extractItem(int slot, int amount, boolean simulate) { return machine.extract(slot, amount, simulate); }
        public int getSlotLimit(int slot) { java.util.Objects.checkIndex(slot, 36); return Integer.MAX_VALUE; }
        public boolean isItemValid(int slot, ItemStack stack) { java.util.Objects.checkIndex(slot, 36); return false; }
    };
    final IFluidHandler fluids = new IFluidHandler() {
        public int getTanks() { return 1; }
        public FluidStack getFluidInTank(int tank) { java.util.Objects.checkIndex(tank, 1); return machine.lava == 0 ? FluidStack.EMPTY : new FluidStack(Fluids.LAVA, machine.lava); }
        public int getTankCapacity(int tank) { java.util.Objects.checkIndex(tank, 1); return 16000; }
        public boolean isFluidValid(int tank, FluidStack stack) { java.util.Objects.checkIndex(tank, 1); return stack.getFluid() == Fluids.LAVA; }
        public int fill(FluidStack stack, FluidAction action) {
            if (!isFluidValid(0, stack) || machine.isRemoved()) return 0;
            int accepted = Math.min(stack.getAmount(), 16000 - machine.lava);
            if (accepted > 0 && action.execute()) { machine.lava += accepted; machine.setChanged(); }
            return accepted;
        }
        public FluidStack drain(FluidStack stack, FluidAction action) { return stack.getFluid() == Fluids.LAVA ? drain(stack.getAmount(), action) : FluidStack.EMPTY; }
        public FluidStack drain(int amount, FluidAction action) {
            int taken = Math.min(Math.max(0, amount), machine.lava);
            if (taken == 0 || machine.isRemoved()) return FluidStack.EMPTY;
            if (action.execute()) { machine.lava -= taken; machine.setChanged(); }
            return new FluidStack(Fluids.LAVA, taken);
        }
    };
    final IEnergyStorage energy = new IEnergyStorage() {
        public int receiveEnergy(int amount, boolean simulate) {
            if (machine.isRemoved()) return 0;
            int accepted = Math.min(Math.max(0, amount), 16000 - machine.energy);
            if (accepted > 0 && !simulate) { machine.energy += accepted; machine.setChanged(); }
            return accepted;
        }
        public int extractEnergy(int amount, boolean simulate) { return 0; }
        public int getEnergyStored() { return machine.energy; }
        public int getMaxEnergyStored() { return 16000; }
        public boolean canExtract() { return false; }
        public boolean canReceive() { return true; }
    };

}
