package net.gasharaujo.justgashloot;

/** Client-owned tooltip cache; deliberately contains no client-only Minecraft types. */
public final class TabletSelection {
    private TabletSelection() {}
    public static volatile String active = "";
    public static net.minecraft.network.chat.Component label() {
        return active.isEmpty() ? net.minecraft.network.chat.Component.translatable("tooltip.justgashloot.none")
                : net.minecraft.network.chat.Component.literal(active);
    }
}
