package net.rustcraft;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Заменяет ванильные деревья рядом с игроками на 3D-модели (по одному куску карты за тик). */
public class TreeConverter {
    public static boolean enabled = true;

    private static final Map<RegistryKey<World>, Set<Long>> DONE = new HashMap<>();
    private static final List<int[]> OFFSETS = new ArrayList<>();

    static {
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) OFFSETS.add(new int[]{dx, dz});
        }
        OFFSETS.sort((a, b) -> Integer.compare(a[0] * a[0] + a[1] * a[1], b[0] * b[0] + b[1] * b[1]));
    }

    public static void tick(MinecraftServer server) {
        if (!enabled) return;
        int t = server.getTicks();
        for (ServerWorld world : server.getWorlds()) {
            Set<Long> done = DONE.computeIfAbsent(world.getRegistryKey(), k -> new HashSet<>());
            if (t % 12000 == 0) done.clear(); // раз в 10 минут проверяем заново (выросшие саженцы)
            boolean worked = false;
            for (ServerPlayerEntity p : world.getPlayers()) {
                if (worked) break;
                int pcx = p.getBlockX() >> 4;
                int pcz = p.getBlockZ() >> 4;
                for (int[] o : OFFSETS) {
                    int cx = pcx + o[0];
                    int cz = pcz + o[1];
                    long key = ChunkPos.toLong(cx, cz);
                    if (done.contains(key)) continue;
                    WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                    if (chunk == null || !neighboursLoaded(world, cx, cz)) continue;
                    done.add(key);
                    convertChunk(world, chunk);
                    worked = true;
                    break;
                }
            }
        }
    }

    private static boolean neighboursLoaded(ServerWorld world, int cx, int cz) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (world.getChunkManager().getWorldChunk(cx + dx, cz + dz) == null) return false;
            }
        }
        return true;
    }

    private static boolean loaded(ServerWorld world, BlockPos pos) {
        return world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    private static void convertChunk(ServerWorld world, WorldChunk chunk) {
        int sx = chunk.getPos().getStartX();
        int sz = chunk.getPos().getStartZ();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = sx + x;
                int wz = sz + z;
                int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, wx, wz);
                BlockPos p = new BlockPos(wx, top - 1, wz);
                if (!world.getBlockState(p).isIn(BlockTags.LOGS)) continue;
                BlockPos base = p;
                while (world.getBlockState(base.down()).isIn(BlockTags.LOGS)) base = base.down();
                if (!world.getBlockState(base.down()).isIn(BlockTags.DIRT)) continue;
                if (!hasNaturalLeaves(world, p)) continue; // не трогаем срубы из брёвен
                convertTree(world, base.toImmutable());
            }
        }
    }

    private static boolean naturalLeaves(BlockState s) {
        return s.isIn(BlockTags.LEAVES) && s.contains(LeavesBlock.PERSISTENT) && !s.get(LeavesBlock.PERSISTENT);
    }

    private static boolean hasNaturalLeaves(ServerWorld world, BlockPos top) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 3; dy++) {
                    if (naturalLeaves(world.getBlockState(top.add(dx, dy, dz)))) return true;
                }
            }
        }
        return false;
    }

    private static void convertTree(ServerWorld world, BlockPos base) {
        boolean acacia = world.getBlockState(base).isOf(Blocks.ACACIA_LOG);

        Set<BlockPos> logs = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        logs.add(base);
        queue.add(base);
        while (!queue.isEmpty() && logs.size() < 800) {
            BlockPos c = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos n = c.add(dx, dy, dz);
                        if (logs.contains(n)) continue;
                        if (Math.abs(n.getX() - base.getX()) > 8 || Math.abs(n.getZ() - base.getZ()) > 8) continue;
                        if (n.getY() < base.getY() || n.getY() - base.getY() > 45) continue;
                        if (!loaded(world, n)) continue;
                        if (world.getBlockState(n).isIn(BlockTags.LOGS)) {
                            logs.add(n.toImmutable());
                            queue.add(n.toImmutable());
                        }
                    }
                }
            }
        }

        Set<BlockPos> leaves = new HashSet<>();
        for (BlockPos l : logs) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dy = -4; dy <= 4; dy++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        BlockPos n = l.add(dx, dy, dz);
                        if (!loaded(world, n)) continue;
                        if (naturalLeaves(world.getBlockState(n))) leaves.add(n.toImmutable());
                    }
                }
            }
        }

        for (BlockPos l : logs) {
            if (!l.equals(base)) world.setBlockState(l, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
        for (BlockPos l : leaves) world.setBlockState(l, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);

        int variant = acacia ? 0 : 1 + Math.floorMod(base.getX() * 31 + base.getZ() * 17, 3);
        Trees.place(world, base, variant);
    }
}
