package net.rustcraft;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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

        if (Trees.isTree(state)) {
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

    private static final Map<BlockPos, Integer> COUNT = new HashMap<>();

    private static void chop(ServerPlayerEntity p, ServerWorld world, BlockPos hit, boolean axe) {
        BlockPos key = Marker.base(world, hit);
        int count = COUNT.getOrDefault(key, 0);
        BlockPos marker = Marker.at(world, key, count);
        boolean bonus = hit.equals(marker);

        int health = HEALTH.getOrDefault(key, 100) - (axe ? 25 : 10);
        int amount = axe ? 10 : 4;
        if (bonus) amount *= 2;
        p.giveItemStack(new ItemStack(ModItems.WOOD, amount));
        p.sendMessage(Text.literal("+" + amount + " дерева" + (bonus ? " (в крест!)" : "")), true);

        if (health <= 0) {
            HEALTH.remove(key);
            COUNT.remove(key);
            ServerPlayNetworking.send(p, new MarkerPayload(key, -1));
            felling(world, key);
        } else {
            HEALTH.put(key, health);
            COUNT.put(key, count + 1);
            ServerPlayNetworking.send(p, new MarkerPayload(key, count + 1));
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

    private static void felling(ServerWorld world, BlockPos base) {
        if (Trees.isMeshTree(world.getBlockState(base))) {
            BlockPos q = base;
            while (Trees.isMeshTree(world.getBlockState(q))) {
                world.setBlockState(q, Blocks.AIR.getDefaultState());
                q = q.up();
            }
        } else {
            fell(world, base);
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
