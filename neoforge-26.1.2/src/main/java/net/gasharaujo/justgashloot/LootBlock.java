package net.gasharaujo.justgashloot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public final class LootBlock extends Block implements EntityBlock {
    LootBlock(Properties properties) { super(properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LootEntity(pos, state); }
    @Override public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) { return level.getBlockEntity(pos) instanceof LootEntity machine ? machine : null; }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Interactions.use(level, pos, player, InteractionHand.MAIN_HAND); return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        Interactions.use(level, pos, player, hand); return InteractionResult.SUCCESS;
    }

}
