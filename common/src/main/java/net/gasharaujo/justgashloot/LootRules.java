package net.gasharaujo.justgashloot;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class LootRules {
    public static final int OUTPUT_SLOTS = 36, UPGRADE_SLOTS = 5;
    public static final int LAVA_CAPACITY = 16_000, ENERGY_CAPACITY = 16_000;
    public static final int SAFE_DROP_STACKS = 300;
    private LootRules() {}

    /** Saturate at 301: even 36 full long counters cannot overflow this sum. */
    public static int addDropStacks(int previous, long count, int stackSize) {
        if (count <= 0) return previous;
        if (stackSize < 1) throw new IllegalArgumentException("Invalid stack size");
        long piles = count / stackSize + (count % stackSize == 0 ? 0 : 1);
        return previous >= 301 || piles > 300 - previous ? 301 : previous + (int) piles;
    }
    public static String compact(long count) {
        if (count < 1000) return Long.toString(count);
        String[] suffixes = {"", "k", "M", "B", "T", "P", "E"};
        BigDecimal value = BigDecimal.valueOf(count);
        int unit = 0;
        while (value.compareTo(BigDecimal.valueOf(1000)) >= 0 && unit < 6) {
            value = value.movePointLeft(3); unit++;
        }
        return value.setScale(value.compareTo(BigDecimal.TEN) < 0 ? 1 : 0, RoundingMode.DOWN)
                .stripTrailingZeros().toPlainString() + suffixes[unit];
    }
}
