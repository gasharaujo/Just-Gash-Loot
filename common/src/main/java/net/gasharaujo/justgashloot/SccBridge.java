package net.gasharaujo.justgashloot;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;

/** Optional SCC adapter. SCC owns deposit priorities, filters, overflow policies and network state. */
public final class SccBridge {
    private static volatile boolean enabled;
    private static Class<?> nodeType;
    private static Method network, conducts, online, insert, proxyCore;
    private SccBridge() {}
    public static void initialize() {
        if (!Platform.sccLoaded()) return;
        try {
            nodeType = Class.forName("de.scc.calculator.network.NetworkBlockEntity");
            Class<?> viewType = Class.forName("de.scc.calculator.network.NetworkView");
            network = nodeType.getMethod("network"); conducts = nodeType.getMethod("conducts");
            online = viewType.getMethod("online"); insert = viewType.getMethod("insert", ItemStack.class, boolean.class);
            try { proxyCore = Class.forName("de.scc.calculator.network.NetworkProxy").getMethod("coreOf", BlockGetter.class, BlockPos.class); }
            catch (ReflectiveOperationException | LinkageError ignored) { proxyCore = null; }
            Class<?> linkType = Class.forName("de.scc.calculator.network.ForeignLinks$Link");
            Object link = Proxy.newProxyInstance(linkType.getClassLoader(), new Class<?>[]{linkType}, (proxy, method, args) -> {
                return switch (method.getName()) {
                    case "connects" -> ((BlockGetter) args[0]).getBlockEntity((BlockPos) args[1]) instanceof LootEntity;
                    // A generator is an endpoint; it must not bridge two independent SCC networks.
                    case "reach" -> null;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "Just Gash Loot SCC endpoint";
                    default -> null;
                };
            });
            Class.forName("de.scc.calculator.network.ForeignLinks").getMethod("register", linkType).invoke(null, link);
            enabled = true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            com.mojang.logging.LogUtils.getLogger().warn("SCC deposit integration is unavailable; Loot Generators will use local storage", error);
        }
    }
    public static boolean enabled() { return enabled; }
    private static List<Object> views(LootEntity machine) {
        if (!enabled || !(machine.getLevel() instanceof ServerLevel level)) return List.of();
        var seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        var result = new ArrayList<Object>();
        try {
            for (Direction face : Direction.values()) {
                BlockPos pos = machine.getBlockPos().relative(face);
                if (!level.hasChunkAt(pos)) continue;
                Object node = level.getBlockEntity(pos);
                if (node == null && proxyCore != null) {
                    Object core = proxyCore.invoke(null, level, pos);
                    if (core instanceof BlockPos root && level.hasChunkAt(root)) node = level.getBlockEntity(root);
                }
                if (!nodeType.isInstance(node) || !Boolean.TRUE.equals(conducts.invoke(node))) continue;
                Object view = network.invoke(node);
                if (seen.add(view) && Boolean.TRUE.equals(online.invoke(view))) result.add(view);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) { disable(error); }
        return result;
    }
    public static ItemStack deposit(LootEntity machine, ItemStack stack) { return deposit(views(machine), stack, false); }
    private static ItemStack deposit(List<Object> views, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (Object view : views) {
            if (remaining.isEmpty()) break;
            try {
                ItemStack returned = (ItemStack) insert.invoke(view, remaining.copy(), simulate);
                if (returned == null || returned.getCount() > remaining.getCount() || !returned.isEmpty() && !Platform.sameItem(returned, remaining))
                    throw new IllegalStateException("SCC returned an invalid deposit remainder");
                remaining = returned;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
                disable(error);
                // A failing external write must not be replayed as another local or network insert.
                throw new IllegalStateException("SCC deposit failed", error);
            }
        }
        return remaining;
    }
    /** Retry buffered output at most once per ten ticks, one normal stack from each occupied slot. */
    public static void flush(LootEntity machine) {
        if (!enabled || machine.getLevel() == null || Math.floorMod(machine.getLevel().getGameTime() + machine.getBlockPos().asLong(), 10) != 0) return;
        flushNow(machine);
    }
    static void flushNow(LootEntity machine) {
        List<Object> targets = views(machine);
        if (targets.isEmpty()) return;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack offered = machine.extract(slot, Integer.MAX_VALUE, true);
            if (offered.isEmpty()) continue;
            int accepted = offered.getCount() - deposit(targets, offered, true).getCount();
            if (accepted == 0) continue;
            ItemStack remaining = deposit(targets, machine.extract(slot, accepted, false), false);
            // Restore directly: existing stock is never re-evaluated by the generation-only Void Filter.
            if (!remaining.isEmpty()) { machine.outputs.insert(remaining, remaining.getCount()); machine.setChanged(); }
        }
    }
    private static void disable(Throwable error) {
        if (enabled) {
            enabled = false;
            com.mojang.logging.LogUtils.getLogger().error("SCC deposit integration disabled after an API failure", error);
        }
    }
}
