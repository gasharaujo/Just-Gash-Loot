package net.gasharaujo.justgashloot;
public final class TabletItem extends net.minecraft.world.item.Item {
    public TabletItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        if (player instanceof net.minecraft.server.level.ServerPlayer server && player.isShiftKeyDown()) Tablets.sync(server, true, "", 0);
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), Platform.clientSide(level));
    }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level, java.util.List<net.minecraft.network.chat.Component> text, net.minecraft.world.item.TooltipFlag flag) {
        text.add(net.minecraft.network.chat.Component.translatable("tooltip.justgashloot.active", TabletSelection.label()));
    }
}
