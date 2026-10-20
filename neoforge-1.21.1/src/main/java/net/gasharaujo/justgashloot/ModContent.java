package net.gasharaujo.justgashloot;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "justgashloot");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "justgashloot");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "justgashloot");
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "justgashloot");
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "justgashloot");
    public static final Supplier<LootBlock> BLOCK = BLOCKS.register("loot_generator", () -> new LootBlock(BlockBehaviour.Properties.of().strength(3.0F, 6.0F)));
    public static final Supplier<BlockEntityType<LootEntity>> ENTITY = ENTITIES.register("loot_generator", () -> BlockEntityType.Builder.of(LootEntity::new, BLOCK.get()).build(null));
    public static final Supplier<MenuType<LootMenu>> MENU = MENUS.register("loot_generator", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id, inventory, data) -> new LootMenu(id, inventory)));
    public static final List<Supplier<? extends Item>> ALL_ITEMS = new ArrayList<>();
    static {
        item("loot_generator", () -> new BlockItem(BLOCK.get(), props("loot_generator")));
        item("upgrade_base", () -> new Item(props("upgrade_base")));
        item("loot_tablet", () -> new Item(props("loot_tablet").stacksTo(1)));
        for (UpgradeItem.Category category : UpgradeItem.Category.values()) {
            int tiers = category == UpgradeItem.Category.PLAYER ? 1 : 5;
            for (int i = 1; i <= tiers; i++) {
                final int tier = category == UpgradeItem.Category.PLAYER ? 0 : i;
                String name = category.name().toLowerCase(Locale.ROOT) + "_upgrade" + (tier == 0 ? "" : "_" + tier);
                item(name, () -> new UpgradeItem(props(name), category, tier));
            }
        }
        TABS.register("main", () -> CreativeModeTab.builder().title(net.minecraft.network.chat.Component.translatable("itemGroup.justgashloot"))
                .icon(() -> new ItemStack(BLOCK.get())).displayItems((parameters, output) -> ALL_ITEMS.forEach(item -> output.accept(item.get()))).build());
    }
    private static Item.Properties props(String name) { return new Item.Properties(); }
    private static void item(String name, Supplier<? extends Item> factory) { ALL_ITEMS.add(ITEMS.register(name, factory)); }
    static void register(net.neoforged.bus.api.IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus); TABS.register(bus);
        bus.addListener(PlatformCapabilities::register);
    }
}
