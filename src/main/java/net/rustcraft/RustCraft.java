package net.rustcraft;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RustCraft implements ModInitializer {
    public static final String MOD_ID = "rustcraft";
    public static final Map<UUID, PlayerStats> STATS = new HashMap<>();

    public static PlayerStats stats(ServerPlayerEntity p) {
        return STATS.computeIfAbsent(p.getUuid(), id -> new PlayerStats());
    }

    public static void sync(ServerPlayerEntity p) {
        PlayerStats s = stats(p);
        ServerPlayNetworking.send(p, new StatsPayload(s.rust, s.water, s.food));
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(StatsPayload.ID, StatsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MarkerPayload.ID, MarkerPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TogglePayload.ID, TogglePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ActionPayload.ID, ActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HitPayload.ID, HitPayload.CODEC);

        ModItems.init();
        ModBlocks.init();
        ModBlockEntities.init();
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(ModItems.ROCK);
            entries.add(ModItems.WOOD);
            entries.add(ModItems.STONE);
            entries.add(ModBlocks.STONE_NODE);
        });

        // Удар по дереву / глыбе
        ServerPlayNetworking.registerGlobalReceiver(HitPayload.ID, (payload, context) -> {
            ServerPlayerEntity p = context.player();
            if (!stats(p).rust) return;
            Gathering.hit(p, payload.pos());
        });

        // Инвентарь в стиле Rust
        RustInventoryHandler.TYPE = Registry.register(Registries.SCREEN_HANDLER,
                Identifier.of(MOD_ID, "rust_inventory"),
                new ScreenHandlerType<>(RustInventoryHandler::new, FeatureSet.empty()));

        // Кнопка X на клиенте: переключаем режим
        ServerPlayNetworking.registerGlobalReceiver(TogglePayload.ID, (payload, context) -> {
            ServerPlayerEntity p = context.player();
            applyMode(p, !stats(p).rust);
        });

        // Действия клиента: открыть инвентарь / выбрать постройку
        ServerPlayNetworking.registerGlobalReceiver(ActionPayload.ID, (payload, context) -> {
            ServerPlayerEntity p = context.player();
            PlayerStats s = stats(p);
            if (!s.rust) return;
            if (payload.type() == 1) {
                p.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                        (syncId, inv, player) -> new RustInventoryHandler(syncId, inv),
                        Text.literal("Инвентарь")));
            } else if (payload.type() == 2) {
                s.piece = Math.max(0, Math.min(3, payload.value()));
                p.sendMessage(Text.literal(Building.NAMES[s.piece]), true);
            }
        });

        // Строительство: ПКМ по блоку пустой рукой, когда выбрана постройка
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand != Hand.MAIN_HAND || !player.getMainHandStack().isEmpty()) return ActionResult.PASS;
            if (world.isClient()) {
                return (RustState.rust && RustState.piece != 0) ? ActionResult.SUCCESS : ActionResult.PASS;
            }
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            PlayerStats s = stats(sp);
            if (!s.rust || s.piece == 0) return ActionResult.PASS;
            Building.place(world, sp, hit, s.piece);
            return ActionResult.SUCCESS;
        });

        // /tree [0-3] ставит 3D-дерево перед вами (0 акация, 1-3 деревья)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("tree")
                        .executes(ctx -> spawnTree(ctx, -1))
                        .then(CommandManager.argument("type", IntegerArgumentType.integer(0, 3))
                                .executes(ctx -> spawnTree(ctx, IntegerArgumentType.getInteger(ctx, "type"))))));

        // Команды: /mode rust, /mode minecraft, /mode refill
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("mode")
                        .then(CommandManager.literal("rust").executes(ctx -> setMode(ctx, true)))
                        .then(CommandManager.literal("minecraft").executes(ctx -> setMode(ctx, false)))
                        .then(CommandManager.literal("refill").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            stats(p).refill();
                            sync(p);
                            return 1;
                        }))));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STATS.remove(handler.getPlayer().getUuid()));

        // Тик: жажда -1 раз в секунду, еда -1 раз в 2 секунды (только в режиме Rust)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int t = server.getTicks();
            if (t % 20 != 0) return;
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                PlayerStats s = stats(p);
                if (!s.rust) continue;
                if (s.water > 0) s.water--;
                if (t % 40 == 0 && s.food > 0) s.food--;
                if ((s.water == 0 || s.food == 0) && p.isAlive()) {
                    p.damage((ServerWorld) p.getEntityWorld(), p.getDamageSources().starve(), 1.0f);
                }
                sync(p);
            }
        });
    }

    private static int spawnTree(CommandContext<ServerCommandSource> ctx, int type) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        ServerWorld w = (ServerWorld) p.getEntityWorld();
        Direction f = p.getHorizontalFacing();
        int x = p.getBlockX() + f.getOffsetX() * 6;
        int z = p.getBlockZ() + f.getOffsetZ() * 6;
        int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        int variant = type >= 0 ? type : w.getRandom().nextInt(4);
        Trees.place(w, new BlockPos(x, y, z), variant);
        return 1;
    }

    private static int setMode(CommandContext<ServerCommandSource> ctx, boolean rust) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        applyMode(ctx.getSource().getPlayerOrThrow(), rust);
        return 1;
    }

    public static void applyMode(ServerPlayerEntity p, boolean rust) {
        PlayerStats s = stats(p);
        s.rust = rust;
        s.piece = 0;
        if (rust) {
            s.refill();
            if (p.getInventory().count(ModItems.ROCK) == 0) p.giveItemStack(new ItemStack(ModItems.ROCK));
        }
        sync(p);
        p.sendMessage(Text.literal(rust ? "Режим: RUST" : "Режим: MINECRAFT"), true);
    }
}
