package net.gasharaujo.justgashloot;

import java.util.Objects;

public final class CoreChecks {
    private static int checks;
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }
    public static void main(String[] args) {
        BulkInventory<String> inventory = new BulkInventory<>(2, Objects::equals, value -> value);
        check(inventory.insert("a", Long.MAX_VALUE) == 0, "long maximum fits");
        check(inventory.count(0) == Long.MAX_VALUE, "exact long maximum");
        check(inventory.insert("a", 100) == 0 && inventory.count(1) == 100, "overflow uses next slot");
        check(inventory.insert("b", 1) == 1, "full inventory returns remainder");
        BulkInventory.Snapshot snapshot = inventory.snapshot();
        check(inventory.extract(0, 64) == 64 && inventory.count(0) == Long.MAX_VALUE - 64, "large extraction");
        inventory.restore(snapshot);
        check(inventory.count(0) == Long.MAX_VALUE && inventory.count(1) == 100, "transaction snapshot restoration");
        inventory.clear();
        check(inventory.insert("a", 2_147_483_649L) == 0, "above signed integer");
        check(inventory.extract(0, Integer.MAX_VALUE) == Integer.MAX_VALUE && inventory.count(0) == 2, "integer extraction boundary");
        check(inventory.extract(0, 64) == 2 && inventory.type(0) == null, "empty slot clears identity");
        check(LootRules.addDropStacks(1, 299L * 64, 64) == 300, "300 piles allowed");
        check(LootRules.addDropStacks(1, 299L * 64 + 1, 64) == 301, "301 piles guarded");
        check(LootRules.addDropStacks(0, 300, 1) == 300, "unstackable piles");
        check(LootRules.addDropStacks(300, Long.MAX_VALUE, 1) == 301, "drop count cannot overflow");
        check(LootRules.compact(Long.MAX_VALUE).equals("9.2E"), "maximum count formatting");
        check(LootRules.compact(2_147_483_649L).equals("2.1B"), "billion formatting");
        java.util.Random random = new java.util.Random(42);
        BulkInventory<String> randomInventory = new BulkInventory<>(1, Objects::equals, value -> value);
        long model = 0;
        for (int i = 0; i < 10_000; i++) {
            int amount = random.nextInt(100_000);
            if (random.nextBoolean()) { randomInventory.insert("x", amount); model += amount; }
            else { int moved = randomInventory.extract(0, amount); check(moved == Math.min(model, amount), "random extraction"); model -= moved; }
            check(randomInventory.count(0) == model, "random conservation");
        }
        System.out.println("PASS: " + checks + " core checks");
    }
}
