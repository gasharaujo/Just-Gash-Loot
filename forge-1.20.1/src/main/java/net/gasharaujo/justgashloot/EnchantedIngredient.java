package net.gasharaujo.justgashloot;
import java.util.stream.Stream;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.common.crafting.*;
import com.google.gson.*;
import net.minecraft.network.FriendlyByteBuf;
public final class EnchantedIngredient extends AbstractIngredient {
    final Enchantment enchantment;
    EnchantedIngredient(Enchantment enchantment) {
        super(Stream.of(new Ingredient.ItemValue(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, 3)))));
        if (enchantment != Enchantments.BLOCK_FORTUNE && enchantment != Enchantments.MOB_LOOTING) throw new IllegalArgumentException("Unsupported recipe enchantment");
        this.enchantment = enchantment;
    }
    @Override public boolean test(ItemStack stack) { return stack != null && !stack.isEmpty() && EnchantmentHelper.getEnchantments(stack).getOrDefault(enchantment, 0) == 3; }
    @Override public boolean isSimple() { return false; }
    @Override public ItemStack[] getItems() { return new ItemStack[] {EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, 3))}; }
    @Override public IIngredientSerializer<? extends Ingredient> getSerializer() { return SERIALIZER; }
    @Override public JsonElement toJson() {
        JsonObject json = new JsonObject(); json.addProperty("type", "justgashloot:exact_enchantment");
        json.addProperty("enchantment", net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.getKey(enchantment).toString()); return json;
    }
    private static final IIngredientSerializer<EnchantedIngredient> SERIALIZER = new IIngredientSerializer<>() {
        public EnchantedIngredient parse(JsonObject json) { return new EnchantedIngredient(net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.get(new net.minecraft.resources.ResourceLocation(json.get("enchantment").getAsString()))); }
        public EnchantedIngredient parse(FriendlyByteBuf buffer) { return new EnchantedIngredient(net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.get(buffer.readResourceLocation())); }
        public void write(FriendlyByteBuf buffer, EnchantedIngredient value) { buffer.writeResourceLocation(net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.getKey(value.enchantment)); }
    };
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> CraftingHelper.register(new net.minecraft.resources.ResourceLocation("justgashloot", "exact_enchantment"), SERIALIZER)));
    }
}
