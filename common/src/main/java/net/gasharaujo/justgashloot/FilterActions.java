package net.gasharaujo.justgashloot;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
public final class FilterActions {
    private FilterActions() {}
    public static void handle(ServerPlayer player, FilterAction action) {
        if (!(player.containerMenu instanceof LootMenu menu) || menu.machine == null || menu.containerId != action.menu()
                || !menu.stillValid(player) || player.isSpectator() || action.query().length() > VoidRules.LENGTH
                || action.value().length() > VoidRules.LENGTH || action.operation() < 0 || action.operation() > 3) return;
        String error = "";
        var rules = new ArrayList<>(menu.machine.voidRules());
        String value = VoidRules.normalize(action.value());
        if (action.operation() == 2) {
            if (value.isEmpty() || !FilterPlatform.exists(value)) error = "unknown";
            else if (!rules.contains(value)) {
                if (rules.size() == VoidRules.LIMIT) error = "limit";
                else { rules.add(value); menu.machine.setVoidRules(rules); }
            }
        } else if (action.operation() == 3 && rules.remove(value)) menu.machine.setVoidRules(rules);
        FilterNetwork.send(player, new FilterState(menu.containerId, action.operation() == 0, menu.machine.voidRules(), action.query(), FilterPlatform.suggestions(action.query()), error));
    }
}
