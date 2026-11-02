package net.gasharaujo.justgashloot;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
final class VerificationScc {
    private static void check(boolean value, String text) { VerificationChecks.check(value, text); }
    private static Object call(Object target, String name, Object... args) throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != args.length) continue;
            boolean match = true;
            for (int i = 0; i < args.length; i++) if (!method.getParameterTypes()[i].isPrimitive() && !method.getParameterTypes()[i].isInstance(args[i])) match = false;
            if (match) return method.invoke(target, args);
        }
        throw new NoSuchMethodException(name);
    }
    private static void invalidate(ServerLevel level) throws Exception { Class.forName("de.scc.calculator.network.NetworkCache").getMethod("invalidate", net.minecraft.world.level.Level.class).invoke(null, level); }
    private static long count(Object storage, Item item) throws Exception { return ((Number) call(storage, "countItem", item)).longValue(); }
    private static Object place(ServerLevel level, BlockPos pos, String name) {
        var block = BuiltInRegistries.BLOCK.getOptional(LootRuntime.id("scc:" + name)).orElseThrow();
        level.setBlockAndUpdate(pos, block.defaultBlockState()); return level.getBlockEntity(pos);
    }
    private static void filter(Object storage, Item item) throws Exception {
        String entry = (String) Class.forName("de.scc.calculator.network.ItemFilter").getMethod("itemEntry", Item.class).invoke(null, item);
        call(storage, "setFilterEntry", 0, entry); call(storage, "setFilterBlacklist", false);
    }
    static void run(ServerLevel level) {
        if (!Platform.sccLoaded()) { check(!SccBridge.enabled(), "SCC integration remains optional when SCC is absent"); return; }
        check(SccBridge.enabled(), "Installed SCC API is bound without a mandatory runtime dependency");
        BlockPos generator = new BlockPos(50, 80, 0), cable = generator.east(), controller = cable.east(), highPos = controller.east(), lowPos = controller.south();
        List<BlockPos> positions = List.of(generator, cable, controller, highPos, lowPos);
        try {
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(), "scc power off");
            for (BlockPos pos : positions) { level.removeBlockEntity(pos); level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
            level.setBlockAndUpdate(generator, ModContent.BLOCK.get().defaultBlockState());
            var machine = (LootEntity) level.getBlockEntity(generator);
            place(level, cable, "cable"); place(level, controller, "calculator_controller");
            Object high = place(level, highPos, "item_storage"), low = place(level, lowPos, "item_storage");
            call(high, "setPriority", 100); call(low, "setPriority", 0); filter(high, Items.DIAMOND); filter(low, Items.DIAMOND); invalidate(level);
            check(Boolean.TRUE.equals(Class.forName("de.scc.calculator.network.ForeignLinks").getMethod("connects", net.minecraft.world.level.BlockGetter.class, BlockPos.class, Direction.class).invoke(null, level, generator, Direction.WEST)), "SCC cables recognize the Loot Generator as a connected endpoint");
            machine.addGeneratedLoot(new ItemStack(Items.DIAMOND), 7);
            check(count(high, Items.DIAMOND) == 7 && count(low, Items.DIAMOND) == 0 && machine.count(0) == 0, "Direct SCC deposits follow native storage priority and avoid local duplication");
            machine.setVoidRules(List.of("minecraft:diamond")); machine.addGeneratedLoot(new ItemStack(Items.DIAMOND), 64);
            check(count(high, Items.DIAMOND) == 7, "Void Filter runs before optional SCC deposits");
            machine.setVoidRules(List.of()); machine.addGeneratedLoot(new ItemStack(Items.STONE), 3);
            check(machine.count(0) == 3 && count(high, Items.STONE) == 0 && count(low, Items.STONE) == 0, "SCC filter rejection buffers the unchanged remainder in the generator");
            filter(high, Items.STONE); invalidate(level); machine.setVoidRules(List.of("minecraft:stone")); SccBridge.flushNow(machine);
            check(machine.count(0) == 0 && count(high, Items.STONE) == 3, "Buffered items drain into SCC when accepted without applying generation-only void rules retroactively");
            ItemStack named = new ItemStack(Items.DIAMOND); VerificationMod.name(named, "SCC component test"); machine.addGeneratedLoot(named, 4);
            ItemStack simulated = (ItemStack) call(low, "extractExact", named, 4, true);
            check(simulated.getCount() == 4 && Platform.sameItem(named, simulated), "SCC deposits preserve item data and components");
            Object flow = call(high, "flow"), both = null, noWrites = null;
            for (Object mode : flow.getClass().getEnumConstants()) {
                if (Boolean.TRUE.equals(call(mode, "writes")) && Boolean.TRUE.equals(call(mode, "reads"))) both = mode;
                if (Boolean.FALSE.equals(call(mode, "writes"))) noWrites = mode;
            }
            if (both == null || noWrites == null) throw new AssertionError("Missing native SCC flow modes");
            call(high, "setFlow", noWrites); call(low, "setFlow", noWrites); invalidate(level);
            machine.setVoidRules(List.of()); machine.addGeneratedLoot(new ItemStack(Items.DIAMOND), 2);
            check(machine.count(0) == 2, "SCC deposit-disabled flow modes are respected");
            machine.outputs.clear(); call(high, "setFlow", both); call(low, "setFlow", both); filter(high, Items.DIAMOND); invalidate(level);
            var highExcess = (ItemStack) call(high, "insert", new ItemStack(Items.DIAMOND, Integer.MAX_VALUE), false);
            var lowExcess = (ItemStack) call(low, "insert", new ItemStack(Items.DIAMOND, Integer.MAX_VALUE), false);
            check(!highExcess.isEmpty() && !lowExcess.isEmpty(), "Native SCC basic storage capacity is bounded");
            invalidate(level); machine.addGeneratedLoot(new ItemStack(Items.DIAMOND), 8);
            check(machine.count(0) == 8, "A full SCC network preserves overflow in local storage");
            long revision = machine.outputs.revision(); SccBridge.flushNow(machine);
            check(machine.count(0) == 8 && machine.outputs.revision() == revision, "Rejected SCC retries do not retransmit unchanged inventory counts");
            machine.outputs.clear(); call(high, "setVoidOverflow", true); invalidate(level);
            long voided = ((Number) call(high, "voidedItems")).longValue(); machine.addGeneratedLoot(new ItemStack(Items.DIAMOND), 8);
            check(machine.count(0) == 0 && ((Number) call(high, "voidedItems")).longValue() > voided, "Native SCC overflow-void deposit policy remains in control");
            level.removeBlockEntity(cable); level.setBlockAndUpdate(cable, Blocks.AIR.defaultBlockState()); invalidate(level);
            machine.addGeneratedLoot(new ItemStack(Items.IRON_INGOT), 5);
            check(machine.count(0) == 5 && machine.template(0).is(Items.IRON_INGOT), "Disconnected SCC integration falls back to local storage");
        } catch (Exception error) { throw new RuntimeException(error); }
        finally {
            for (BlockPos pos : positions) { level.removeBlockEntity(pos); level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(), "scc power on");
        }
    }
}
