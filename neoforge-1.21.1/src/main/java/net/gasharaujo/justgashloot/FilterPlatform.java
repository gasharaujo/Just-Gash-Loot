package net.gasharaujo.justgashloot;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
public final class FilterPlatform {
    private FilterPlatform() {}
    public static boolean matches(ItemStack stack, List<String> rules) {
        String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (String rule : rules) {
            if (rule.startsWith("#")) { if (stack.is(TagKey.create(Registries.ITEM, LootRuntime.id(rule.substring(1))))) return true; }
            else if (rule.equals(item)) return true;
        }
        return false;
    }
    public static List<String> suggestions(String query) {
        String text = query.strip().toLowerCase(Locale.ROOT);
        java.util.stream.Stream<String> values = text.startsWith("#") ? net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagNames().map(tag -> "#" + tag.location())
                : BuiltInRegistries.ITEM.keySet().stream().map(Object::toString);
        String match = text.startsWith("#") ? text.substring(1) : text;
        return values.filter(value -> value.contains(match)).sorted().limit(8).toList();
    }
    public static boolean exists(String rule) {
        return rule.startsWith("#") ? net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagNames().anyMatch(tag -> ("#" + tag.location()).equals(rule))
                : BuiltInRegistries.ITEM.containsKey(LootRuntime.id(rule));
    }
}
