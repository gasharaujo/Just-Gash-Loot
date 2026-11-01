package net.gasharaujo.justgashloot;
import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
public record FilterState(int menu, boolean open, List<String> rules, String query, List<String> suggestions, String error) {
    public void write(FriendlyByteBuf out) {
        out.writeVarInt(menu); out.writeBoolean(open); strings(out, rules); out.writeUtf(query, VoidRules.LENGTH); strings(out, suggestions); out.writeUtf(error, 64);
    }
    public static FilterState read(FriendlyByteBuf in) {
        int menu = in.readVarInt(); boolean open = in.readBoolean(); List<String> rules = strings(in, VoidRules.LIMIT);
        String query = in.readUtf(VoidRules.LENGTH); List<String> suggestions = strings(in, 8);
        return new FilterState(menu, open, rules, query, suggestions, in.readUtf(64));
    }
    private static void strings(FriendlyByteBuf out, List<String> values) { out.writeVarInt(values.size()); for (String value : values) out.writeUtf(value, VoidRules.LENGTH); }
    private static List<String> strings(FriendlyByteBuf in, int limit) {
        int size = in.readVarInt(); if (size < 0 || size > limit) throw new IllegalArgumentException("Invalid filter entries");
        var result = new ArrayList<String>(size); for (int i = 0; i < size; i++) result.add(in.readUtf(VoidRules.LENGTH)); return List.copyOf(result);
    }
}
