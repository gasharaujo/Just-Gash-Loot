package net.gasharaujo.justgashloot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public final class TabletNetwork {
    public static void register(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) -> {
            var registrar = event.registrar("2");
            registrar.playToClient(State.TYPE, State.CODEC, (payload, context) -> context.enqueueWork(() -> net.gasharaujo.justgashloot.client.TabletClient.receive(payload.value())));
            registrar.playToServer(Action.TYPE, Action.CODEC, (payload, context) -> context.enqueueWork(() -> {
                if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) Tablets.action(player, payload.value());
            }));
        });
    }
    public static void send(net.minecraft.server.level.ServerPlayer player, LibraryState state) { net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new State(state)); }
    public static void request(LibraryAction action) { net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new Action(action)); }
    public record State(LibraryState value) implements CustomPacketPayload {
        static final Type<State> TYPE = new Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("justgashloot", "tablet_state"));
        static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of((buffer, payload) -> payload.value.write(buffer), buffer -> new State(LibraryState.read(buffer)));
        @Override public Type<State> type() { return TYPE; }
    }
    public record Action(LibraryAction value) implements CustomPacketPayload {
        static final Type<Action> TYPE = new Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("justgashloot", "tablet_action"));
        static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.of((buffer, payload) -> payload.value.write(buffer), buffer -> new Action(LibraryAction.read(buffer)));
        @Override public Type<Action> type() { return TYPE; }
    }
}
