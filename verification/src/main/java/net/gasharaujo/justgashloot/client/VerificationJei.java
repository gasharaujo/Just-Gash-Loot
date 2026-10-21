package net.gasharaujo.justgashloot.client;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Exercises JEI's real recipe index and layouts, without adding JEI to release dependencies. */
public final class VerificationJei {
    private static int ticks, stage;
    private static Object runtime, category;
    private static List<Object> recipes;
    private static final List<String> checks = new ArrayList<>();
    private static Object call(Object object, String name, Object... args) throws ReflectiveOperationException {
        for (Method method : object.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != args.length) continue;
            boolean matches = true;
            for (int i = 0; i < args.length; i++) if (!method.getParameterTypes()[i].isInstance(args[i])) matches = false;
            if (!matches) continue;
            method.setAccessible(true);
            return method.invoke(object, args);
        }
        throw new NoSuchMethodException(name);
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks.add(message); System.out.println("JGL_JEI_CHECK_PASS " + message);
    }
    private static String id(Object recipe) throws ReflectiveOperationException {
        try { return call(recipe, "id").toString(); }
        catch (NoSuchMethodException ignored) { return call(recipe, "getId").toString(); }
    }
    private static void show(String categoryName) throws ReflectiveOperationException {
        var shown = new ArrayList<Object>();
        for (Object recipe : recipes) if (id(recipe).contains("justgashloot:" + categoryName + "_upgrade_1")) shown.add(recipe);
        call(call(runtime, "getRecipesGui"), "showRecipes", category, shown, List.of());
    }
    public static void tick(Minecraft mc) {
        try {
            ticks++;
            if (stage == 0) {
                var optional = (Optional<?>) Class.forName("mezz.jei.common.Internal").getMethod("getOptionalJeiRuntime").invoke(null);
                if (optional.isEmpty()) { if (ticks > 600) throw new AssertionError("JEI did not start"); return; }
                runtime = optional.get();
                Object type = Class.forName("mezz.jei.api.constants.RecipeTypes").getField("CRAFTING").get(null);
                Object manager = call(runtime, "getRecipeManager");
                category = call(manager, "getRecipeCategory", type);
                Object lookup = call(manager, "createRecipeLookup", type);
                try (var stream = (Stream<?>) call(lookup, "get")) { recipes = new ArrayList<>(stream.toList()); }
                Object focus = call(call(call(runtime, "getJeiHelpers"), "getFocusFactory"), "getEmptyFocusGroup");
                int count = 0;
                for (Object recipe : recipes) {
                    String id = id(recipe);
                    if (!id.contains("justgashloot:quantity_upgrade_") && !id.contains("justgashloot:quality_upgrade_")) continue;
                    Object layout = ((Optional<?>) call(manager, "createRecipeLayoutDrawable", category, recipe, focus)).orElseThrow();
                    var slots = (List<?>) call(call(layout, "getRecipeSlotsView"), "getSlotViews");
                    int inputs = 0, books = 0;
                    for (Object slot : slots) {
                        if (!call(slot, "getRole").toString().equals("INPUT")) continue;
                        List<?> stacks;
                        try (var values = (Stream<?>) call(slot, "getItemStacks")) { stacks = values.toList(); }
                        check(!stacks.isEmpty(), "Visible input slot " + inputs + " for " + id);
                        if (stacks.stream().anyMatch(value -> ((ItemStack) value).is(Items.ENCHANTED_BOOK))) books++;
                        inputs++;
                    }
                    check(inputs == 9 && books == 4, "JEI exposes all nine inputs and four enchanted books for " + id);
                    count++;
                }
                check(count == 10, "All ten Quantity and Quality recipes are visible in JEI");
                show("quantity"); stage = 1; ticks = 0;
            } else if (ticks > 40 && stage == 1) {
                check(mc.screen != null, "JEI Quantity recipe screen opens");
                capture(mc, "jgl-jei-quantity.png");
                show("quality"); stage = 2; ticks = 0;
            } else if (ticks > 40 && stage == 2) {
                check(mc.screen != null, "JEI Quality recipe screen opens");
                capture(mc, "jgl-jei-quality.png");
                finish(mc, null); stage = 3;
            }
        } catch (Throwable error) { error.printStackTrace(); finish(mc, error); stage = 3; }
    }
    private static void capture(Minecraft mc, String name) throws ReflectiveOperationException {
        for (Method method : Screenshot.class.getMethods()) {
            if (!method.getName().equals("grab")) continue;
            java.util.function.Consumer<Object> callback = value -> {};
            if (method.getParameterCount() == 4) { method.invoke(null, mc.gameDirectory, name, mc.getMainRenderTarget(), callback); return; }
            if (method.getParameterCount() == 5) { method.invoke(null, mc.gameDirectory, name, mc.getMainRenderTarget(), 1, callback); return; }
        }
        throw new NoSuchMethodException("Screenshot.grab");
    }
    private static void finish(Minecraft mc, Throwable error) {
        var report = new com.google.gson.JsonObject(); report.addProperty("passed", error == null);
        report.add("checks", new com.google.gson.Gson().toJsonTree(checks));
        if (error != null) report.addProperty("failure", error.toString());
        try { java.nio.file.Files.writeString(java.nio.file.Path.of("jei-results.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n"); }
        catch (java.io.IOException failure) { throw new RuntimeException(failure); }
        System.out.println("JGL_JEI_VERIFICATION " + (error == null ? "PASS" : "FAIL") + " " + checks.size());
        mc.stop();
    }
}
