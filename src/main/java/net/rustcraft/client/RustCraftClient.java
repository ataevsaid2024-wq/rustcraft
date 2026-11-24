package net.rustcraft.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.rustcraft.PlayerStats;
import net.rustcraft.TogglePayload;
import net.rustcraft.StatsPayload;

public class RustCraftClient implements ClientModInitializer {
    private static boolean rust = false;
    private static int water = PlayerStats.MAX_WATER;
    private static int food = PlayerStats.MAX_FOOD;

    private static final int GREEN = 0xFF86AE33;
    private static final int BLUE = 0xFF459CCB;
    private static final int ORANGE = 0xFFC8793F;
    private static final int BG = 0x99000000;

    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rustcraft.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X,
                KeyBinding.Category.create(Identifier.of("rustcraft", "main"))));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                if (client.player != null) ClientPlayNetworking.send(new TogglePayload());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(StatsPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    rust = payload.rust();
                    water = payload.water();
                    food = payload.food();
                }));

        HudElementRegistry.addLast(Identifier.of("rustcraft", "stats"), (ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (!rust || mc.player == null || mc.options.hudHidden) return;
            int hp = Math.round(mc.player.getHealth() / mc.player.getMaxHealth() * 100f);
            int w = ctx.getScaledWindowWidth();
            int h = ctx.getScaledWindowHeight();
            int barW = 90, barH = 12, x = w - barW - 8, y = h - 8 - barH;
            bar(ctx, mc, x, y, barW, barH, food / (float) PlayerStats.MAX_FOOD, ORANGE, String.valueOf(food));
            bar(ctx, mc, x, y - 15, barW, barH, water / (float) PlayerStats.MAX_WATER, BLUE, String.valueOf(water));
            bar(ctx, mc, x, y - 30, barW, barH, hp / 100f, GREEN, String.valueOf(hp));
        });
    }

    private static void bar(DrawContext ctx, MinecraftClient mc, int x, int y, int w, int h, float frac, int color, String text) {
        ctx.fill(x, y, x + w, y + h, BG);
        ctx.fill(x, y, x + Math.round(w * Math.max(0f, Math.min(1f, frac))), y + h, color);
        ctx.drawText(mc.textRenderer, text, x + 4, y + 2, 0xFFFFFFFF, true);
    }
}
