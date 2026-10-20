package net.gasharaujo.justgashloot;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

final class VerificationV2Checks {
    static final String BASIC = "justgashloot:verification/basic", LUCK = "justgashloot:verification/luck", PLAYER = "justgashloot:verification/player";
    private static final BlockPos POS = new BlockPos(8, 80, 0);
    private static void check(boolean condition, String message) { VerificationChecks.check(condition, message); }
    private static Item upgrade(UpgradeItem.Category category, int tier) {
        return ModContent.ALL_ITEMS.stream().map(java.util.function.Supplier::get).filter(value -> value instanceof UpgradeItem u && u.category == category && u.tier == tier).findFirst().orElseThrow();
    }
    private static LootEntity fresh(ServerLevel level, String table) {
        if (level.getBlockEntity(POS) instanceof LootEntity old) { old.outputs.clear(); old.upgrades.clearContent(); }
        level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(POS, ModContent.BLOCK.get().defaultBlockState());
        LootEntity machine = (LootEntity) level.getBlockEntity(POS); machine.setTable(table);
        machine.upgrades.setItem(0, new ItemStack(upgrade(UpgradeItem.Category.SPEED, 5)));
        return machine;
    }
    private static long total(LootEntity machine, Item item) {
        long count = 0; for (int i = 0; i < 36; i++) if (machine.template(i).is(item)) count += machine.count(i); return count;
    }
    private static int cycle(LootEntity machine) {
        int spent = 0;
        for (int i = 0; i < 4; i++) { machine.energy = 16000; int before = machine.energy; machine.serverTick(); spent += before - machine.energy; }
        return spent;
    }
    static void run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        check(LootRuntime.resolve(level, BASIC) != null, "Verification datapack loot table is loaded");
        LootEntity machine = fresh(level, BASIC);
        machine.addGeneratedLoot(new ItemStack(Items.IRON_INGOT), 2_147_483_649L); machine.energy = 12345; machine.lava = 9876;
        LootEntity migrated = VerificationV2Platform.oldMachine(machine); migrated.setLevel(level);
        migrated.serverTick();
        check(migrated.tableId().isEmpty() && migrated.memory.cycle.work == 0 && migrated.energy == 12345 && migrated.lava == 9876
                && total(migrated, Items.IRON_INGOT) == 2_147_483_649L, "Pre-2.0 block schema migrates without losing stock, resources or starting unsolicited generation");
        machine = fresh(level, BASIC); machine.setTable("justgashloot:removed/table"); machine.energy = machine.lava = 16000;
        for (int i = 0; i < 20; i++) machine.serverTick();
        check(machine.energy == 16000 && machine.lava == 16000 && machine.memory.cycle.work == 0, "Unavailable datapack table pauses without consuming resources");
        machine.setTable(BASIC);
        check(cycle(machine) == 24000 && total(machine, Items.DIAMOND) == 2, "Real table generates after four Speed T5 ticks and charges 24000 FE");
        int[] quantities = {2, 3, 5, 6, 8};
        for (int i = 1; i <= 5; i++) {
            machine = fresh(level, BASIC); machine.upgrades.setItem(1, new ItemStack(upgrade(UpgradeItem.Category.QUANTITY, i)));
            check(cycle(machine) == 24000 && total(machine, Items.DIAMOND) == quantities[i - 1] * 2, "Quantity T" + i + " adds independent executions without multiplying fuel cost");
        }
        int[] luck = {1, 5, 15, 50, 100};
        for (int i = 1; i <= 5; i++) {
            machine = fresh(level, LUCK); machine.upgrades.setItem(1, new ItemStack(upgrade(UpgradeItem.Category.QUALITY, i)));
            cycle(machine);
            check(total(machine, Items.GOLD_INGOT) == 1 + luck[i - 1], "Quality T" + i + " changes luck-sensitive loot bonus rolls");
        }
        machine = fresh(level, BASIC); machine.upgrades.setItem(1, new ItemStack(upgrade(UpgradeItem.Category.ENERGY, 5)));
        machine.energy = 6000;
        for (int i = 0; i < 32; i++) machine.serverTick();
        check(machine.energy == 0 && total(machine, Items.DIAMOND) == 16, "Real block carries paid Energy T5 credit across eight fast cycles");
        machine = fresh(level, BASIC); machine.energy = 6000; machine.serverTick();
        LootEntity restored = VerificationMod.roundTrip(machine);
        check(restored.tableId().equals(BASIC) && restored.memory.cycle.work == 600, "Native block save preserves selected table and partial generation progress");
        restored.setLevel(level); restored.energy = 16000;
        for (int i = 0; i < 3; i++) { restored.energy = 16000; restored.serverTick(); }
        check(total(restored, Items.DIAMOND) == 2, "Reloaded partial cycle completes exactly once");
        machine = fresh(level, BASIC); machine.upgrades.setItem(0, new ItemStack(upgrade(UpgradeItem.Category.SPEED, 4)));
        machine.lava = 100;
        machine.serverTick(); machine.serverTick();
        restored = VerificationMod.roundTrip(machine); restored.setLevel(level); restored.serverTick();
        check(restored.lava == 60 && restored.memory.cycle.lavaThirds == 0, "Fractional lava accounting survives native serialization");
        machine = fresh(level, PLAYER); cycle(machine);
        check(total(machine, Items.EMERALD) == 0, "Player-conditioned loot is absent without a Player Upgrade");
        ServerPlayer installer = VerificationMod.player(level, "JglInstaller"); installer.setPos(8.5, 80.5, .5);
        installer.getInventory().clearContent();
        machine = fresh(level, PLAYER);
        LootMenu menu = new LootMenu(77, installer.getInventory(), machine); installer.containerMenu = menu;
        installer.getInventory().setItem(9, new ItemStack(upgrade(UpgradeItem.Category.PLAYER, 0)));
        menu.quickMoveStack(installer, 41);
        check(installer.getUUID().equals(machine.memory.owner), "Shift installation binds the Player Upgrade to its installer");
        cycle(machine);
        check(total(machine, Items.EMERALD) == 1, "Offline identity-only simulated player satisfies real loot conditions");
        menu.quickMoveStack(installer, 40);
        check(machine.memory.owner == null && machine.memory.cycle.work == 0, "Removing Player Upgrade clears identity and resets progress");
        menu.setCarried(new ItemStack(upgrade(UpgradeItem.Category.PLAYER, 0))); menu.handleClick(40, 0, "PICKUP", installer);
        check(installer.getUUID().equals(machine.memory.owner), "Normal click installation records installer identity");
        menu.handleClick(40, 0, "PICKUP", installer);
        installer.getInventory().setItem(0, new ItemStack(upgrade(UpgradeItem.Category.PLAYER, 0))); menu.handleClick(40, 0, "SWAP", installer);
        check(installer.getUUID().equals(machine.memory.owner), "Number key installation records installer identity");
        restored = VerificationMod.roundTrip(machine);
        check(installer.getUUID().equals(restored.memory.owner) && restored.ownerName().equals("JglInstaller"), "Player identity persists with the machine");
        machine = fresh(level, PLAYER); menu = new LootMenu(78, installer.getInventory(), machine); installer.containerMenu = menu;
        menu.setCarried(new ItemStack(upgrade(UpgradeItem.Category.PLAYER, 0)));
        menu.handleClick(-999, 0, "QUICK_CRAFT", installer); menu.handleClick(40, 1, "QUICK_CRAFT", installer); menu.handleClick(-999, 2, "QUICK_CRAFT", installer);
        check(installer.getUUID().equals(machine.memory.owner), "Drag installation binds the Player Upgrade to its installer");
        machine = fresh(level, BASIC);
        for (int i = 0; i < 36; i++) { ItemStack stack = new ItemStack(Items.STONE); VerificationMod.name(stack, "Full " + i); machine.addGeneratedLoot(stack, Long.MAX_VALUE); }
        cycle(machine);
        check(total(machine, Items.DIAMOND) == 0 && machine.count(0) == Long.MAX_VALUE, "Full storage discards generated excess without overflow or overwriting existing items");
        machine.outputs.clear(); machine.upgrades.clearContent(); level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
        var book = LibraryData.get(server).book;
        UUID person = new UUID(2000, 2), other = new UUID(2000, 3);
        if (Boolean.getBoolean("justgashloot.expectLibrary")) check(book.player(person).active().equals(BASIC), "Personal collection was saved to disk and restored by another dedicated-server run");
        book.player(person).capture(BASIC); book.player(person).capture(BASIC); book.player(person).capture(LUCK);
        check(book.player(person).tables().size() == 2 && book.player(person).active().equals(LUCK), "Capture deduplicates personal tables and activates the latest capture");
        check(!book.player(other).select(BASIC) && book.player(other).tables().isEmpty(), "Personal collection cannot select another player's table");
        book.player(person).select(BASIC); LibraryData.get(server).setDirty();
        var decoded = LibraryBook.decode(book.encode());
        check(decoded.player(person).active().equals(BASIC) && decoded.player(person).tables().size() == 2, "Collection and active selection round trip without item-specific state");
        var large = new LibraryBook();
        for (int i = 0; i < 3500; i++) large.player(person).capture("minecraft:verification/large_collection_table_" + i);
        var largeTag = LibraryBook.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, large).result().orElseThrow();
        try {
            var bytes = new java.io.ByteArrayOutputStream();
            net.minecraft.nbt.NbtIo.write((net.minecraft.nbt.CompoundTag) largeTag, new java.io.DataOutputStream(bytes));
            var largeRestored = LibraryBook.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, largeTag).result().orElseThrow();
            check(bytes.size() > 65535 && largeRestored.player(person).tables().size() == 3500
                    && largeRestored.player(person).active().equals(large.player(person).active()), "Structured personal collection persists 3500 tables beyond the NBT string limit");
        } catch (java.io.IOException error) { throw new RuntimeException(error); }
        Tablets.library(installer).capture(BASIC);
        installer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModContent.ALL_ITEMS.stream().map(java.util.function.Supplier::get).filter(item -> item instanceof TabletItem).findFirst().orElseThrow()));
        Tablets.action(installer, new LibraryAction(1, "minecraft:chests/nether_bridge", 0));
        check(Tablets.library(installer).active().equals(BASIC), "Client selection cannot inject an uncaptured table into the personal library");
        VerificationV2Platform.recipes(level, installer);
        VerificationV2Platform.capture(level);
        installer.containerMenu = installer.inventoryMenu;
    }
}
