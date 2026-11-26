package net.rustcraft;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Крест на дереве (как в Rust): по нему надо бить, чтобы получить бонус. После каждого удара крест переезжает. */
public class Marker {
    public static BlockPos base(World world, BlockPos p) {
        BlockPos b = p;
        while (Trees.isTree(world.getBlockState(b.down()))) b = b.down();
        return b.toImmutable();
    }

    public static BlockPos at(World world, BlockPos base, int count) {
        int off = Math.floorMod(base.getX() * 31 + base.getZ() * 17 + count * 7919, 3) + 1;
        for (int i = off; i >= 0; i--) {
            BlockPos p = base.up(i);
            if (Trees.isTree(world.getBlockState(p))) return p.toImmutable();
        }
        return base;
    }
}
