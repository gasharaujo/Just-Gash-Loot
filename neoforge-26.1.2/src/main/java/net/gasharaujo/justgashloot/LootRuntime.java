package net.gasharaujo.justgashloot;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
public final class LootRuntime {
    private LootRuntime() {}
    public static ServerLevel level(ServerPlayer player) { return player.level(); }
    public static String playerName(net.minecraft.world.entity.player.Player player) { return player.getGameProfile().name(); }
    public static net.minecraft.resources.Identifier id(String value) { return net.minecraft.resources.Identifier.parse(value); }
    public static boolean validId(String value) { try { return !value.isEmpty() && id(value) != null; } catch (RuntimeException error) { return false; } }
    public static LootTable resolve(ServerLevel level, String value) {
        if (!validId(value)) return null;
        LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id(value)));
        return table == LootTable.EMPTY ? null : table;
    }
    public static List<ItemStack> roll(ServerLevel level, BlockPos pos, LootTable table, ServerPlayer player, int luck) {
        LootParams.Builder params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withLuck(luck + (player == null ? 0 : player.getLuck()));
        if (player != null) params.withParameter(LootContextParams.THIS_ENTITY, player);
        return table.getRandomItems(params.create(LootContextParamSets.CHEST));
    }
    public static ServerPlayer simulate(ServerLevel level, UUID uuid, String name) {
        // A private instance avoids sharing mutable fake-player equipment with other machines or mods.
        return new net.neoforged.neoforge.common.util.FakePlayer(level, new com.mojang.authlib.GameProfile(uuid, name));
    }
    public static String capture(Object target) {
        if (target == null) return null;
        // Lootr's public provider names differ between 1.20, 1.21 and 26.1.
        for (String method : List.of("getDataLootTable", "getInfoLootTable", "getTable")) {
            try {
                var getter = target.getClass().getMethod(method);
                for (String fixed : List.of("isDataReferenceInventory", "isInfoReferenceInventory")) {
                    try { if (Boolean.TRUE.equals(target.getClass().getMethod(fixed).invoke(target))) return null; }
                    catch (NoSuchMethodException ignored) {}
                }
                Object value = getter.invoke(target);
                if (value instanceof ResourceKey<?> key) return key.identifier().toString();
                return value instanceof net.minecraft.resources.Identifier location ? location.toString() : null;
            } catch (NoSuchMethodException ignored) {}
            catch (ReflectiveOperationException error) {
                com.mojang.logging.LogUtils.getLogger().warn("Cannot read Lootr table from {}", target.getClass().getName(), error);
                return null;
            }
        }
        if (target instanceof RandomizableContainerBlockEntity container) { var key = container.getLootTable(); return key == null ? null : key.identifier().toString(); }
        if (target instanceof ContainerEntity container) {
            var value = container.getContainerLootTable();
            return value == null ? null : value.identifier().toString();
        }
        return null;
    }
}
