package net.rustcraft;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class TreeBlockEntity extends BlockEntity {
    public TreeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TREE, pos, state);
    }
}
