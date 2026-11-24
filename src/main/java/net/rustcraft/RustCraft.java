package net.rustcraft;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

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
        PayloadTypeRegistry.playC2S().register(TogglePayload.ID, TogglePayload.CODEC);

        // Кнопка X на клиенте шлёт этот пакет: переключаем режим
        ServerPlayNetworking.registerGlobalReceiver(TogglePayload.ID, (payload, context) -> {
            ServerPlayerEntity p = context.player();
            applyMode(p, !stats(p).rust);
        });

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

    private static int setMode(CommandContext<ServerCommandSource> ctx, boolean rust) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        applyMode(ctx.getSource().getPlayerOrThrow(), rust);
        return 1;
    }

    public static void applyMode(ServerPlayerEntity p, boolean rust) {
        PlayerStats s = stats(p);
        s.rust = rust;
        if (rust) s.refill();
        sync(p);
        p.sendMessage(Text.literal(rust ? "Режим: RUST" : "Режим: MINECRAFT"), true);
    }
}
