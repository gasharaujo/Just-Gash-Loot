package net.gasharaujo.justgashloot;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
public final class TabletNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new net.minecraft.resources.ResourceLocation("justgashloot", "tablet"), () -> "2", "2"::equals, "2"::equals);
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        CHANNEL.messageBuilder(LibraryState.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(LibraryState::write).decoder(LibraryState::read)
                .consumerMainThread((state, context) -> net.gasharaujo.justgashloot.client.TabletClient.receive(state)).add();
        CHANNEL.messageBuilder(LibraryAction.class, 1, NetworkDirection.PLAY_TO_SERVER).encoder(LibraryAction::write).decoder(LibraryAction::read)
                .consumerMainThread((action, context) -> { var player = context.get().getSender(); if (player != null) Tablets.action(player, action); }).add();
    }
    public static void send(net.minecraft.server.level.ServerPlayer player, LibraryState state) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), state); }
    public static void request(LibraryAction action) { CHANNEL.sendToServer(action); }
}
