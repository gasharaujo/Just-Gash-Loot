package net.gasharaujo.justgashloot;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class VerificationScene {
    public static void commands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jgluitest").executes(context -> {
            var player = context.getSource().getPlayerOrException();
            var level = (net.minecraft.server.level.ServerLevel) player.level();
            level.getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
            player.setHealth(player.getMaxHealth());
            player.getInventory().clearContent();
            var pos = player.blockPosition().offset(0, 0, -3);
            if (level.getBlockEntity(pos) instanceof LootEntity previous) {
                previous.outputs.clear(); previous.upgrades.clearContent(); previous.lava = previous.energy = 0;
            }
            level.setBlockAndUpdate(pos, ModContent.BLOCK.get().defaultBlockState());
            var machine = (LootEntity) level.getBlockEntity(pos);
            machine.addGeneratedLoot(new ItemStack(Items.DIAMOND), 123456);
            machine.addGeneratedLoot(new ItemStack(Items.IRON_INGOT), 2_147_483_649L);
            machine.addGeneratedLoot(new ItemStack(Items.EMERALD), Long.MAX_VALUE);
            machine.addGeneratedLoot(new ItemStack(Items.DIAMOND_SWORD), 17);
            int index = 0;
            for (var category : UpgradeItem.Category.values()) {
                final int tier = category == UpgradeItem.Category.PLAYER ? 0 : index + 1;
                var item = ModContent.ALL_ITEMS.stream().map(java.util.function.Supplier::get)
                        .filter(value -> value instanceof UpgradeItem up && up.category == category && up.tier == tier).findFirst().orElseThrow();
                machine.upgrades.setItem(index++, new ItemStack(item));
            }
            machine.lava = 12000; machine.energy = 15000; machine.setChanged();
            Platform.open(player, machine);
            return 1;
        }));
    }
}
