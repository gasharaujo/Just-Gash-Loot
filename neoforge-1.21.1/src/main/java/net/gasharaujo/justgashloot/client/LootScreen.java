package net.gasharaujo.justgashloot.client;
import java.util.ArrayList;
import java.util.List;
import net.gasharaujo.justgashloot.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class LootScreen extends AbstractContainerScreen<LootMenu> {
    public LootScreen(LootMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = LootMenu.WIDTH; imageHeight = LootMenu.HEIGHT;
        inventoryLabelY = 129;
    }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int x, int y) { frame(graphics); }
    @Override public void render(GuiGraphics graphics, int x, int y, float partial) {
        renderBackground(graphics, x, y, partial); super.render(graphics, x, y, partial); renderTooltip(graphics, x, y);
    }
    private void frame(GuiGraphics graphics) {
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + 176, y + 222, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 175, y + 221, 0xFFFFFFFF);
        graphics.fill(x + 3, y + 3, x + 175, y + 221, 0xFF555555);
        graphics.fill(x + 3, y + 3, x + 173, y + 219, 0xFFC6C6C6);
        for (Slot slot : menu.slots) {
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFFFFFFFF);
            graphics.fill(sx, sy, sx + 17, sy + 17, 0xFF373737);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
        }
    }
    @Override protected void renderLabels(GuiGraphics graphics, int x, int y) {
        graphics.drawString(font, title, 8, 6, 0xFF404040, false);
        graphics.drawString(font, Component.translatable("gui.justgashloot.upgrades"), 8, 93, 0xFF404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, 129, 0xFF404040, false);
        for (int i = 0; i < 36; i++) count(graphics, menu.slots.get(i));
    }

    private void count(GuiGraphics graphics, Slot slot) {
        long amount = menu.counts[slot.index];
        if (amount <= 1 || !slot.hasItem()) return;
        String count = LootRules.compact(amount);
        float scale = Math.min(1F, 17F / font.width(count));
        graphics.pose().pushPose();
        graphics.pose().translate(slot.x + 17 - font.width(count) * scale, slot.y + 16 - 8 * scale, 250);
        graphics.pose().scale(scale, scale, 1F);
        graphics.drawString(font, count, 0, 0, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }
    @Override protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> result = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (hoveredSlot != null && hoveredSlot.index < 36) result.add(Component.translatable("gui.justgashloot.count", java.text.NumberFormat.getIntegerInstance().format(menu.counts[hoveredSlot.index])));
        if (stack.getItem() instanceof UpgradeItem) result.add(Component.translatable("tooltip.justgashloot.upgrade_pending"));
        return result;
    }
}
