package net.gasharaujo.justgashloot;
import java.util.*;
public final class VoidChecks {
    private static int count;
    private static void check(boolean value) { if (!value) throw new AssertionError("Void rules check " + count); count++; }
    public static void main(String[] args) {
        check(VoidRules.normalize("diamond").equals("minecraft:diamond"));
        check(VoidRules.normalize(" #Minecraft:PLANKS ").equals("#minecraft:planks"));
        for (String bad : List.of("", "#", "minecraft:", ":stone", "a:b:c", "foo bar", "#invalid tag", "a".repeat(257))) check(VoidRules.normalize(bad).isEmpty());
        check(VoidRules.normalize(null).isEmpty());
        check(VoidRules.restore(Arrays.asList(null, "diamond", "minecraft:diamond", "#minecraft:planks", "invalid tag")).equals(List.of("minecraft:diamond", "#minecraft:planks")));
        var many = new ArrayList<String>(); for (int i = 0; i < 100; i++) many.add("minecraft:item_" + i);
        check(VoidRules.restore(many).size() == 64);
        check(VoidRules.restore(null).isEmpty());
        System.out.println("PASS: " + count + " void checks");
    }
}
