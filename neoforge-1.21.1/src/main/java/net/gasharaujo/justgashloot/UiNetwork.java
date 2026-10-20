package net.gasharaujo.justgashloot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public final class UiNetwork {
    public static void register(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) -> event.registrar("2").playToClient(Payload.TYPE, Payload.CODEC,
                (payload, context) -> context.enqueueWork(() -> net.gasharaujo.justgashloot.client.ClientCounts.receive(payload.state()))));
    }
    public static void send(net.minecraft.server.level.ServerPlayer player, CountState state) { net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new Payload(state)); }
    public record Payload(CountState state) implements CustomPacketPayload {
        static final Type<Payload> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("justgashloot", "counts"));
        static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.of((buffer, payload) -> payload.state.write(buffer), buffer -> new Payload(CountState.read(buffer)));
        @Override public Type<Payload> type() { return TYPE; }
    }
}
