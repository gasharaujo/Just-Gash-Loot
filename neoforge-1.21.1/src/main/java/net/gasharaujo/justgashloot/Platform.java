package net.gasharaujo.justgashloot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
public final class Platform {
    private Platform() {}
    public static boolean sameItem(ItemStack a, ItemStack b) { return ItemStack.isSameItemSameComponents(a, b); }
    public static boolean clientSide(Level level) { return level.isClientSide; }
    public static void open(Player player, LootEntity machine) { if (player instanceof ServerPlayer server) server.openMenu(machine, machine.getBlockPos()); }
}
