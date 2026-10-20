package net.gasharaujo.justgashloot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ClickType;
abstract class VersionedMenu extends AbstractContainerMenu {
    VersionedMenu(MenuType<?> type, int id) { super(type, id); }
    @Override public void clicked(int slot, int button, ClickType action, Player player) { handleClick(slot, button, action.name(), player); }
    protected abstract void handleClick(int slot, int button, String action, Player player);
    protected void vanillaClicked(int slot, int button, String action, Player player) { super.clicked(slot, button, ClickType.valueOf(action), player); }
}
