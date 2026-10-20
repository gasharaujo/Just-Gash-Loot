package net.gasharaujo.justgashloot.client;

import net.gasharaujo.justgashloot.*;
import net.minecraft.client.Minecraft;

public final class TabletClient {
    private TabletClient() {}
    public static void receive(LibraryState state) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        TabletSelection.active = state.active();
        if (client.screen instanceof TabletScreen screen) screen.accept(state);
        else if (state.open()) client.setScreen(new TabletScreen(state));
    }
}
