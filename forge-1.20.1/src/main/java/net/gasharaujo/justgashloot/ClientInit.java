package net.gasharaujo.justgashloot;
final class ClientInit {
    static void register(net.minecraftforge.eventbus.api.IEventBus bus) { if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) ClientOnly.register(bus); }
    private static final class ClientOnly { static void register(net.minecraftforge.eventbus.api.IEventBus bus) { bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) -> event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(ModContent.MENU.get(), net.gasharaujo.justgashloot.client.LootScreen::new))); } }
}
