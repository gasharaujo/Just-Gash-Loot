package net.gasharaujo.justgashloot;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.UnaryOperator;

/** Counts never enter an ItemStack. All calls run on the owning server thread. */
public final class BulkInventory<T> {
    private final Object[] types;
    private final long[] counts;
    private long revision;
    private final BiPredicate<T, T> same;
    private final UnaryOperator<T> copy;

    public BulkInventory(int size, BiPredicate<T, T> same, UnaryOperator<T> copy) {
        this.types = new Object[size];
        this.counts = new long[size];
        this.same = same;
        this.copy = copy;
    }
    public int size() { return counts.length; }
    public long revision() { return revision; }
    @SuppressWarnings("unchecked")
    public T type(int slot) { return (T) types[Objects.checkIndex(slot, size())]; }
    public long count(int slot) { return counts[Objects.checkIndex(slot, size())]; }

    /** Returns the amount that did not fit. Matching slots are filled first. */
    public long insert(T type, long amount) {
        Objects.requireNonNull(type);
        if (amount < 0) throw new IllegalArgumentException("Negative amount");
        long remaining = amount;
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int i = 0; i < size() && remaining > 0; i++) {
                if (pass == 0 ? counts[i] == 0 || !same.test(type(i), type) : counts[i] != 0) continue;
                long moved = Math.min(remaining, Long.MAX_VALUE - counts[i]);
                if (moved == 0) continue;
                if (counts[i] == 0) types[i] = copy.apply(type);
                counts[i] += moved;
                revision++;
                remaining -= moved;
            }
        }
        return remaining;
    }
    public int extract(int slot, int amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative amount");
        int moved = (int) Math.min(count(slot), amount);
        counts[slot] -= moved;
        if (moved > 0) revision++;
        if (counts[slot] == 0) types[slot] = null;
        return moved;
    }
    public void restore(int slot, T type, long count) {
        Objects.checkIndex(slot, size());
        if (count < 0) throw new IllegalArgumentException("Negative count");
        types[slot] = count == 0 ? null : copy.apply(Objects.requireNonNull(type));
        counts[slot] = count;
        revision++;
    }
    public void clear() {
        revision++;
        java.util.Arrays.fill(types, null);
        java.util.Arrays.fill(counts, 0);
    }
    public Snapshot snapshot() { return new Snapshot(types.clone(), counts.clone()); }
    public void restore(Snapshot snapshot) {
        revision++;
        System.arraycopy(snapshot.types, 0, types, 0, size());
        System.arraycopy(snapshot.counts, 0, counts, 0, size());
    }
    public static final class Snapshot {
        private final Object[] types;
        private final long[] counts;
        private Snapshot(Object[] types, long[] counts) { this.types = types; this.counts = counts; }
    }
}
