package net.gasharaujo.justgashloot;
abstract class VersionedEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    final PlatformCapabilities capabilities = new PlatformCapabilities((LootEntity) this);
    private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> itemCap = net.minecraftforge.common.util.LazyOptional.of(() -> capabilities.items);
    private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> fluidCap = net.minecraftforge.common.util.LazyOptional.of(() -> capabilities.fluids);
    private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.energy.IEnergyStorage> energyCap = net.minecraftforge.common.util.LazyOptional.of(() -> capabilities.energy);
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, net.minecraft.core.Direction side) {
        if (!isRemoved()) {
            if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
            if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
            if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY) return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCap.invalidate(); fluidCap.invalidate(); energyCap.invalidate(); }

    VersionedEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { super(ModContent.ENTITY.get(), pos, state); }
    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag); LootEntity self = (LootEntity) this;
        StoredLoot.CODEC.listOf().encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, self.savedLoot()).result().ifPresent(data -> tag.put("Loot", data));
        StoredLoot.CODEC.listOf().encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, self.savedUpgrades()).result().ifPresent(data -> tag.put("Upgrades", data));
        tag.putString("Generation", self.memory.encode());
        tag.putInt("Lava", self.lava); tag.putInt("Energy", self.energy);
    }
    @Override public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag); LootEntity self = (LootEntity) this;
        self.loadLoot(tag.contains("Loot") ? StoredLoot.CODEC.listOf().parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("Loot")).result().orElse(java.util.List.of()) : java.util.List.of());
        self.loadUpgrades(tag.contains("Upgrades") ? StoredLoot.CODEC.listOf().parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("Upgrades")).result().orElse(java.util.List.of()) : java.util.List.of());
        self.memory = MachineMemory.decode(tag.getString("Generation"));
        self.lava = Math.max(0, Math.min(16000, tag.getInt("Lava")));
        self.energy = Math.max(0, Math.min(16000, tag.getInt("Energy")));
    }
}
