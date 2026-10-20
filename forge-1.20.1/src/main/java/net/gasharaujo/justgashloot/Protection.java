package net.gasharaujo.justgashloot;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraft.world.entity.player.Player;
final class Protection {
    static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(Protection::start);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(Protection::breaking);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(Protection::explosion);
    }
    static boolean deny(LootEntity machine, Player player) {
        if (machine.dropStacks(player instanceof FakePlayer || !player.getAbilities().instabuild) <= LootRules.SAFE_DROP_STACKS) return false;
        if (player instanceof FakePlayer) return true;
        if (!player.isShiftKeyDown()) { machine.warn(player); return true; }
        return false;
    }
    private static void start(PlayerInteractEvent.LeftClickBlock event) {
        if (!Platform.clientSide(event.getLevel()) && event.getLevel().getBlockEntity(event.getPos()) instanceof LootEntity machine && deny(machine, event.getEntity())) event.setCanceled(true);
    }
    private static void breaking(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof LootEntity machine && deny(machine, event.getPlayer())) { event.setCanceled(true);  }
    }
    private static void explosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> event.getLevel().getBlockEntity(pos) instanceof LootEntity machine && machine.guarded());
    }
}
