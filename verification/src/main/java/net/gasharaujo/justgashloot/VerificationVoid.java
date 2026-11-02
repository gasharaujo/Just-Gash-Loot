package net.gasharaujo.justgashloot;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
final class VerificationVoid {
    static void run(ServerLevel level) {
        var pos = new BlockPos(30, 80, 0);
        level.setBlockAndUpdate(pos, ModContent.BLOCK.get().defaultBlockState());
        var machine = (LootEntity) level.getBlockEntity(pos);
        machine.addGeneratedLoot(new ItemStack(Items.DIAMOND), 10);
        machine.setVoidRules(List.of("minecraft:diamond"));
        VerificationChecks.check(machine.addGeneratedLoot(new ItemStack(Items.DIAMOND), Long.MAX_VALUE) == 0 && machine.count(0) == 10, "Void item filter discards new output without altering stored items or overflowing");
        machine.setVoidRules(List.of("minecraft:diamond", "#minecraft:planks"));
        VerificationChecks.check(machine.addGeneratedLoot(new ItemStack(Items.OAK_PLANKS), 64) == 0 && machine.addGeneratedLoot(new ItemStack(Items.STONE), 1) == 0 && machine.template(1).is(Items.STONE), "Void tag filter discards matching items and retains unrelated output");
        var copy = VerificationMod.roundTrip(machine);
        VerificationChecks.check(copy.voidRules().equals(machine.voidRules()) && copy.count(0) == 10, "Void rules persist with native block data while preserving existing stock");
        VerificationChecks.check(MachineMemory.decode("{\"table\":\"\",\"cycle\":{}}").voidRules.isEmpty(), "Older machines migrate to an empty Void Filter");
        VerificationChecks.check(FilterPlatform.suggestions("minecraft:diamond").contains("minecraft:diamond") && FilterPlatform.suggestions("#minecraft:planks").contains("#minecraft:planks") && FilterPlatform.suggestions("#planks").contains("#minecraft:planks"), "Server autocomplete provides real item IDs and item tags");
        VerificationChecks.check(!FilterPlatform.exists("minecraft:not_a_real_jgl_item") && !FilterPlatform.exists("#minecraft:not_a_real_jgl_tag"), "Void configuration refuses unknown items and tags");
        var installer = VerificationMod.player(level, "JglVoid"); installer.setPos(30.5, 80.5, .5);
        var menu = new LootMenu(79, installer.getInventory(), machine); installer.containerMenu = menu;
        FilterActions.handle(installer, new FilterAction(80, 3, "minecraft:diamond", ""));
        installer.setPos(100, 80, 0);
        FilterActions.handle(installer, new FilterAction(79, 3, "minecraft:diamond", ""));
        VerificationChecks.check(machine.voidRules().size() == 2, "Void packets cannot edit a different menu or an out-of-range machine");
        installer.containerMenu = installer.inventoryMenu;
        machine.setTable(VerificationV2Checks.BASIC);
        var speed = ModContent.ALL_ITEMS.stream().map(java.util.function.Supplier::get).filter(item -> item instanceof UpgradeItem u && u.category == UpgradeItem.Category.SPEED && u.tier == 5).findFirst().orElseThrow();
        machine.upgrades.setItem(0, new ItemStack(speed));
        for (int i = 0; i < 4; i++) { machine.energy = 16000; machine.serverTick(); }
        VerificationChecks.check(machine.count(0) == 10 && machine.memory.cycle.work == 0, "Real loot execution applies Void Filter before storing generated rewards");
        machine.outputs.clear(); machine.upgrades.clearContent(); level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }
    static int assertClient(net.minecraft.server.level.ServerPlayer player) {
        boolean passed = player.containerMenu instanceof LootMenu menu && menu.machine != null
                && menu.machine.voidRules().equals(List.of("minecraft:diamond", "#minecraft:planks"))
                && VerificationMod.roundTrip(menu.machine).voidRules().equals(menu.machine.voidRules());
        try { java.nio.file.Files.writeString(java.nio.file.Path.of("void-server-results.json"), "{\"passed\":" + passed + "}\n"); }
        catch (java.io.IOException error) { throw new RuntimeException(error); }
        return passed ? 1 : 0;
    }
}
