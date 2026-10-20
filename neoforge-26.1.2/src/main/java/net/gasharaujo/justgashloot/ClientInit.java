package net.gasharaujo.justgashloot;
final class ClientInit {
    static void register(net.neoforged.bus.api.IEventBus bus) { if (net.neoforged.fml.loading.FMLEnvironment.getDist().isClient()) ClientOnly.register(bus); }
    private static final class ClientOnly { static void register(net.neoforged.bus.api.IEventBus bus) { bus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) -> event.register(ModContent.MENU.get(), net.gasharaujo.justgashloot.client.LootScreen::new)); } }
}
