package net.gasharaujo.justgashloot;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
final class VerificationV2Platform {
    private static void check(boolean value, String text) { VerificationChecks.check(value, text); }
    private static Item item(String id) { return BuiltInRegistries.ITEM.get(LootRuntime.id(id)); }
    private static ItemStack enchanted(ServerLevel level, boolean fortune, boolean book, int tier) {
        var enchantment = fortune ? net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE : net.minecraft.world.item.enchantment.Enchantments.MOB_LOOTING;
        ItemStack stack = new ItemStack(book ? Items.ENCHANTED_BOOK : fortune ? Items.DIAMOND_PICKAXE : Items.DIAMOND_SWORD);
        if (book) {
            stack = net.minecraft.world.item.EnchantedBookItem.createForEnchantment(new net.minecraft.world.item.enchantment.EnchantmentInstance(enchantment, tier));
        } else { stack.enchant(enchantment, tier); stack.setDamageValue(20); }
        return stack;
    }
    private static net.minecraft.world.inventory.CraftingContainer input(ServerPlayer player, List<ItemStack> stacks) { var menu = new net.minecraft.world.inventory.CraftingMenu(101, player.getInventory());
        for (int i = 0; i < 9; i++) menu.getSlot(i + 1).set(stacks.get(i).copy());
        return menu.getSlot(1).container instanceof net.minecraft.world.inventory.CraftingContainer container ? container : null; }
    private static ShapedRecipe recipe(ServerLevel level, String name) {
        var value = level.getServer().getRecipeManager().byKey(LootRuntime.id("justgashloot:" + name)).orElseThrow();
        return (ShapedRecipe) value;
    }
    private static List<ItemStack> ingredients(ServerLevel level, String name) {
        String path = "/data/justgashloot/recipes/" + name + ".json";
        try (var stream = VerificationV2Platform.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException(path);
            var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var key = json.getAsJsonObject("key"); var stacks = new ArrayList<ItemStack>();
            for (var row : json.getAsJsonArray("pattern")) for (char c : row.getAsString().toCharArray()) {
                var entry = key.get(String.valueOf(c));
                if (entry.isJsonPrimitive()) stacks.add(new ItemStack(item(entry.getAsString())));
                else if (entry.getAsJsonObject().has("item")) stacks.add(new ItemStack(item(entry.getAsJsonObject().get("item").getAsString())));
                else stacks.add(enchanted(level, entry.getAsJsonObject().get("enchantment").getAsString().endsWith("fortune"), true, 3));
            }
            return stacks;
        } catch (java.io.IOException error) { throw new RuntimeException(error); }
    }
    static void recipes(ServerLevel level, ServerPlayer player) {
        List<String> names = new ArrayList<>(List.of("loot_generator", "loot_tablet", "upgrade_base", "player_upgrade"));
        for (String category : List.of("energy", "speed", "quantity", "quality")) for (int tier = 1; tier <= 5; tier++) names.add(category + "_upgrade_" + tier);
        for (String name : names) {
            var recipe = recipe(level, name); var stacks = ingredients(level, name); var input = input(player, stacks);
            check(recipe.matches(input, level) && recipe.assemble(input, level.registryAccess()).is(item("justgashloot:" + name)), "Registered crafting recipe matches and produces " + name);
            player.getInventory().clearContent();
            var menu = new net.minecraft.world.inventory.CraftingMenu(102, player.getInventory(), net.minecraft.world.inventory.ContainerLevelAccess.create(level, new BlockPos(12, 80, 0))); player.containerMenu = menu;
            for (int i = 0; i < 9; i++) menu.getSlot(i + 1).set(stacks.get(i).copy());
            menu.quickMoveStack(player, 0);
            check(java.util.stream.IntStream.range(1, 10).allMatch(i -> menu.getSlot(i).getItem().isEmpty()) && player.getInventory().contains(new ItemStack(item("justgashloot:" + name))), "Vanilla crafting consumes every ingredient once for " + name);
        }
        for (String category : List.of("quantity", "quality")) {
            boolean fortune = category.equals("quantity"); var stacks = ingredients(level, category + "_upgrade_1"); var recipe = recipe(level, category + "_upgrade_1");
            for (int tier : new int[]{2, 4}) { stacks.set(0, enchanted(level, fortune, true, tier)); check(!recipe.matches(input(player, stacks), level), category + " rejects enchanted book level " + tier); }
            stacks.set(0, enchanted(level, fortune, false, 3)); check(recipe.matches(input(player, stacks), level), category + " accepts a damaged enchanted tool mixed with books");
        }
        player.containerMenu = player.inventoryMenu;
    }
    private static boolean provider(Object object) {
        for (String name : List.of("getDataLootTable", "getInfoLootTable", "getTable")) try { object.getClass().getMethod(name); return true; } catch (NoSuchMethodException ignored) {}
        return false;
    }
    private static boolean fixed(Object object) {
        for (String name : List.of("isDataReferenceInventory", "isInfoReferenceInventory")) try { if (Boolean.TRUE.equals(object.getClass().getMethod(name).invoke(object))) return true; } catch (ReflectiveOperationException ignored) {}
        return false;
    }
    static LootEntity oldMachine(LootEntity machine) {
        var tag = machine.saveWithFullMetadata(); tag.remove("Generation");
        return (LootEntity) BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), tag);
    }
    static void installCaptureTarget(ServerLevel level, BlockPos pos) {
        Block selected = Blocks.CHEST;
        for (Block block : BuiltInRegistries.BLOCK) {
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            if (id.equals("lootr:chest") || id.equals("lootr:lootr_chest")) selected = block;
        }
        var state = selected.defaultBlockState(); level.setBlockAndUpdate(pos, state);
        var fixture = level.getBlockEntity(pos);
        var tag = fixture.saveWithFullMetadata(); tag.putString("LootTable", VerificationV2Checks.BASIC);
        var loaded = BlockEntity.loadStatic(pos, state, tag); level.setBlockEntity(loaded);
    }
    static void capture(ServerLevel level) {
        BlockPos pos = new BlockPos(12, 80, 0);
        var state = Blocks.CHEST.defaultBlockState(); BlockEntity fixture = new ChestBlockEntity(pos, state); fixture.setLevel(level);
        var tag = fixture.saveWithFullMetadata(); tag.putString("LootTable", VerificationV2Checks.BASIC);
        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, state, tag); loaded.setLevel(level);
        check(VerificationV2Checks.BASIC.equals(LootRuntime.capture(loaded)), "Vanilla chest table capture does not unpack its contents");
        ((ChestBlockEntity) loaded).getItem(0);
        if (!net.minecraftforge.fml.ModList.get().isLoaded("lootr")) check(LootRuntime.capture(loaded) == null, "Already generated vanilla container has no table to capture");
        if (!net.minecraftforge.fml.ModList.get().isLoaded("lootr")) return;
        int blocks = 0, entities = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            if (!id.startsWith("lootr:") || !(block instanceof EntityBlock entityBlock)) continue;
            state = block.defaultBlockState(); fixture = entityBlock.newBlockEntity(pos, state);
            if (fixture == null || !provider(fixture)) continue;
            fixture.setLevel(level); tag = fixture.saveWithFullMetadata(); tag.putString("LootTable", VerificationV2Checks.BASIC);
            loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, state, tag); loaded.setLevel(level);
            String captured = LootRuntime.capture(loaded);
            check(fixed(loaded) ? captured == null : VerificationV2Checks.BASIC.equals(captured), "Lootr block provider captures only a real table: " + id); blocks++;
        }
        for (var type : BuiltInRegistries.ENTITY_TYPE) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(); if (!id.startsWith("lootr:")) continue;
            var entity = type.create(level);
            if (entity == null || !provider(entity) && !(entity instanceof net.minecraft.world.entity.vehicle.ContainerEntity)) continue;
            if (entity instanceof net.minecraft.world.entity.vehicle.ContainerEntity container) container.setLootTable(LootRuntime.id(VerificationV2Checks.BASIC));
            String captured = LootRuntime.capture(entity);
            check(fixed(entity) ? captured == null : VerificationV2Checks.BASIC.equals(captured), "Lootr entity provider respects table or fixed content: " + id); entities++;
        }
        check(blocks > 0 && entities > 0, "Installed Lootr block and entity provider families were exercised");
    }
}
