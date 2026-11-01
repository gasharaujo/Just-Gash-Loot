package net.gasharaujo.justgashloot;
import net.minecraft.network.FriendlyByteBuf;
public record FilterAction(int menu, int operation, String value, String query) {
    public void write(FriendlyByteBuf out) { out.writeVarInt(menu); out.writeVarInt(operation); out.writeUtf(value, VoidRules.LENGTH); out.writeUtf(query, VoidRules.LENGTH); }
    public static FilterAction read(FriendlyByteBuf in) { return new FilterAction(in.readVarInt(), in.readVarInt(), in.readUtf(VoidRules.LENGTH), in.readUtf(VoidRules.LENGTH)); }
}
