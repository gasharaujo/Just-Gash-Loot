package net.gasharaujo.justgashloot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

public record StoredLoot(int slot, ItemStack item, long count) {
    public static final Codec<StoredLoot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, 35).fieldOf("slot").forGetter(StoredLoot::slot),
            ItemStack.CODEC.fieldOf("item").forGetter(StoredLoot::item),
            Codec.LONG.comapFlatMap(value -> value > 0 ? com.mojang.serialization.DataResult.success(value) : com.mojang.serialization.DataResult.error(() -> "Loot count must be positive"), value -> value).fieldOf("count").forGetter(StoredLoot::count)
    ).apply(instance, StoredLoot::new));
}
