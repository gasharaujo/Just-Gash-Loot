package net.gasharaujo.justgashloot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
public final class TabletEvents {
    private TabletEvents() {}
    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.HIGHEST, TabletEvents::block);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.HIGHEST, TabletEvents::entity);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.HIGHEST, TabletEvents::specific);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(TabletEvents::login);
    }
    private static void block(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (!Tablets.isTablet(event.getItemStack()) || event.getEntity().isSpectator()) return;
        event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.isShiftKeyDown()) Tablets.sync(player, true, "", 0);
            else if (event.getLevel().getBlockEntity(event.getPos()) instanceof LootEntity machine) Tablets.configure(player, machine);
            else Tablets.capture(player, event.getLevel().getBlockEntity(event.getPos()));
        }
    }
    private static void entity(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (!Tablets.isTablet(event.getItemStack()) || event.getEntity().isSpectator()) return;
        event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.isShiftKeyDown()) Tablets.sync(player, true, "", 0); else Tablets.capture(player, event.getTarget());
        }
    }
    private static void specific(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific event) {
        if (!Tablets.isTablet(event.getItemStack()) || event.getEntity().isSpectator()) return;
        event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.isShiftKeyDown()) Tablets.sync(player, true, "", 0); else Tablets.capture(player, event.getTarget());
        }
    }
    private static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) Tablets.sync(player, false, "", 0);
    }
}
