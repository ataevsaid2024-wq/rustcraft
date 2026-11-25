package net.rustcraft;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Добыча как в Rust: бьёшь по дереву или глыбе несколько раз, с каждого удара получаешь ресурс. */
public class Gathering {
    private static final Map<BlockPos, Integer> HEALTH = new HashMap<>();
    private static final Map<UUID, Integer> LAST_HIT = new HashMap<>();

    public static void hit(ServerPlayerEntity p, BlockPos pos) {
        if (p.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 36.0) return;
        ServerWorld world = (ServerWorld) p.getEntityWorld();

        int now = world.getServer().getTicks();
        Integer last = LAST_HIT.get(p.getUuid());
        if (last != null && now - last < 8) return;
        LAST_HIT.put(p.getUuid(), now);

        BlockState state = world.getBlockState(pos);
        ItemStack tool = p.getMainHandStack();
        boolean rock = tool.isOf(ModItems.ROCK);
        boolean axe = tool.isIn(ItemTags.AXES);
        boolean pick = tool.isIn(ItemTags.PICKAXES);

        if (state.isIn(BlockTags.LOGS)) {
            if (!rock && !axe) {
                p.sendMessage(Text.literal("Нужен камень или топор"), true);
                return;
            }
            chop(p, world, pos, axe);
        } else if (state.isOf(ModBlocks.STONE_NODE)) {
            if (!rock && !pick) {
                p.sendMessage(Text.literal("Нужен камень или кирка"), true);
                return;
            }
            mine(p, world, pos, pick);
        }
    }

    private static void chop(ServerPlayerEntity p, ServerWorld world, BlockPos hit, boolean axe) {
        BlockPos base = hit;
        while (world.getBlockState(base.down()).isIn(BlockTags.LOGS)) base = base.down();
        BlockPos key = base.toImmutable();

        int health = HEALTH.getOrDefault(key, 100) - (axe ? 25 : 10);
        int amount = axe ? 15 : 6;
        p.giveItemStack(new ItemStack(ModItems.WOOD, amount));
        p.sendMessage(Text.literal("+" + amount + " дерева"), true);

        if (health <= 0) {
            HEALTH.remove(key);
            fell(world, key);
        } else {
            HEALTH.put(key, health);
        }
    }

    private static void mine(ServerPlayerEntity p, ServerWorld world, BlockPos pos, boolean pick) {
        BlockPos key = pos.toImmutable();
        int health = HEALTH.getOrDefault(key, 120) - (pick ? 30 : 10);
        int amount = pick ? 12 : 4;
        p.giveItemStack(new ItemStack(ModItems.STONE, amount));
        p.sendMessage(Text.literal("+" + amount + " камней"), true);

        if (health <= 0) {
            HEALTH.remove(key);
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
        } else {
            HEALTH.put(key, health);
        }
    }

    /** Убирает дерево целиком: ствол, ветки и листву рядом. */
    private static void fell(ServerWorld world, BlockPos base) {
        Set<BlockPos> logs = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        logs.add(base);
        queue.add(base);
        while (!queue.isEmpty() && logs.size() < 200) {
            BlockPos c = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos n = c.add(dx, dy, dz);
                        if (logs.contains(n)) continue;
                        if (Math.abs(n.getX() - base.getX()) > 4 || Math.abs(n.getZ() - base.getZ()) > 4) continue;
                        if (n.getY() < base.getY() || n.getY() - base.getY() > 24) continue;
                        if (world.getBlockState(n).isIn(BlockTags.LOGS)) {
                            logs.add(n);
                            queue.add(n);
                        }
                    }
                }
            }
        }
        Set<BlockPos> leaves = new HashSet<>();
        for (BlockPos l : logs) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        BlockPos n = l.add(dx, dy, dz);
                        if (world.getBlockState(n).isIn(BlockTags.LEAVES)) leaves.add(n.toImmutable());
                    }
                }
            }
        }
        for (BlockPos l : logs) world.setBlockState(l, Blocks.AIR.getDefaultState());
        for (BlockPos l : leaves) world.setBlockState(l, Blocks.AIR.getDefaultState());
    }
}
