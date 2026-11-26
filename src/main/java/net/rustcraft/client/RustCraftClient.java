package net.rustcraft.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.rustcraft.Marker;
import net.rustcraft.MarkerPayload;
import net.rustcraft.HitPayload;
import java.util.HashMap;
import java.util.Map;
import net.rustcraft.ModBlocks;
import net.rustcraft.ModBlockEntities;
import net.rustcraft.Trees;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.rustcraft.ActionPayload;
import net.rustcraft.PlayerStats;
import net.rustcraft.RustInventoryHandler;
import net.rustcraft.RustLayout;
import net.rustcraft.RustState;
import net.rustcraft.StatsPayload;
import net.rustcraft.TogglePayload;
import org.lwjgl.glfw.GLFW;

public class RustCraftClient implements ClientModInitializer {
    public static KeyBinding toggleKey;
    public static KeyBinding buildKey;
    private static long lastHit = 0;
    public static final Map<BlockPos, Integer> COUNTS = new HashMap<>();

    private static final int GREEN = 0xFF86AE33;
    private static final int BLUE = 0xFF459CCB;
    private static final int ORANGE = 0xFFC8793F;
    private static final int BG = 0x99000000;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("rustcraft", "main"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustcraft.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, category));
        buildKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustcraft.build", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, category));

        BlockEntityRendererFactories.register(ModBlockEntities.TREE, TreeRenderer::new);
        HandledScreens.register(RustInventoryHandler.TYPE, RustInventoryScreen::new);

        ClientPlayNetworking.registerGlobalReceiver(StatsPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    RustState.rust = payload.rust();
                    RustState.water = payload.water();
                    RustState.food = payload.food();
                    if (!payload.rust()) RustState.piece = 0;
                }));

        ClientPlayNetworking.registerGlobalReceiver(MarkerPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.count() < 0) COUNTS.remove(payload.base());
                    else COUNTS.put(payload.base(), payload.count());
                }));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getWindow() != null) {
                RustLayout.compute(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
            }
            drawCross(client);
            while (toggleKey.wasPressed()) {
                if (client.player != null) ClientPlayNetworking.send(new TogglePayload());
            }
            while (buildKey.wasPressed()) {
                if (RustState.rust && client.player != null && client.currentScreen == null) {
                    client.setScreen(new RadialMenuScreen());
                }
            }
            // В режиме Rust вместо ванильного инвентаря открываем свой
            if (RustState.rust && client.player != null && client.currentScreen instanceof InventoryScreen) {
                client.setScreen(null);
                ClientPlayNetworking.send(new ActionPayload(1, 0));
            }
        });

        // Удар по дереву или глыбе: вместо ванильной ломки шлём удар на сервер
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient() || !RustState.rust) return ActionResult.PASS;
            var state = world.getBlockState(pos);
            if (Trees.isTree(state) || state.isOf(ModBlocks.STONE_NODE)) {
                long now = System.currentTimeMillis();
                if (now - lastHit < 450) return ActionResult.FAIL;
                lastHit = now;
                ClientPlayNetworking.send(new HitPayload(pos));
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        // Прячем ванильные сердечки, броню, голод и опыт в режиме Rust
        hide(VanillaHudElements.HEALTH_BAR);
        hide(VanillaHudElements.ARMOR_BAR);
        hide(VanillaHudElements.FOOD_BAR);
        hide(VanillaHudElements.EXPERIENCE_LEVEL);
        hide(VanillaHudElements.INFO_BAR);

        HudElementRegistry.addLast(Identifier.of("rustcraft", "stats"), (ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (!RustState.rust || mc.player == null || mc.options.hudHidden) return;
            int hp = Math.round(mc.player.getHealth() / mc.player.getMaxHealth() * 100f);
            int w = ctx.getScaledWindowWidth();
            int h = ctx.getScaledWindowHeight();
            int barW = 90, barH = 12, x = w - barW - 8, y = h - 8 - barH;
            bar(ctx, mc, x, y, barW, barH, RustState.food / (float) PlayerStats.MAX_FOOD, ORANGE, String.valueOf(RustState.food));
            bar(ctx, mc, x, y - 15, barW, barH, RustState.water / (float) PlayerStats.MAX_WATER, BLUE, String.valueOf(RustState.water));
            bar(ctx, mc, x, y - 30, barW, barH, hp / 100f, GREEN, String.valueOf(hp));
        });
    }

    /** Рисует жёлтый крест на стволе, на который смотрит игрок. */
    private static void drawCross(MinecraftClient client) {
        if (!RustState.rust || client.world == null || client.player == null) return;
        if (client.player.age % 4 != 0) return;
        if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos p = hit.getBlockPos();
        if (!client.world.getBlockState(p).isIn(BlockTags.LOGS)) return;

        BlockPos base = Marker.base(client.world, p);
        BlockPos m = Marker.at(client.world, base, COUNTS.getOrDefault(base, 0));
        Direction f = client.player.getHorizontalFacing().getOpposite();
        double cx = m.getX() + 0.5 + f.getOffsetX() * 0.55;
        double cy = m.getY() + 0.5;
        double cz = m.getZ() + 0.5 + f.getOffsetZ() * 0.55;
        double tx = f.getOffsetZ() != 0 ? 1 : 0;
        double tz = f.getOffsetX() != 0 ? 1 : 0;
        for (int i = -2; i <= 2; i++) {
            double d = i * 0.1;
            client.world.addParticleClient(ParticleTypes.FLAME, cx + tx * d, cy + d, cz + tz * d, 0, 0, 0);
            client.world.addParticleClient(ParticleTypes.FLAME, cx + tx * d, cy - d, cz + tz * d, 0, 0, 0);
        }
    }

    private static void hide(Identifier id) {
        HudElementRegistry.replaceElement(id, original -> (ctx, tickCounter) -> {
            if (!RustState.rust) original.render(ctx, tickCounter);
        });
    }

    private static void bar(DrawContext ctx, MinecraftClient mc, int x, int y, int w, int h, float frac, int color, String text) {
        ctx.fill(x, y, x + w, y + h, BG);
        ctx.fill(x, y, x + Math.round(w * Math.max(0f, Math.min(1f, frac))), y + h, color);
        ctx.drawText(mc.textRenderer, text, x + 4, y + 2, 0xFFFFFFFF, true);
    }
}
