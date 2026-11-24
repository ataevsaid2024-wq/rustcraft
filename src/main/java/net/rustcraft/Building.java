package net.rustcraft;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Постройки по сетке 3x3 (как в Rust). Пока из дубовых досок и бесплатно. */
public class Building {
    public static final String[] NAMES = {"Постройка: нет", "Постройка: фундамент", "Постройка: стена", "Постройка: проём"};

    public static void place(World world, PlayerEntity player, BlockHitResult hit, int piece) {
        BlockPos base = hit.getBlockPos().offset(hit.getSide());
        int cx = Math.floorDiv(base.getX(), 3) * 3 + 1;
        int cz = Math.floorDiv(base.getZ(), 3) * 3 + 1;
        int y = base.getY();
        BlockState planks = Blocks.OAK_PLANKS.getDefaultState();

        if (piece == 1) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    set(world, new BlockPos(cx + dx, y, cz + dz), planks);
                }
            }
        } else if (piece == 2 || piece == 3) {
            Direction f = player.getHorizontalFacing();
            int ox = f.getOffsetX();
            int oz = f.getOffsetZ();
            int wx = cx + ox;
            int wz = cz + oz;
            for (int i = -1; i <= 1; i++) {
                for (int h = 0; h < 3; h++) {
                    if (piece == 3 && i == 0 && h < 2) continue; // проём 1x2
                    int px = ox != 0 ? wx : wx + i;
                    int pz = ox != 0 ? wz + i : wz;
                    set(world, new BlockPos(px, y + h, pz), planks);
                }
            }
        }
    }

    private static void set(World world, BlockPos pos, BlockState state) {
        if (world.getBlockState(pos).isReplaceable()) {
            world.setBlockState(pos, state);
        }
    }
}
