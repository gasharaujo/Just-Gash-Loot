package net.gasharaujo.justgashloot;
import net.minecraft.world.item.*;
import net.minecraft.server.level.*;
public final class VerificationMod {
    public static void install() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent event) -> VerificationScene.commands(event.getDispatcher()));
        if (Boolean.getBoolean("justgashloot.uiVerification") && net.neoforged.fml.loading.FMLEnvironment.getDist().isClient()) net.gasharaujo.justgashloot.client.VerificationClient.install();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent event) -> { if (!Boolean.getBoolean("justgashloot.uiVerification")) VerificationChecks.run(event.getServer()); });
    }
    static ServerPlayer player(ServerLevel level, String name) {
        return net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name));
    }
    static void name(ItemStack stack, String name) { stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(name)); }
    static LootEntity roundTrip(LootEntity machine) {
        return (LootEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), machine.saveWithFullMetadata(machine.getLevel().registryAccess()), machine.getLevel().registryAccess());
    }
    static void capabilities(LootEntity machine) {
        if (net.neoforged.fml.ModList.get().isLoaded("jade")) jadeData(machine);

        var cap = machine.capabilities;
        machine.outputs.clear(); machine.lava = machine.energy = 0;
        machine.addGeneratedLoot(new ItemStack(Items.REDSTONE), 100);
        var redstone = net.neoforged.neoforge.transfer.item.ItemResource.of(Items.REDSTONE);
        var lava = net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.LAVA);
        var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);
        try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            VerificationChecks.check(cap.items.insert(0, redstone, 64, tx) == 0, "Automation cannot insert into bulk output");
            VerificationChecks.check(cap.fluids.insert(0, water, 1000, tx) == 0, "Lava tank rejects water");
            cap.items.extract(0, redstone, 9, tx); cap.fluids.insert(0, lava, 1000, tx); cap.energy.insert(999, tx);
        }
        VerificationChecks.check(machine.count(0) == 100 && machine.lava == 0 && machine.energy == 0, "Aborted transaction rolls back items, lava and energy together");
        try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            cap.items.extract(0, redstone, 1, tx); cap.fluids.insert(0, lava, 20000, tx); cap.energy.insert(20000, tx); tx.commit();
        }
        VerificationChecks.check(machine.count(0) == 99 && machine.lava == 16000 && machine.energy == 16000, "Committed transaction persists exact extraction and capacity limits");
        try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            VerificationChecks.check(cap.energy.extract(100, tx) == 0, "Machine energy cannot be extracted");
            cap.fluids.extract(0, lava, 1000, tx); tx.commit();
        }
        VerificationChecks.check(machine.lava == 15000, "Lava can be drained through standard resource handler");
        for (var side : net.minecraft.core.Direction.values()) {
            VerificationChecks.check(machine.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, machine.getBlockPos(), side) != null
                && machine.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, machine.getBlockPos(), side) != null, "Standard HUD and pipe capabilities available on face " + side);
        }

    }
    static void protection(LootEntity machine, ServerPlayer player) {
        var event = new net.neoforged.neoforge.event.level.block.BreakBlockEvent(machine.getLevel(), machine.getBlockPos(), machine.getBlockState(), player);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        VerificationChecks.check(event.isCanceled(), "Loader break event cancels automatic large drops");
    }
    static void confirmation(LootEntity machine) {
        var messages = new java.util.ArrayList<net.minecraft.network.chat.Component>();
        var level = (ServerLevel) machine.getLevel();
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("JglHuman".getBytes(java.nio.charset.StandardCharsets.UTF_8)), "JglHuman");
        var human = new ServerPlayer(level.getServer(), level, profile, net.minecraft.server.level.ClientInformation.createDefault()) {
            @Override public void sendSystemMessage(net.minecraft.network.chat.Component message) { messages.add(message); }
        };
        human.setShiftKeyDown(false);
        human.getAbilities().instabuild = true;
        VerificationChecks.check(machine.dropStacks(false) == 300 && !Protection.deny(machine, human), "Creative break excludes the block item when counting actual drop piles");
        human.getAbilities().instabuild = false;
        VerificationChecks.check(Protection.deny(machine, human) && messages.size() == 1, "Real player receives chat warning and cannot break without sneaking");
        human.setShiftKeyDown(true);
        VerificationChecks.check(!Protection.deny(machine, human), "Sneaking confirms large drops for a real player");
    }

    static void jadeData(LootEntity machine) {
        var previousMemory = machine.memory; machine.memory = new MachineMemory();
        machine.memory.table = VerificationV2Checks.BASIC; machine.memory.owner = new java.util.UUID(0, 2); machine.memory.ownerName = "JadeInstaller";
        machine.outputs.clear(); machine.addGeneratedLoot(new ItemStack(Items.DIAMOND), Long.MAX_VALUE);
        var group = JadeCompat.groups(machine).get(0);
        VerificationChecks.check(group.getExtraData().getLongArray("Counts").orElse(new long[0])[0] == Long.MAX_VALUE && group.views.get(0).getCount() == 1, "Jade bulk provider carries exact long count separately from safe icon");
        var data = new net.minecraft.nbt.CompoundTag();
        var accessor = (snownee.jade.api.BlockAccessor) java.lang.reflect.Proxy.newProxyInstance(VerificationMod.class.getClassLoader(), new Class<?>[]{snownee.jade.api.BlockAccessor.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getBlockEntity", "getTarget" -> machine;
            case "getLevel" -> machine.getLevel();
            case "getServerData" -> data;
            default -> null;
        });
        new JadeCompat.ExactCounts().appendServerData(data, accessor);
        VerificationChecks.check(data.getStringOr("JglTable", "").equals(VerificationV2Checks.BASIC) && data.getStringOr("JglOwner", "").equals("JadeInstaller"), "Jade payload identifies the configured table and Player Upgrade installer");
        machine.memory = previousMemory;
        var decoded = StoredLoot.CODEC.listOf().parse(machine.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), data.get("JglExactLoot")).result().orElseThrow();
        VerificationChecks.check(decoded.get(0).count() == Long.MAX_VALUE, "Jade server payload preserves exact long maximum through NBT codec");
        machine.outputs.clear();
    }

}
