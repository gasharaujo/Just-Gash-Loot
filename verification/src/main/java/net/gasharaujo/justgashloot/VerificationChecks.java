package net.gasharaujo.justgashloot;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;

/** Opt-in, dedicated-server integration checks. Never packaged in release JARs. */
public final class VerificationChecks {
    private static final List<String> passed = new ArrayList<>();
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        passed.add(message);
        System.out.println("JGL_CHECK_PASS " + message);
    }
    public static void run(MinecraftServer server) {
        Throwable failure = null;
        try { VerificationV2Checks.run(server); checks(server); }
        catch (Throwable error) { failure = error; error.printStackTrace(); }
        try {
            StringBuilder report = new StringBuilder("{\n  \"passed\": " + (failure == null) + ",\n  \"checks\": [");
            for (int i = 0; i < passed.size(); i++) report.append(i == 0 ? "\n" : ",\n").append("    \"").append(passed.get(i)).append('"');
            report.append("\n  ],\n  \"failure\": \"").append(failure == null ? "" : failure.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")).append("\"\n}\n");
            java.nio.file.Files.writeString(java.nio.file.Path.of("verification-results.json"), report);
        } catch (java.io.IOException error) { throw new RuntimeException(error); }
        System.out.println("JGL_VERIFICATION " + (failure == null ? "PASS" : "FAIL") + " " + passed.size());
        server.halt(false);
    }
    private static Item upgrade(UpgradeItem.Category category, int tier) {
        return ModContent.ALL_ITEMS.stream().map(java.util.function.Supplier::get)
                .filter(item -> item instanceof UpgradeItem value && value.category == category && value.tier == tier).findFirst().orElseThrow();
    }
    private static void checks(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos pos = new BlockPos(0, 80, 0);
        level.setBlockAndUpdate(pos, ModContent.BLOCK.get().defaultBlockState());
        LootEntity machine = (LootEntity) level.getBlockEntity(pos);
        check(machine != null, "Registered block creates its block entity");
        check(ModContent.ALL_ITEMS.size() == 24, "Creative catalog contains block, 21 upgrades, base and tablet");
        check(machine.addGeneratedLoot(new ItemStack(Items.DIAMOND), 2_147_483_649L) == 0, "Server storage accepts counts above 32 bit");
        check(machine.extract(0, Integer.MAX_VALUE, true).getCount() == 64 && machine.count(0) == 2_147_483_649L, "Simulated extraction is capped to native stack size and does not mutate");
        machine.extract(0, 1, false);
        check(machine.count(0) == 2_147_483_648L, "Real extraction decrements the long counter exactly");
        ItemStack named = new ItemStack(Items.DIAMOND);
        VerificationMod.name(named, "Unique component");
        machine.addGeneratedLoot(named, 70);
        check(machine.count(1) == 70, "Different item data uses a different output slot");
        machine.lava = 13000; machine.energy = 12345;
        machine.upgrades.setItem(0, new ItemStack(upgrade(UpgradeItem.Category.SPEED, 2)));
        LootEntity restored = VerificationMod.roundTrip(machine);
        check(restored.count(0) == 2_147_483_648L && restored.count(1) == 70 && Platform.sameItem(restored.template(1), named), "Block entity round trip preserves long counts and item data");
        check(restored.lava == 13000 && restored.energy == 12345 && restored.upgrades.getItem(0).getItem() == upgrade(UpgradeItem.Category.SPEED, 2), "Block entity round trip preserves resources and upgrades");
        machine.outputs.clear(); machine.upgrades.clearContent(); machine.lava = machine.energy = 0;

        ServerPlayer player = VerificationMod.player(level, "JglTestA");
        player.setPos(.5, 80.5, .5); player.getInventory().clearContent();
        LootMenu menu = new LootMenu(1, player.getInventory(), machine); player.containerMenu = menu;
        check(menu.slots.size() == 77, "Menu has 36 outputs, 5 upgrades and 36 player slots");
        check(!menu.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND)), "Output slots reject manual insertion");
        check(!menu.getSlot(36).mayPlace(new ItemStack(Items.STONE)), "Upgrade slots reject unrelated items");
        for (int i = 0; i < 4; i++) check(menu.getSlot(36 + i).mayPlace(new ItemStack(upgrade(UpgradeItem.Category.QUALITY, 3))), "General upgrade slot " + i + " accepts upgrade categories");
        menu.getSlot(36).set(new ItemStack(upgrade(UpgradeItem.Category.QUALITY, 1)));
        check(!menu.getSlot(37).mayPlace(new ItemStack(upgrade(UpgradeItem.Category.QUALITY, 5))), "Different tiers of the same category cannot be installed together");
        check(!menu.getSlot(36).mayPlace(new ItemStack(upgrade(UpgradeItem.Category.PLAYER, 0))), "Player upgrade is rejected from general slots");
        check(menu.getSlot(40).mayPlace(new ItemStack(upgrade(UpgradeItem.Category.PLAYER, 0))) && !menu.getSlot(40).mayPlace(new ItemStack(upgrade(UpgradeItem.Category.ENERGY, 1))), "Fifth upgrade slot accepts only Player");
        check(new ItemStack(upgrade(UpgradeItem.Category.ENERGY, 1)).getMaxStackSize() == 1, "Upgrade items have native stack limit one");
        machine.upgrades.clearContent();
        player.getInventory().setItem(9, new ItemStack(upgrade(UpgradeItem.Category.SPEED, 5)));
        menu.quickMoveStack(player, 41);
        check(machine.upgrades.getItem(0).getItem() == upgrade(UpgradeItem.Category.SPEED, 5) && player.getInventory().getItem(9).isEmpty(), "Shift insertion moves exactly one upgrade");
        menu.quickMoveStack(player, 36);
        check(machine.upgrades.isEmpty(), "Shift extraction removes an installed upgrade");
        player.getInventory().clearContent();
        machine.addGeneratedLoot(new ItemStack(Items.DIAMOND), 200);
        menu.handleClick(0, 1, "PICKUP", player);
        check(menu.getCarried().getCount() == 1 && machine.count(0) == 199, "Right click withdraws one item");
        menu.setCarried(ItemStack.EMPTY);
        menu.handleClick(0, 0, "PICKUP", player);
        check(menu.getCarried().getCount() == 64 && machine.count(0) == 135, "Left click withdraws one native stack");
        menu.handleClick(0, 0, "QUICK_CRAFT", player);
        check(machine.count(0) == 135 && menu.getCarried().getCount() == 64, "Dragging onto output cannot insert or duplicate");
        menu.setCarried(ItemStack.EMPTY);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 64));
        menu.handleClick(0, 0, "SWAP", player);
        check(machine.count(0) == 135 && player.getInventory().getItem(0).is(Items.DIRT), "Number key never swaps unrelated hotbar contents into output");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        menu.handleClick(0, 0, "SWAP", player);
        check(machine.count(0) == 71 && player.getInventory().getItem(0).getCount() == 64, "Number key withdraws a native stack into empty hotbar slot");
        check(!menu.canTakeItemForPickAll(new ItemStack(Items.DIAMOND), menu.getSlot(0)), "Double click cannot mutate output view slots");

        machine.outputs.clear(); player.getInventory().clearContent();
        machine.addGeneratedLoot(new ItemStack(Items.EMERALD), Long.MAX_VALUE);
        for (int i = 1; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        menu.quickMoveStack(player, 0);
        check(player.getInventory().getItem(0).getCount() == 64 && machine.count(0) == Long.MAX_VALUE - 64, "Shift extraction at long maximum stops when player inventory is full");
        ServerPlayer second = VerificationMod.player(level, "JglTestB");
        second.setPos(.5, 80.5, .5); second.getInventory().clearContent();
        for (int i = 1; i < 36; i++) second.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        LootMenu otherMenu = new LootMenu(2, second.getInventory(), machine); second.containerMenu = otherMenu;
        otherMenu.quickMoveStack(second, 0);
        check(machine.count(0) == Long.MAX_VALUE - 128 && second.getInventory().getItem(0).getCount() == 64, "Two viewers consume the same server-owned counter without duplication");
        FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        long[] counts = new long[36]; counts[0] = Long.MAX_VALUE; counts[35] = 2_147_483_649L;
        new CountState(7, counts).write(buffer); CountState decoded = CountState.read(buffer); buffer.release();
        check(decoded.menuId() == 7 && decoded.counts()[0] == Long.MAX_VALUE && decoded.counts()[35] == 2_147_483_649L, "Menu count packet preserves every long bit");
        machine.outputs.clear();
        VerificationMod.capabilities(machine);

        machine.outputs.clear(); machine.addGeneratedLoot(new ItemStack(Items.IRON_INGOT), 1000);
        BlockPos hopperPos = pos.below(); level.setBlockAndUpdate(hopperPos, Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopper = (HopperBlockEntity) level.getBlockEntity(hopperPos);
        boolean pulled = HopperBlockEntity.suckInItems(level, hopper);
        check(pulled && machine.count(0) == 999 && hopper.getItem(0).is(Items.IRON_INGOT), "Real vanilla hopper extracts through loader capability");
        hopper.clearContent(); level.setBlockAndUpdate(hopperPos, Blocks.AIR.defaultBlockState());
        machine.outputs.clear();
        machine.addGeneratedLoot(new ItemStack(Items.STONE), 299L * 64);
        check(machine.dropStacks(true) == 300 && !machine.guarded(), "Exactly 300 normal piles including block do not require confirmation");
        machine.addGeneratedLoot(new ItemStack(Items.STONE), 1);
        check(machine.dropStacks(true) == 301 && machine.guarded(), "301 normal piles including block require confirmation");
        player.setShiftKeyDown(true);
        check(Protection.deny(machine, player), "Fake player cannot bypass large-drop protection by sneaking");
        VerificationMod.protection(machine, player);
        VerificationMod.confirmation(machine);
        level.explode(null, .5, 80.5, .5, 4F, net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        check(level.getBlockEntity(pos) == machine && machine.count(0) == 299L * 64 + 1, "Real explosion preserves a machine above the 300-pile threshold");
        machine.outputs.clear(); machine.addGeneratedLoot(new ItemStack(Items.DIAMOND_SWORD), 300);
        check(machine.guarded(), "Unstackable items count as separate drop piles");
        machine.outputs.clear();
        machine.addGeneratedLoot(new ItemStack(Items.GOLD_INGOT), 129);
        machine.upgrades.setItem(4, new ItemStack(upgrade(UpgradeItem.Category.PLAYER, 0)));
        machine.lava = 16000; machine.energy = 16000;
        level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).forEach(ItemEntity::discard);
        level.explode(null, .5, 80.5, .5, 4F, net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4));
        check(level.getBlockEntity(pos) == null, "Small machine is destroyed normally by a real explosion");
        check(drops.size() == 5 && drops.stream().filter(entity -> entity.getItem().is(Items.GOLD_INGOT)).mapToInt(entity -> entity.getItem().getCount()).sum() == 129, "Drops emit exactly one entity per native stack plus upgrade and block");
        check(drops.stream().anyMatch(entity -> entity.getItem().is(ModContent.BLOCK.get().asItem())), "Versioned block loot table drops an empty machine item");
        check(machine.lava == 0 && machine.energy == 0 && machine.upgrades.isEmpty() && machine.count(0) == 0, "Breaking voids lava and FE and clears all contents");
        machine.dropContents();
        check(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).size() == 5, "Repeated removal callback cannot duplicate drops");
        drops.forEach(ItemEntity::discard);
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        player.containerMenu = player.inventoryMenu; second.containerMenu = second.inventoryMenu;
    }
}
