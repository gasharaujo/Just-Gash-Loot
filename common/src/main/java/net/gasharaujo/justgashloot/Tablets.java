package net.gasharaujo.justgashloot;

import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class Tablets {
    private Tablets() {}
    public static boolean isTablet(ItemStack stack) { return stack.getItem() instanceof TabletItem; }
    public static boolean holding(Player player) { return isTablet(player.getMainHandItem()) || isTablet(player.getOffhandItem()); }
    public static LibraryBook.Entry library(ServerPlayer player) { return LibraryData.get(LootRuntime.level(player).getServer()).book.player(player.getUUID()); }
    public static void message(ServerPlayer player, String key, Object... args) { player.sendSystemMessage(Component.translatable("message.justgashloot." + key, args)); }
    public static void capture(ServerPlayer player, Object target) {
        String id = LootRuntime.capture(target);
        if (id == null || LootRuntime.resolve(LootRuntime.level(player), id) == null) { message(player, "no_table"); return; }
        library(player).capture(id); LibraryData.get(LootRuntime.level(player).getServer()).setDirty();
        sync(player, false, "", 0); message(player, "captured", id);
    }
    public static void configure(ServerPlayer player, LootEntity machine) {
        String id = library(player).active();
        if (id.isEmpty() || LootRuntime.resolve(LootRuntime.level(player), id) == null) { message(player, "no_active"); return; }
        machine.setTable(id); message(player, "configured", id);
        if (!machine.upgrades.getItem(4).isEmpty() && machine.memory.owner == null) message(player, "unbound_player");
    }
    public static void action(ServerPlayer player, LibraryAction action) {
        if (!holding(player) || player.isSpectator()) return;
        if (action.operation() == 1) {
            if (!LootRuntime.validId(action.value()) || !library(player).select(action.value())) return;
            LibraryData.get(LootRuntime.level(player).getServer()).setDirty(); sync(player, false, "", 0);
        } else if (action.operation() == 0 || action.operation() == 2) {
            if (action.value().length() > 128 || action.offset() < 0) return;
            sync(player, true, action.value(), action.offset());
        }
    }
    public static void sync(ServerPlayer player, boolean open, String query, int requestedOffset) {
        LibraryBook.Entry entry = library(player);
        String filter = query.toLowerCase(Locale.ROOT);
        List<String> matching = entry.tables().stream().filter(id -> id.toLowerCase(Locale.ROOT).contains(filter)).toList();
        int offset = Math.min(Math.max(0, requestedOffset), Math.max(0, (matching.size() - 1) / LibraryState.PAGE_SIZE * LibraryState.PAGE_SIZE));
        TabletNetwork.send(player, new LibraryState(open, entry.active(), query, offset, matching.size(),
                matching.subList(offset, Math.min(matching.size(), offset + LibraryState.PAGE_SIZE))));
    }
}
