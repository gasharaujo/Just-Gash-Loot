package net.gasharaujo.justgashloot;

import net.minecraft.network.FriendlyByteBuf;

public record LibraryAction(int operation, String value, int offset) {
    public void write(FriendlyByteBuf buf) { buf.writeVarInt(operation); buf.writeUtf(value, 256); buf.writeVarInt(offset); }
    public static LibraryAction read(FriendlyByteBuf buf) { return new LibraryAction(buf.readVarInt(), buf.readUtf(256), buf.readVarInt()); }
}
