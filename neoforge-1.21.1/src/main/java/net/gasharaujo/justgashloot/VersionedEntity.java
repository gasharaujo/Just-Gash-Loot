package net.gasharaujo.justgashloot;
abstract class VersionedEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    final PlatformCapabilities capabilities = new PlatformCapabilities((LootEntity) this);
    VersionedEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { super(ModContent.ENTITY.get(), pos, state); }
    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider); LootEntity self = (LootEntity) this;
        StoredLoot.CODEC.listOf().encodeStart(provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), self.savedLoot()).result().ifPresent(data -> tag.put("Loot", data));
        StoredLoot.CODEC.listOf().encodeStart(provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), self.savedUpgrades()).result().ifPresent(data -> tag.put("Upgrades", data));
        tag.putInt("Lava", self.lava); tag.putInt("Energy", self.energy);
    }
    @Override protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider); LootEntity self = (LootEntity) this;
        self.loadLoot(tag.contains("Loot") ? StoredLoot.CODEC.listOf().parse(provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag.get("Loot")).result().orElse(java.util.List.of()) : java.util.List.of());
        self.loadUpgrades(tag.contains("Upgrades") ? StoredLoot.CODEC.listOf().parse(provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag.get("Upgrades")).result().orElse(java.util.List.of()) : java.util.List.of());
        self.lava = Math.max(0, Math.min(16000, tag.getInt("Lava")));
        self.energy = Math.max(0, Math.min(16000, tag.getInt("Energy")));
    }
}
