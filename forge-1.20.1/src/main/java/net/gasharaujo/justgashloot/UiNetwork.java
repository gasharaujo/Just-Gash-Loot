package net.gasharaujo.justgashloot;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
public final class UiNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new net.minecraft.resources.ResourceLocation("justgashloot", "counts"), () -> "1", "1"::equals, "1"::equals);
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        CHANNEL.messageBuilder(CountState.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(CountState::write).decoder(CountState::read)
                .consumerMainThread((state, context) -> net.gasharaujo.justgashloot.client.ClientCounts.receive(state)).add();
    }
    public static void send(net.minecraft.server.level.ServerPlayer player, CountState state) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), state); }
}
