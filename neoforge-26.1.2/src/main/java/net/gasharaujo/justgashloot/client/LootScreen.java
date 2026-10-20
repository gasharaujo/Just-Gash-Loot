package net.gasharaujo.justgashloot.client;
import java.util.ArrayList;
import java.util.List;
import net.gasharaujo.justgashloot.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class LootScreen extends AbstractContainerScreen<LootMenu> {
    public LootScreen(LootMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, LootMenu.WIDTH, LootMenu.HEIGHT);

        inventoryLabelY = 129;
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float partial) {
        super.extractBackground(graphics, x, y, partial); frame(graphics);
    }
    private void frame(GuiGraphicsExtractor graphics) {
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
    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.text(font, title, 8, 6, 0xFF404040, false);
        graphics.text(font, Component.translatable("gui.justgashloot.upgrades"), 8, 93, 0xFF404040, false);
        graphics.text(font, playerInventoryTitle, 8, 129, 0xFF404040, false);

    }
    @Override protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        super.extractSlot(graphics, slot, mouseX, mouseY); if (slot.index < 36) count(graphics, slot);
    }
    private void count(GuiGraphicsExtractor graphics, Slot slot) {
        long amount = menu.counts[slot.index];
        if (amount <= 1 || !slot.hasItem()) return;
        String count = LootRules.compact(amount);
        float scale = Math.min(1F, 17F / font.width(count));
        graphics.pose().pushMatrix();
        graphics.pose().translate(slot.x + 17 - font.width(count) * scale, slot.y + 16 - 8 * scale);
        graphics.pose().scale(scale, scale);
        graphics.text(font, count, 0, 0, 0xFFFFFFFF, true);
        graphics.pose().popMatrix();
    }
    @Override protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> result = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (hoveredSlot != null && hoveredSlot.index < 36) result.add(Component.translatable("gui.justgashloot.count", java.text.NumberFormat.getIntegerInstance().format(menu.counts[hoveredSlot.index])));
        if (stack.getItem() instanceof UpgradeItem) result.add(Component.translatable("tooltip.justgashloot.upgrade_pending"));
        return result;
    }
}
