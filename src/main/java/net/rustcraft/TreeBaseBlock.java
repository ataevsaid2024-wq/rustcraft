package net.rustcraft;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;

/** Нижний блок дерева: к нему привязан рендер 3D-модели. VARIANT выбирает модель (0 акация, 1-3 деревья). */
public class TreeBaseBlock extends TreeTrunkBlock implements BlockEntityProvider {
    public static final IntProperty VARIANT = IntProperty.of("variant", 0, 3);

    public TreeBaseBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(VARIANT, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TreeBlockEntity(pos, state);
    }
}
