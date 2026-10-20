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
        dispatcher.register(Commands.literal("jglv2tablet").executes(context -> {
            var player = context.getSource().getPlayerOrException(); var level = LootRuntime.level(player);
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("ui-server-results.json")); }
            catch (java.io.IOException error) { throw new RuntimeException(error); }
            var pos = player.blockPosition().offset(2, 0, -3);
            if (level.getBlockEntity(pos) instanceof LootEntity old) { old.outputs.clear(); old.upgrades.clearContent(); }
            level.setBlockAndUpdate(pos, ModContent.BLOCK.get().defaultBlockState());
            var machine = (LootEntity) level.getBlockEntity(pos);
            machine.upgrades.setItem(0, new ItemStack(findUpgrade(UpgradeItem.Category.SPEED, 5)));
            var menu = new LootMenu(88, player.getInventory(), machine);
            menu.setCarried(new ItemStack(findUpgrade(UpgradeItem.Category.PLAYER, 0)));
            menu.handleClick(40, 0, "PICKUP", player);
            VerificationChecks.check(machine.playerContext(level) == player, "Online Player Upgrade resolves the connected real player");
            machine.setTable(VerificationV2Checks.PLAYER);
            for (int i = 0; i < 4; i++) { machine.energy = 16000; machine.serverTick(); }
            VerificationChecks.check(machine.count(0) == 1 && machine.template(0).is(Items.EMERALD), "Connected real player generates player-conditioned loot");
            machine.energy = 0;
            var entry = Tablets.library(player); entry.tables.clear();
            for (int i = 0; i < 40; i++) entry.capture("justgashloot:removed/table_" + String.format(java.util.Locale.ROOT, "%02d", i));
            entry.capture(VerificationV2Checks.BASIC); entry.capture(VerificationV2Checks.LUCK);
            VerificationV2Platform.installCaptureTarget(level, player.blockPosition().offset(1, 0, -3));
            LibraryData.get(level.getServer()).setDirty();
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModContent.ALL_ITEMS.stream().map(java.util.function.Supplier::get).filter(item -> item instanceof TabletItem).findFirst().orElseThrow()));
            Tablets.sync(player, false, "", 0);
            return 1;
        }));
        dispatcher.register(Commands.literal("jglv2assert").executes(context -> {
            var player = context.getSource().getPlayerOrException(); var level = LootRuntime.level(player);
            var pos = player.blockPosition().offset(2, 0, -3);
            boolean configured = level.getBlockEntity(pos) instanceof LootEntity machine && machine.tableId().equals(VerificationV2Checks.LUCK);
            boolean selected = Tablets.library(player).active().equals(VerificationV2Checks.LUCK);
            boolean sourcePreserved = VerificationV2Checks.BASIC.equals(LootRuntime.capture(level.getBlockEntity(player.blockPosition().offset(1, 0, -3))));
            boolean passed = configured && selected && sourcePreserved;
            try { java.nio.file.Files.writeString(java.nio.file.Path.of("ui-server-results.json"), "{\"passed\":" + passed + ",\"configuredByTablet\":" + configured + ",\"personalSelection\":" + selected + "}\n"); }
            catch (java.io.IOException error) { throw new RuntimeException(error); }
            System.out.println("JGL_UI_SERVER " + (passed ? "PASS" : "FAIL"));
            return passed ? 1 : 0;
        }));
    }
    private static net.minecraft.world.item.Item findUpgrade(UpgradeItem.Category category, int tier) {
        return ModContent.ALL_ITEMS.stream().map(java.util.function.Supplier::get).filter(item -> item instanceof UpgradeItem value && value.category == category && value.tier == tier).findFirst().orElseThrow();
    }
}
