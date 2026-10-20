package net.gasharaujo.justgashloot.client;
import net.gasharaujo.justgashloot.LootMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.inventory.ClickType;
public final class VerificationClient {
    private static int stage, ticks, wait;
    private static boolean rightClick, leftClick;
    public static void install() { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event) -> tick()); }
    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (++ticks > 5000) { System.out.println("JGL_UI_FAIL timeout"); mc.stop(); return; }
        if (mc.player == null || mc.level == null) return;
        mc.getToasts().clear();
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        if (++wait < 30) return;
        if (stage == 0) {
            mc.options.guiScale().set(2); mc.resizeDisplay(); mc.player.connection.sendCommand("jgluitest"); stage = 1; wait = 0;
        } else if (stage == 1 && mc.player.containerMenu instanceof LootMenu menu && menu.counts[2] == Long.MAX_VALUE) {
            capture(mc, "jgl-gui-scale-2.png"); stage = 2; wait = 0;
        } else if (stage == 2) {
            mc.options.guiScale().set(3); mc.resizeDisplay(); stage = 3; wait = 0;
        } else if (stage == 3 && mc.player.containerMenu instanceof LootMenu menu) {
            capture(mc, "jgl-gui-scale-3.png");
            mc.gameMode.handleInventoryMouseClick(menu.containerId, 0, 1, ClickType.PICKUP, mc.player); stage = 4; wait = 0;
        } else if (stage == 4 && mc.player.containerMenu instanceof LootMenu menu) {
            rightClick = menu.counts[0] == 123455 && menu.getCarried().getCount() == 1;
            mc.gameMode.handleInventoryMouseClick(menu.containerId, 0, 0, ClickType.PICKUP, mc.player); stage = 5; wait = 0;
        } else if (stage == 5 && mc.player.containerMenu instanceof LootMenu menu) {
            leftClick = menu.counts[0] == 123392 && menu.getCarried().getCount() == 64;
            mc.gameMode.handleInventoryMouseClick(menu.containerId, 41, 0, ClickType.PICKUP, mc.player); stage = 6; wait = 0;
        } else if (stage == 6) {
            mc.player.closeContainer(); mc.player.setYRot(180); mc.player.setXRot(20.5F); stage = 7; wait = 0;
        } else if (stage == 7 && wait > 80) {
            capture(mc, "jgl-world-hud.png"); stage = 8; wait = 0;
        } else if (stage == 8) {
            try { java.nio.file.Files.writeString(java.nio.file.Path.of("ui-results.json"), "{\n  \"passed\": " + (rightClick && leftClick) + ",\n  \"rightClickPacket\": " + rightClick + ",\n  \"leftClickPacket\": " + leftClick + "\n}\n"); } catch (java.io.IOException error) { throw new RuntimeException(error); }
            System.out.println("JGL_UI_VERIFICATION " + (rightClick && leftClick ? "PASS" : "FAIL")); mc.stop(); stage = 9;
        }
    }
    private static void capture(Minecraft mc, String name) { Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), result -> System.out.println("JGL_SCREENSHOT " + name)); }
}
