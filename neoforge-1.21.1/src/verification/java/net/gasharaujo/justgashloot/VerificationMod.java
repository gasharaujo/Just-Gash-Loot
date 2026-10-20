package net.gasharaujo.justgashloot;
import net.minecraft.world.item.*;
import net.minecraft.server.level.*;
public final class VerificationMod {
    public static void install() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent event) -> VerificationScene.commands(event.getDispatcher()));
        if (Boolean.getBoolean("justgashloot.uiVerification") && net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) net.gasharaujo.justgashloot.client.VerificationClient.install();
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
        machine.addGeneratedLoot(new ItemStack(Items.REDSTONE), 2_147_483_649L);
        VerificationChecks.check(cap.items.getStackInSlot(0).getCount() == Integer.MAX_VALUE, "Legacy item query exposes full representable count instead of one native stack");
        VerificationChecks.check(cap.items.extractItem(0, 64, true).getCount() == 64 && machine.count(0) == 2_147_483_649L, "Legacy cable simulation at huge count never changes actual stock");
        machine.outputs.clear(); machine.addGeneratedLoot(new ItemStack(Items.REDSTONE), 100);
        VerificationChecks.check(cap.items.getStackInSlot(0).getCount() == 100, "Legacy cable query sees all 100 items instead of 64");
        VerificationChecks.check(cap.items.insertItem(0, new ItemStack(Items.REDSTONE, 64), false).getCount() == 64, "Automation cannot insert into bulk output");
        VerificationChecks.check(cap.items.extractItem(0, 64, true).getCount() == 64 && machine.count(0) == 100, "Item handler simulation preserves bulk count");
        cap.items.extractItem(0, 9, false);
        VerificationChecks.check(machine.count(0) == 91, "Item handler extraction decrements bulk count");
        var water = new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000);
        var lava = new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 20000);
        VerificationChecks.check(cap.fluids.fill(water, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 0, "Lava tank rejects water");
        cap.fluids.fill(lava, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE); cap.energy.receiveEnergy(20000, true);
        VerificationChecks.check(machine.lava == 0 && machine.energy == 0, "Fluid and energy simulations do not mutate state");
        cap.fluids.fill(lava, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE); cap.energy.receiveEnergy(20000, false);
        VerificationChecks.check(machine.lava == 16000 && machine.energy == 16000, "Fluid and energy insertion stop at capacity");
        VerificationChecks.check(cap.energy.extractEnergy(100, false) == 0, "Machine energy cannot be extracted");
        cap.fluids.drain(1000, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        VerificationChecks.check(machine.lava == 15000, "Lava can be drained through standard fluid handler");
        for (var side : net.minecraft.core.Direction.values()) {
            VerificationChecks.check(machine.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, machine.getBlockPos(), side) != null && machine.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, machine.getBlockPos(), side) != null, "Standard HUD and pipe capabilities available on face " + side);
        }

    }
    static void protection(LootEntity machine, ServerPlayer player) {
        var event = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(machine.getLevel(), machine.getBlockPos(), machine.getBlockState(), player);
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
        VerificationChecks.check(group.getExtraData().getLongArray("Counts")[0] == Long.MAX_VALUE && group.views.get(0).getCount() == 1, "Jade bulk provider carries exact long count separately from safe icon");
        var data = new net.minecraft.nbt.CompoundTag();
        var accessor = (snownee.jade.api.BlockAccessor) java.lang.reflect.Proxy.newProxyInstance(VerificationMod.class.getClassLoader(), new Class<?>[]{snownee.jade.api.BlockAccessor.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getBlockEntity", "getTarget" -> machine;
            case "getLevel" -> machine.getLevel();
            case "getServerData" -> data;
            default -> null;
        });
        new JadeCompat.ExactCounts().appendServerData(data, accessor);
        VerificationChecks.check(data.getString("JglTable").equals(VerificationV2Checks.BASIC) && data.getString("JglOwner").equals("JadeInstaller"), "Jade payload identifies the configured table and Player Upgrade installer");
        machine.memory = previousMemory;
        var decoded = StoredLoot.CODEC.listOf().parse(machine.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), data.get("JglExactLoot")).result().orElseThrow();
        VerificationChecks.check(decoded.get(0).count() == Long.MAX_VALUE, "Jade server payload preserves exact long maximum through NBT codec");
        machine.outputs.clear();
    }

}
