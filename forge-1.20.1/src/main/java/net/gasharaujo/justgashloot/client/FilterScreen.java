package net.gasharaujo.justgashloot.client;
import net.gasharaujo.justgashloot.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class FilterScreen extends Screen {
    private final LootScreen parent;
    private FilterState state;
    private EditBox input;
    private final Button[] suggestions = new Button[3], rules = new Button[4];
    private Button previous, next;
    private int left, top, panelWidth, first, suggestionFirst, delay = -1;
    public FilterScreen(LootScreen parent, FilterState state) { super(Component.translatable("gui.justgashloot.void.title")); this.parent = parent; this.state = state; }
    @Override protected void init() {
        panelWidth = Math.min(420, width - 16); left = (width - panelWidth) / 2; top = Math.max(4, (height - 228) / 2);
        input = addRenderableWidget(new EditBox(font, left + 8, top + 32, panelWidth - 76, 18, Component.translatable("gui.justgashloot.void.input")));
        input.setMaxLength(VoidRules.LENGTH); input.setHint(Component.translatable("gui.justgashloot.void.input")); input.setValue(state.query());
        input.setResponder(value -> delay = 4);
        addRenderableWidget(Button.builder(Component.translatable("gui.justgashloot.void.add"), button -> request(2, input.getValue())).bounds(left + panelWidth - 64, top + 32, 56, 18).build());
        for (int i = 0; i < suggestions.length; i++) {
            final int row = i;
            suggestions[i] = addRenderableWidget(Button.builder(Component.empty(), button -> {
                int index = suggestionFirst + row; if (index < state.suggestions().size()) input.setValue(state.suggestions().get(index));
            }).bounds(left + 8, top + 54 + i * 16, panelWidth - 16, 16).build());
        }
        for (int i = 0; i < rules.length; i++) {
            final int row = i;
            rules[i] = addRenderableWidget(Button.builder(Component.empty(), button -> {
                int index = first + row; if (index < state.rules().size()) request(3, state.rules().get(index));
            }).bounds(left + 8, top + 120 + i * 18, panelWidth - 16, 18).build());
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> page(-4)).bounds(left + 8, top + 202, 32, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> page(4)).bounds(left + 44, top + 202, 32, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(left + panelWidth - 88, top + 202, 80, 20).build());
        refresh(); setInitialFocus(input);
    }
    private void request(int operation, String value) { if (operation == 2 || operation == 3) delay = -1; FilterNetwork.request(new FilterAction(state.menu(), operation, value, input.getValue())); }
    public void accept(FilterState value) {
        var candidates = input != null && !input.getValue().equals(value.query()) ? state.suggestions() : value.suggestions();
        state = new FilterState(value.menu(), true, value.rules(), value.query(), candidates, value.error());
        first = Math.min(first, Math.max(0, (state.rules().size() - 1) / 4 * 4)); suggestionFirst = 0;
        if (input != null) refresh();
    }
    private void refresh() {
        for (int i = 0; i < suggestions.length; i++) {
            int index = suggestionFirst + i; boolean visible = index < state.suggestions().size(); suggestions[i].visible = visible;
            if (visible) { String id = state.suggestions().get(index); suggestions[i].setMessage(Component.literal(font.plainSubstrByWidth(id, panelWidth - 30))); suggestions[i].setTooltip(Tooltip.create(Component.literal(id))); }
        }
        for (int i = 0; i < rules.length; i++) {
            int index = first + i; boolean visible = index < state.rules().size(); rules[i].visible = visible;
            if (visible) { String id = state.rules().get(index); rules[i].setMessage(Component.literal("\u00d7 " + font.plainSubstrByWidth(id, panelWidth - 40))); rules[i].setTooltip(Tooltip.create(Component.literal(id).append(" — ").append(Component.translatable("gui.justgashloot.void.remove")))); }
        }
        previous.active = first > 0; next.active = first + 4 < state.rules().size();
    }
    private void page(int amount) { first = Math.max(0, Math.min(Math.max(0, (state.rules().size() - 1) / 4 * 4), first + amount)); refresh(); }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if (y >= top + 54 && y < top + 104) { suggestionFirst = Math.max(0, Math.min(Math.max(0, state.suggestions().size() - 3), suggestionFirst + (delta > 0 ? -1 : 1))); refresh(); }
        else page(delta > 0 ? -4 : 4);
        return true;
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (input.isFocused() && key == 258 && !state.suggestions().isEmpty()) { input.setValue(state.suggestions().get(suggestionFirst)); return true; }
        if (input.isFocused() && (key == 257 || key == 335)) { request(2, input.getValue()); return true; }
        return super.keyPressed(key, scanCode, modifiers);
    }
    @Override public void tick() {
        super.tick();
        if (minecraft.player == null || !(minecraft.player.containerMenu instanceof LootMenu menu) || menu.containerId != state.menu()) { minecraft.setScreen(null); return; }
        if (delay > 0 && --delay == 0) { delay = -1; request(1, ""); }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        graphics.fill(0, 0, width, height, 0xB0101010); graphics.fill(left, top, left + panelWidth, top + 228, 0xFF303030);
        graphics.drawString(font, title, left + 8, top + 7, 0xFFFFFFFF, false);
        graphics.drawString(font, Component.translatable(state.error().isEmpty() ? "gui.justgashloot.void.help" : "gui.justgashloot.void." + state.error()), left + 8, top + 19, state.error().isEmpty() ? 0xFFCCCCCC : 0xFFFF7777, false);
        graphics.drawString(font, Component.translatable("gui.justgashloot.void.rules", state.rules().size(), VoidRules.LIMIT), left + 8, top + 106, 0xFFFFFFFF, false);
        if (state.rules().isEmpty()) graphics.drawString(font, Component.translatable("gui.justgashloot.void.empty"), left + 8, top + 124, 0xFFCCCCCC, false);
        super.render(graphics, mouseX, mouseY, partial);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
