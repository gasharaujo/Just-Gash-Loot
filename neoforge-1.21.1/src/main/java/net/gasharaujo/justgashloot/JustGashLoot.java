package net.gasharaujo.justgashloot;
@net.neoforged.fml.common.Mod(JustGashLoot.MOD_ID)
public final class JustGashLoot {
    public static final String MOD_ID = "justgashloot";
    public JustGashLoot(net.neoforged.bus.api.IEventBus bus) {

        ModContent.register(bus); UiNetwork.register(bus); ClientInit.register(bus); Protection.register(); TabletNetwork.register(bus); TabletEvents.register(); EnchantedIngredient.register(bus);
        if (Boolean.getBoolean("justgashloot.verification")) {
            try { Class.forName("net.gasharaujo.justgashloot.VerificationMod").getMethod("install").invoke(null); }
            catch (ReflectiveOperationException error) { throw new IllegalStateException("Verification sources are required for this development run", error); }
        }
    }
}
