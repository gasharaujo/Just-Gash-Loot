package net.gasharaujo.justgashloot.client;
import net.gasharaujo.justgashloot.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.inventory.ContainerInput;
public final class VerificationClient {
    private static int stage, ticks, wait;
    private static boolean rightClick, leftClick, tabletOpen, pagination, search, selection, tableCapture;
    public static void install() { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event) -> tick()); }
    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (++ticks > 5000) { System.out.println("JGL_UI_FAIL timeout"); mc.stop(); return; }
        if (mc.player == null || mc.level == null) return;
        mc.getToastManager().clear();
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        if (++wait < 30) return;
        if (stage == 0) {
            mc.options.guiScale().set(2); mc.resizeGui(); mc.player.connection.sendCommand("jgluitest"); stage = 1; wait = 0;
        } else if (stage == 1 && mc.player.containerMenu instanceof LootMenu menu && menu.counts[2] == Long.MAX_VALUE) {
            capture(mc, "jgl-gui-scale-2.png"); stage = 2; wait = 0;
        } else if (stage == 2) {
            mc.options.guiScale().set(3); mc.resizeGui(); stage = 3; wait = 0;
        } else if (stage == 3 && mc.player.containerMenu instanceof LootMenu menu) {
            capture(mc, "jgl-gui-scale-3.png");
            mc.gameMode.handleContainerInput(menu.containerId, 0, 1, ContainerInput.PICKUP, mc.player); stage = 4; wait = 0;
        } else if (stage == 4 && mc.player.containerMenu instanceof LootMenu menu) {
            rightClick = menu.counts[0] == 123455 && menu.getCarried().getCount() == 1;
            mc.gameMode.handleContainerInput(menu.containerId, 0, 0, ContainerInput.PICKUP, mc.player); stage = 5; wait = 0;
        } else if (stage == 5 && mc.player.containerMenu instanceof LootMenu menu) {
            leftClick = menu.counts[0] == 123392 && menu.getCarried().getCount() == 64;
            mc.gameMode.handleContainerInput(menu.containerId, 41, 0, ContainerInput.PICKUP, mc.player); stage = 6; wait = 0;
        } else if (stage == 6) {
            mc.player.closeContainer(); mc.player.setYRot(180); mc.player.setXRot(20.5F); stage = 7; wait = 0;
        } else if (stage == 7 && wait > 80) {
            capture(mc, "jgl-world-hud.png"); stage = 8; wait = 0;
        } else if (stage == 8) {
            mc.player.connection.sendCommand("jglv2tablet"); stage = 9; wait = 0;
        } else if (stage == 9) {
            var pos = mc.player.blockPosition().offset(1, 0, -3);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
            mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit); stage = 20; wait = 0;
        } else if (stage == 20) {
            tableCapture = mc.screen == null && TabletSelection.active.equals("justgashloot:verification/basic");
            if (!tableCapture) return;
            mc.options.keyShift.setDown(true); stage = 10; wait = 0;
        } else if (stage == 10) {
            mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND); stage = 11; wait = 0;
        } else if (stage == 11 && mc.screen instanceof TabletScreen screen) {
            tabletOpen = TabletSelection.active.equals("justgashloot:verification/basic");
            mc.options.keyShift.setDown(false); capture(mc, "jgl-tablet-scale-3.png");
            mc.options.guiScale().set(2); mc.resizeGui(); stage = 12; wait = 0;
        } else if (stage == 12 && mc.screen instanceof TabletScreen screen) {
            capture(mc, "jgl-tablet-scale-2.png");
            var next = (net.minecraft.client.gui.components.Button) field(screen, "next");
            for (int i = 0; i < 4; i++) press(next);
            stage = 13; wait = 0;
        } else if (stage == 13 && mc.screen instanceof TabletScreen screen) {
            LibraryState state = (LibraryState) field(screen, "state");
            if (state.offset() != 32) return;
            pagination = state.total() == 42;
            ((net.minecraft.client.gui.components.EditBox) field(screen, "search")).setValue("verification/luck"); stage = 14; wait = 0;
        } else if (stage == 14 && mc.screen instanceof TabletScreen screen) {
            LibraryState state = (LibraryState) field(screen, "state");
            if (state.tables().size() != 1 || !state.tables().get(0).equals("justgashloot:verification/luck")) return;
            search = true;
            var rows = (net.minecraft.client.gui.components.Button[]) field(screen, "rows"); press(rows[0]); stage = 15; wait = 0;
        } else if (stage == 15 && mc.screen instanceof TabletScreen screen) {
            if (!TabletSelection.active.equals("justgashloot:verification/luck")) return;
            selection = true; capture(mc, "jgl-tablet-selected.png"); screen.onClose(); stage = 16; wait = 0;
        } else if (stage == 16) {
            var pos = mc.player.blockPosition().offset(2, 0, -3);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
            mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit); stage = 17; wait = 0;
        } else if (stage == 17) {
            var target = net.minecraft.world.phys.Vec3.atCenterOf(mc.player.blockPosition().offset(2, 0, -3));
            var direction = target.subtract(mc.player.getEyePosition());
            mc.player.setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
            mc.player.setXRot((float) Math.toDegrees(Math.atan2(-direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z))));
            mc.player.connection.sendCommand("jglv2assert"); stage = 18; wait = 0;
        } else if (stage == 18) {
            capture(mc, "jgl-generator-table-hud.png"); stage = 21; wait = 0;
        } else if (stage == 21) {
            boolean server = false;
            try {
                var report = java.nio.file.Path.of("ui-server-results.json");
                if (!java.nio.file.Files.exists(report)) return;
                server = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(report)).getAsJsonObject().get("passed").getAsBoolean();
                boolean passed = rightClick && leftClick && tableCapture && tabletOpen && pagination && search && selection && server;
                var result = new com.google.gson.JsonObject();
                result.addProperty("passed", passed); result.addProperty("rightClickPacket", rightClick); result.addProperty("leftClickPacket", leftClick);
                result.addProperty("tabletCapturePacket", tableCapture); result.addProperty("tabletShiftRightClick", tabletOpen); result.addProperty("tabletPagination", pagination);
                result.addProperty("tabletSearch", search); result.addProperty("tabletSelection", selection); result.addProperty("serverTabletConfiguration", server);
                java.nio.file.Files.writeString(java.nio.file.Path.of("ui-results.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result) + "\n");
                System.out.println("JGL_UI_VERIFICATION " + (passed ? "PASS" : "FAIL"));
            } catch (java.io.IOException error) { throw new RuntimeException(error); }
            mc.stop(); stage = 19;
        }
    }
    private static Object field(Object object, String name) {
        try { var field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object); }
        catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
    }
    private static void press(net.minecraft.client.gui.components.Button button) {
        button.onPress(new net.minecraft.client.input.MouseButtonInfo(0, 0));
    }
    private static void capture(Minecraft mc, String name) { Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1, result -> System.out.println("JGL_SCREENSHOT " + name)); }
}
