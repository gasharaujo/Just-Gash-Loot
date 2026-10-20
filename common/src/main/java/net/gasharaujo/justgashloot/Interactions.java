package net.gasharaujo.justgashloot;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class Interactions {
    private Interactions() {}
    public static void use(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (Platform.clientSide(level) || !(level.getBlockEntity(pos) instanceof LootEntity machine)) return;
        ItemStack held = player.getItemInHand(hand);
        if (Tablets.isTablet(held) && player instanceof net.minecraft.server.level.ServerPlayer server) {
            if (player.isShiftKeyDown()) Tablets.sync(server, true, "", 0); else Tablets.configure(server, machine);
            return;
        }
        if (held.is(Items.LAVA_BUCKET)) {
            if (machine.lava <= LootRules.LAVA_CAPACITY - 1000) {
                machine.lava += 1000; machine.setChanged();
                exchange(player, hand, held, new ItemStack(Items.BUCKET));
            }
            return;
        }
        if (held.is(Items.BUCKET) && machine.lava >= 1000) {
            machine.lava -= 1000; machine.setChanged();
            exchange(player, hand, held, new ItemStack(Items.LAVA_BUCKET));
            return;
        }
        Platform.open(player, machine);
    }
    private static void exchange(Player player, InteractionHand hand, ItemStack held, ItemStack result) {
        if (player.getAbilities().instabuild) return;
        held.shrink(1);
        if (held.isEmpty()) player.setItemInHand(hand, result);
        else if (!player.getInventory().add(result)) player.drop(result, false);
    }
}
