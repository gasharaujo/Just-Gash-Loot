package net.gasharaujo.justgashloot;

import net.minecraft.world.item.Item;

public final class UpgradeItem extends Item {
    public enum Category { ENERGY, SPEED, QUANTITY, QUALITY, PLAYER }
    public final Category category;
    public final int tier;
    public UpgradeItem(Properties properties, Category category, int tier) {
        super(properties.stacksTo(1));
        this.category = category;
        this.tier = tier;
    }
}
