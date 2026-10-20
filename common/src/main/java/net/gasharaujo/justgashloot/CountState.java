package net.gasharaujo.justgashloot;

import net.minecraft.network.FriendlyByteBuf;

public record CountState(int menuId, long[] counts) {
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(menuId);
        for (int i = 0; i < 36; i++) buffer.writeVarLong(counts[i]);
    }
    public static CountState read(FriendlyByteBuf buffer) {
        int id = buffer.readVarInt();
        long[] counts = new long[36];
        for (int i = 0; i < 36; i++) counts[i] = Math.max(0, buffer.readVarLong());
        return new CountState(id, counts);
    }
}
