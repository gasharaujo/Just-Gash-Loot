package net.gasharaujo.justgashloot.client;
import net.gasharaujo.justgashloot.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
public final class TabletScreen extends Screen {
    private LibraryState state;
    private EditBox search;
    private final Button[] rows = new Button[8];
    private Button previous, next;
    private int first, queryDelay = -1, left, top, panelWidth;
    public TabletScreen(LibraryState state) { super(Component.translatable("gui.justgashloot.library")); this.state = state; }
    @Override protected void init() {
        panelWidth = Math.min(420, width - 16); left = (width - panelWidth) / 2; top = Math.max(4, (height - 228) / 2);
        search = addRenderableWidget(new EditBox(font, left + 8, top + 32, panelWidth - 16, 18, Component.translatable("gui.justgashloot.search")));
        search.setMaxLength(128); search.setHint(Component.translatable("gui.justgashloot.search")); search.setValue(state.query());
        search.setResponder(value -> queryDelay = 5);
        for (int i = 0; i < rows.length; i++) {
            final int row = i;
            rows[i] = addRenderableWidget(Button.builder(Component.empty(), button -> {
                int index = first + row;
                if (index < state.tables().size()) TabletNetwork.request(new LibraryAction(1, state.tables().get(index), 0));
            }).bounds(left + 8, top + 56 + i * 18, panelWidth - 16, 18).build());
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> page(-1)).bounds(left + 8, top + 202, 36, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> page(1)).bounds(left + 48, top + 202, 36, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(left + panelWidth - 88, top + 202, 80, 20).build());
        refresh();
    }
    public void accept(LibraryState value) {
        if (value.open()) { state = value; first = 0; }
        else state = new LibraryState(true, value.active(), state.query(), state.offset(), state.total(), state.tables());
        if (search != null) refresh();
    }
    private void refresh() {
        for (int i = 0; i < rows.length; i++) {
            int index = first + i; boolean present = index < state.tables().size();
            rows[i].visible = present;
            if (present) {
                String id = state.tables().get(index);
                rows[i].setMessage(Component.literal((id.equals(state.active()) ? "\u2713 " : "") + font.plainSubstrByWidth(id, panelWidth - 40))
                        .withStyle(id.equals(state.active()) ? ChatFormatting.GREEN : ChatFormatting.WHITE));
                rows[i].setTooltip(Tooltip.create(Component.literal(id)));
            }
        }
        previous.active = first > 0 || state.offset() > 0;
        next.active = first + 8 < state.tables().size() || state.offset() + state.tables().size() < state.total();
    }
    private void page(int direction) {
        int candidate = first + direction * 8;
        if (candidate >= 0 && candidate < state.tables().size()) { first = candidate; refresh(); }
        else TabletNetwork.request(new LibraryAction(2, search.getValue(), Math.max(0, state.offset() + direction * LibraryState.PAGE_SIZE)));
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double delta) { if (delta != 0) { page(delta > 0 ? -1 : 1); return true; } return false; }
    @Override public void tick() {
        super.tick();
        if (queryDelay > 0 && --queryDelay == 0) { queryDelay = -1; TabletNetwork.request(new LibraryAction(2, search.getValue(), 0)); }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        graphics.fill(0, 0, width, height, 0xB0101010);
        graphics.fill(left, top, left + panelWidth, top + 228, 0xFF303030);
        graphics.text(font, title, left + 8, top + 7, 0xFFFFFFFF, false);
        String active = Component.translatable("tooltip.justgashloot.active", TabletSelection.label()).getString();
        graphics.text(font, font.plainSubstrByWidth(active, panelWidth - 16), left + 8, top + 18, 0xFFFFFFFF, false);
        if (state.tables().isEmpty()) graphics.text(font, Component.translatable("gui.justgashloot.empty"), left + 8, top + 64, 0xFFCCCCCC, false);
        super.extractRenderState(graphics, mouseX, mouseY, partial);
    }
    @Override public boolean isPauseScreen() { return false; }
}
