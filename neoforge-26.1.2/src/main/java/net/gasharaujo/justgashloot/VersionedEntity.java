package net.gasharaujo.justgashloot;
abstract class VersionedEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    final PlatformCapabilities capabilities = new PlatformCapabilities((LootEntity) this);
    VersionedEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { super(ModContent.ENTITY.get(), pos, state); }
    @Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out); LootEntity self = (LootEntity) this;
        out.store("Loot", StoredLoot.CODEC.listOf(), self.savedLoot());
        out.store("Upgrades", StoredLoot.CODEC.listOf(), self.savedUpgrades());
        out.putInt("Lava", self.lava); out.putInt("Energy", self.energy);
    }
    @Override protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in); LootEntity self = (LootEntity) this;
        self.loadLoot(in.read("Loot", StoredLoot.CODEC.listOf()).orElse(java.util.List.of()));
        self.loadUpgrades(in.read("Upgrades", StoredLoot.CODEC.listOf()).orElse(java.util.List.of()));
        self.lava = Math.max(0, Math.min(16000, in.getIntOr("Lava", 0)));
        self.energy = Math.max(0, Math.min(16000, in.getIntOr("Energy", 0)));
    }
    @Override public void preRemoveSideEffects(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        ((LootEntity) this).dropContents(); super.preRemoveSideEffects(pos, state);
    }
}
