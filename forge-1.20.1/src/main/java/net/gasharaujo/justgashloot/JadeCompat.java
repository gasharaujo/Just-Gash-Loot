package net.gasharaujo.justgashloot;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.view.*;

/** Optional Jade integration. Large amounts are transported separately from ItemStack counts. */
@WailaPlugin("justgashloot")
public final class JadeCompat implements IWailaPlugin {
    private static final net.minecraft.resources.ResourceLocation UID = new net.minecraft.resources.ResourceLocation("justgashloot", "bulk_items");
    @Override public void register(IWailaCommonRegistration registration) {
        registration.registerItemStorage(new Server(), LootEntity.class);
        registration.registerBlockDataProvider(new ExactCounts(), LootEntity.class);
    }
    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerItemStorageClient(new Client());
        registration.registerBlockComponent(new ExactCounts(), LootBlock.class);
    }
    public static List<ViewGroup<ItemStack>> groups(LootEntity machine) {
        List<ItemStack> icons = new ArrayList<>(); List<Long> counts = new ArrayList<>();
        for (int i = 0; i < 36; i++) if (machine.count(i) > 0) { icons.add(machine.template(i)); counts.add(machine.count(i)); }
        if (icons.isEmpty()) return List.of();
        ViewGroup<ItemStack> group = new ViewGroup<>(icons);
        group.getExtraData().putLongArray("Counts", counts.stream().mapToLong(Long::longValue).toArray());
        return List.of(group);
    }
    public static final class Server implements IServerExtensionProvider<LootEntity, ItemStack> {
        @Override public net.minecraft.resources.ResourceLocation getUid() { return UID; }
        @Override public List<ViewGroup<ItemStack>> getGroups(net.minecraft.server.level.ServerPlayer player, net.minecraft.server.level.ServerLevel level, LootEntity machine, boolean showDetails) { return groups(machine); }
    }
    public static final class Client implements IClientExtensionProvider<ItemStack, ItemView> {
        @Override public net.minecraft.resources.ResourceLocation getUid() { return UID; }
        // The exact-count provider renders rows. Generic icon overlays cannot fit 19-digit counts.
        @Override public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ItemStack>> groups) { return List.of(); }
    }
    public static final class ExactCounts implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
        @Override public net.minecraft.resources.ResourceLocation getUid() { return UID; }
        @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof LootEntity machine) {
                data.putString("JglTable", machine.tableId()); data.putString("JglOwner", machine.ownerName());
                StoredLoot.CODEC.listOf()
                    .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, machine.savedLoot()).result().ifPresent(tag -> data.put("JglExactLoot", tag));
            }
        }
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            String table = accessor.getServerData().getString("JglTable"), owner = accessor.getServerData().getString("JglOwner");
            if (!table.isEmpty()) tooltip.add(Component.translatable("tooltip.justgashloot.active", table));
            if (!owner.isEmpty()) tooltip.add(Component.translatable("gui.justgashloot.owner", owner));
            if (!accessor.getServerData().contains("JglExactLoot")) return;
            List<StoredLoot> entries = StoredLoot.CODEC.listOf().parse(net.minecraft.nbt.NbtOps.INSTANCE, accessor.getServerData().get("JglExactLoot")).result().orElse(List.of());
            int limit = accessor.showDetails() ? 36 : 4;
            for (int i = 0; i < entries.size() && i < limit; i++) {
                StoredLoot entry = entries.get(i);
                tooltip.add(Component.literal(java.text.NumberFormat.getIntegerInstance(Locale.ROOT).format(entry.count()) + " \u00d7 ").append(entry.item().getHoverName()));
            }
            if (entries.size() > limit) tooltip.add(Component.translatable("gui.justgashloot.more", entries.size() - limit));
        }
    }
}
