package net.gasharaujo.justgashloot.client;
import net.gasharaujo.justgashloot.*;
import net.minecraft.client.Minecraft;
public final class FilterClient {
    public static void receive(FilterState state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.containerMenu instanceof LootMenu menu) || menu.containerId != state.menu()) return;
        if (mc.screen instanceof FilterScreen screen) screen.accept(state);
        else if (state.open() && mc.screen instanceof LootScreen screen) mc.setScreen(new FilterScreen(screen, state));
    }
}
