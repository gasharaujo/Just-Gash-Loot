package net.gasharaujo.justgashloot;
import net.minecraft.core.Holder;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.common.crafting.*;
import net.neoforged.neoforge.registries.*;
public record EnchantedIngredient(Holder<Enchantment> enchantment) implements ICustomIngredient {
    public EnchantedIngredient {
        if (!enchantment.is(Enchantments.FORTUNE) && !enchantment.is(Enchantments.LOOTING)) throw new IllegalArgumentException("Unsupported recipe enchantment");
    }
    private static final com.mojang.serialization.MapCodec<EnchantedIngredient> CODEC = Enchantment.CODEC.fieldOf("enchantment").xmap(EnchantedIngredient::new, EnchantedIngredient::enchantment);
    private static final DeferredRegister<IngredientType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, "justgashloot");
    private static final java.util.function.Supplier<IngredientType<EnchantedIngredient>> TYPE = TYPES.register("exact_enchantment", () -> new IngredientType<>(CODEC));
    @Override public boolean test(ItemStack stack) {
        int applied = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(enchantment);
        return !stack.isEmpty() && (applied == 0 ? stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(enchantment) : applied) == 3;
    }
    private ItemStack example() {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable data = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY); data.set(enchantment, 3);
        stack.set(DataComponents.STORED_ENCHANTMENTS, data.toImmutable()); return stack;
    }
    @Override public java.util.stream.Stream<ItemStack> getItems() { return java.util.stream.Stream.of(example()); }
    @Override public boolean isSimple() { return false; }
    @Override public IngredientType<?> getType() { return TYPE.get(); }
    public static void register(net.neoforged.bus.api.IEventBus bus) { TYPES.register(bus); }
}
