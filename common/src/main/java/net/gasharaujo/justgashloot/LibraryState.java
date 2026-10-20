package net.gasharaujo.justgashloot;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

public record LibraryState(boolean open, String active, String query, int offset, int total, List<String> tables) {
    public static final int PAGE_SIZE = 32;
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(open); buf.writeUtf(active, 256); buf.writeUtf(query, 128);
        buf.writeVarInt(offset); buf.writeVarInt(total); buf.writeVarInt(tables.size());
        for (String id : tables) buf.writeUtf(id, 256);
    }
    public static LibraryState read(FriendlyByteBuf buf) {
        boolean open = buf.readBoolean(); String active = buf.readUtf(256), query = buf.readUtf(128);
        int offset = buf.readVarInt(), total = buf.readVarInt(), count = buf.readVarInt();
        if (offset < 0 || total < 0 || count < 0 || count > PAGE_SIZE) throw new IllegalArgumentException("Invalid library page");
        var entries = new java.util.ArrayList<String>();
        for (int i = 0; i < count; i++) entries.add(buf.readUtf(256));
        return new LibraryState(open, active, query, offset, total, List.copyOf(entries));
    }
}
