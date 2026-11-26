package net.rustcraft;

import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class Trees {
    public static boolean isMeshTree(BlockState s) {
        return s.isOf(ModBlocks.TREE_BASE) || s.isOf(ModBlocks.TREE_TRUNK);
    }

    public static boolean isTree(BlockState s) {
        return s.isIn(BlockTags.LOGS) || isMeshTree(s);
    }

    /** Ставит 3D-дерево: нижний блок рисует модель, два блока выше дают ствол для удара. */
    public static void place(ServerWorld world, BlockPos base, int variant) {
        world.setBlockState(base, ModBlocks.TREE_BASE.getDefaultState().with(TreeBaseBlock.VARIANT, variant));
        world.setBlockState(base.up(), ModBlocks.TREE_TRUNK.getDefaultState());
        world.setBlockState(base.up(2), ModBlocks.TREE_TRUNK.getDefaultState());
    }
}
