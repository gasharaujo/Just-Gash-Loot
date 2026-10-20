package net.gasharaujo.justgashloot;
@net.minecraftforge.fml.common.Mod(JustGashLoot.MOD_ID)
public final class JustGashLoot {
    public static final String MOD_ID = "justgashloot";
    public JustGashLoot() {
        var bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        ModContent.register(bus); UiNetwork.register(bus); ClientInit.register(bus); Protection.register();
        if (Boolean.getBoolean("justgashloot.verification")) {
            try { Class.forName("net.gasharaujo.justgashloot.VerificationMod").getMethod("install").invoke(null); }
            catch (ReflectiveOperationException error) { throw new IllegalStateException("Verification sources are required for this development run", error); }
        }
    }
}
