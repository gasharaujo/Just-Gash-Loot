package net.gasharaujo.justgashloot;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
public final class FilterNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new net.minecraft.resources.ResourceLocation("justgashloot", "void_filter"), () -> "1", "1"::equals, "1"::equals);
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        CHANNEL.messageBuilder(FilterState.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(FilterState::write).decoder(FilterState::read)
            .consumerMainThread((state, context) -> net.gasharaujo.justgashloot.client.FilterClient.receive(state)).add();
        CHANNEL.messageBuilder(FilterAction.class, 1, NetworkDirection.PLAY_TO_SERVER).encoder(FilterAction::write).decoder(FilterAction::read)
            .consumerMainThread((action, context) -> { var player = context.get().getSender(); if (player != null) FilterActions.handle(player, action); }).add();
    }
    public static void send(net.minecraft.server.level.ServerPlayer player, FilterState state) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), state); }
    public static void request(FilterAction action) { CHANNEL.sendToServer(action); }
}
