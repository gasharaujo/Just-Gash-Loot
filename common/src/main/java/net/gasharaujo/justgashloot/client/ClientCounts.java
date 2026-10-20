package net.gasharaujo.justgashloot.client;

import net.gasharaujo.justgashloot.CountState;
import net.gasharaujo.justgashloot.LootMenu;
import net.minecraft.client.Minecraft;

public final class ClientCounts {
    private ClientCounts() {}
    public static void receive(CountState state) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof LootMenu menu && menu.containerId == state.menuId()) {
            System.arraycopy(state.counts(), 0, menu.counts, 0, 36);
        }
    }
}
