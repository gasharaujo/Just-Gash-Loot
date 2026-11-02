package net.gasharaujo.justgashloot.client;
import java.util.*;
import net.gasharaujo.justgashloot.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
public final class VerificationVoidClient {
    private static int stage, ticks, wait;
    private static Object field(Object object, String name) throws Exception { var field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object); }
    private static FilterState state(FilterScreen screen) throws Exception { return (FilterState) field(screen, "state"); }
    private static EditBox input(FilterScreen screen) throws Exception { return (EditBox) field(screen, "input"); }
    private static void press(Button button) throws Exception {
        for (var method : button.getClass().getMethods()) if (method.getName().equals("onPress")) {
            if (method.getParameterCount() == 0) { method.invoke(button); return; }
            if (method.getParameterCount() == 1) { method.invoke(button, Class.forName("net.minecraft.client.input.MouseButtonInfo").getConstructor(int.class, int.class).newInstance(0, 0)); return; }
        }
        throw new AssertionError("No button callback");
    }
    private static void add(FilterScreen screen) throws Exception {
        for (var child : screen.children()) if (child instanceof Button button && button.getMessage().getString().equals(Component.translatable("gui.justgashloot.void.add").getString())) { press(button); return; }
        throw new AssertionError("No Add button");
    }
    private static void resize(Minecraft mc, int scale) throws Exception {
        mc.options.guiScale().set(scale);
        try { mc.getClass().getMethod("resizeGui").invoke(mc); } catch (NoSuchMethodException ignored) { mc.getClass().getMethod("resizeDisplay").invoke(mc); }
    }
    private static void capture(Minecraft mc, String name) throws Exception {
        var type = net.minecraft.client.Screenshot.class;
        for (var method : type.getMethods()) if (method.getName().equals("grab")) {
            java.util.function.Consumer<Object> callback = value -> {};
            if (method.getParameterCount() == 4) { method.invoke(null, mc.gameDirectory, name, mc.getMainRenderTarget(), callback); return; }
            if (method.getParameterCount() == 5) { method.invoke(null, mc.gameDirectory, name, mc.getMainRenderTarget(), 1, callback); return; }
        }
    }
    public static void tick(Minecraft mc) {
        try {
            if (++ticks > 1800) throw new AssertionError("Void UI timeout at stage " + stage);
            if (++wait < 15) return;
            if (stage == 0) { java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("void-server-results.json")); mc.player.connection.sendCommand("jgluitest"); stage++; wait = 0; }
            else if (stage == 1 && mc.screen instanceof LootScreen screen) {
                capture(mc, "jgl-void-button.png");
                for (var child : screen.children()) if (child instanceof PlainTextButton button && button.getMessage().getString().equals("Void")) { press(button); stage++; wait = 0; break; }
            } else if (stage == 2 && mc.screen instanceof FilterScreen screen) { input(screen).setValue("minecraft:dia"); stage++; wait = 0; }
            else if (stage == 3 && mc.screen instanceof FilterScreen screen && state(screen).suggestions().contains("minecraft:diamond")) {
                press(((Button[]) field(screen, "suggestions"))[0]);
                if (!input(screen).getValue().equals("minecraft:diamond")) throw new AssertionError("Autocomplete did not complete the item ID");
                add(screen); stage++; wait = 0;
            } else if (stage == 4 && mc.screen instanceof FilterScreen screen && state(screen).rules().contains("minecraft:diamond")) { input(screen).setValue("minecraft:not_real_jgl"); add(screen); stage++; wait = 0; }
            else if (stage == 5 && mc.screen instanceof FilterScreen screen && state(screen).error().equals("unknown")) {
                if (state(screen).rules().size() != 1) throw new AssertionError("Server accepted an unknown item");
                input(screen).setValue("#minecraft:planks"); stage++; wait = 0;
            } else if (stage == 6 && mc.screen instanceof FilterScreen screen && state(screen).suggestions().contains("#minecraft:planks")) { press(((Button[]) field(screen, "suggestions"))[0]); add(screen); stage++; wait = 0; }
            else if (stage == 7 && mc.screen instanceof FilterScreen screen && state(screen).rules().size() == 2) { press(((Button[]) field(screen, "rules"))[1]); stage++; wait = 0; }
            else if (stage == 8 && mc.screen instanceof FilterScreen screen && state(screen).rules().size() == 1) { input(screen).setValue("#minecraft:planks"); add(screen); stage++; wait = 0; }
            else if (stage == 9 && mc.screen instanceof FilterScreen screen && state(screen).rules().size() == 2) { resize(mc, 2); stage++; wait = 0; }
            else if (stage == 10) { capture(mc, "jgl-void-scale-2.png"); resize(mc, 3); stage++; wait = 0; }
            else if (stage == 11 && mc.screen instanceof FilterScreen screen) {
                capture(mc, "jgl-void-scale-3.png");
                FilterNetwork.request(new FilterAction(state(screen).menu() + 1, 3, "minecraft:diamond", ""));
                mc.player.connection.sendCommand("jglvoidassert"); stage++; wait = 0;
            } else if (stage == 12 && mc.screen instanceof FilterScreen screen && java.nio.file.Files.exists(java.nio.file.Path.of("void-server-results.json"))) {
                var report = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(java.nio.file.Path.of("void-server-results.json"))).getAsJsonObject();
                if (!report.get("passed").getAsBoolean()) throw new AssertionError("Server rules or native persistence did not match");
                screen.onClose(); stage++; wait = 0;
            } else if (stage == 13) {
                if (!(mc.screen instanceof LootScreen) || !(mc.player.containerMenu instanceof LootMenu)) throw new AssertionError("Returning from the filter lost the machine menu");
                finish(mc, null); stage++;
            }
        } catch (Throwable error) { error.printStackTrace(); finish(mc, error); stage = 99; }
    }
    private static void finish(Minecraft mc, Throwable error) {
        var result = new com.google.gson.JsonObject(); result.addProperty("passed", error == null); result.addProperty("stage", stage);
        if (error != null) result.addProperty("failure", error.toString());
        else for (String check : List.of("voidTextButton", "itemAutocomplete", "tagAutocomplete", "serverValidation", "addRemoveRules", "wrongMenuRejected", "nativePersistence", "scale2", "scale3", "returnToMachine")) result.addProperty(check, true);
        try { java.nio.file.Files.writeString(java.nio.file.Path.of("void-ui-results.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result) + "\n"); }
        catch (java.io.IOException failure) { throw new RuntimeException(failure); }
        System.out.println("JGL_VOID_UI " + (error == null ? "PASS" : "FAIL") + " stage=" + stage); mc.stop();
    }
}
