package net.gasharaujo.justgashloot;
import java.util.Objects;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.transfer.*;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.*;

final class PlatformCapabilities {
    private final LootEntity machine;
    PlatformCapabilities(LootEntity machine) { this.machine = machine; }
    private record State(BulkInventory.Snapshot outputs, int lava, int energy) {}
    private final SnapshotJournal<State> journal = new SnapshotJournal<>() {
        @Override protected State createSnapshot() { return new State(machine.outputs.snapshot(), machine.lava, machine.energy); }
        @Override protected void revertToSnapshot(State state) { machine.outputs.restore(state.outputs); machine.lava = state.lava; machine.energy = state.energy; }
        @Override protected void onRootCommit(State state) { if (!machine.isRemoved()) machine.setChanged(); }
    };
    final ResourceHandler<ItemResource> items = new ResourceHandler<>() {
        public int size() { return 36; }
        public ItemResource getResource(int index) { return ItemResource.of(machine.template(index)); }
        public long getAmountAsLong(int index) { return machine.count(index); }
        public long getCapacityAsLong(int index, ItemResource resource) { Objects.checkIndex(index, 36); return 0; }
        public boolean isValid(int index, ItemResource resource) { Objects.checkIndex(index, 36); return false; }
        public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
            Objects.checkIndex(index, 36); TransferPreconditions.checkNonEmptyNonNegative(resource, amount); return 0;
        }
        public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
            Objects.checkIndex(index, 36); TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (machine.isRemoved() || !resource.equals(getResource(index))) return 0;
            int taken = (int) Math.min(amount, machine.count(index));
            if (taken > 0) { journal.updateSnapshots(tx); machine.outputs.extract(index, taken); }
            return taken;
        }
    };
    private final FluidResource LAVA = FluidResource.of(Fluids.LAVA);
    final ResourceHandler<FluidResource> fluids = new ResourceHandler<>() {
        public int size() { return 1; }
        public FluidResource getResource(int index) { Objects.checkIndex(index, 1); return machine.lava == 0 ? FluidResource.EMPTY : LAVA; }
        public long getAmountAsLong(int index) { Objects.checkIndex(index, 1); return machine.lava; }
        public long getCapacityAsLong(int index, FluidResource resource) { Objects.checkIndex(index, 1); return resource.isEmpty() || resource.equals(LAVA) ? 16000 : 0; }
        public boolean isValid(int index, FluidResource resource) { Objects.checkIndex(index, 1); return resource.equals(LAVA); }
        public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
            Objects.checkIndex(index, 1); TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (machine.isRemoved() || !isValid(index, resource)) return 0;
            int taken = Math.min(amount, 16000 - machine.lava);
            if (taken > 0) { journal.updateSnapshots(tx); machine.lava += taken; }
            return taken;
        }
        public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
            Objects.checkIndex(index, 1); TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (machine.isRemoved() || !resource.equals(LAVA)) return 0;
            int taken = Math.min(amount, machine.lava);
            if (taken > 0) { journal.updateSnapshots(tx); machine.lava -= taken; }
            return taken;
        }
    };
    final EnergyHandler energy = new EnergyHandler() {
        public long getAmountAsLong() { return machine.energy; }
        public long getCapacityAsLong() { return 16000; }
        public int insert(int amount, TransactionContext tx) {
            TransferPreconditions.checkNonNegative(amount);
            if (machine.isRemoved()) return 0;
            int taken = Math.min(amount, 16000 - machine.energy);
            if (taken > 0) { journal.updateSnapshots(tx); machine.energy += taken; }
            return taken;
        }
        public int extract(int amount, TransactionContext tx) { TransferPreconditions.checkNonNegative(amount); return 0; }
    };
    static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModContent.ENTITY.get(), (machine, side) -> machine.capabilities.items);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModContent.ENTITY.get(), (machine, side) -> machine.capabilities.fluids);
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModContent.ENTITY.get(), (machine, side) -> machine.capabilities.energy);
    }
}
