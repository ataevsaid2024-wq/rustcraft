package net.rustcraft.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.rustcraft.PlayerStats;
import net.rustcraft.RustInventoryHandler;
import net.rustcraft.RustState;

public class RustInventoryScreen extends HandledScreen<RustInventoryHandler> {
    public RustInventoryScreen(RustInventoryHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 130;
        // прячем стандартные подписи
        this.titleY = -200;
        this.playerInventoryTitleY = -200;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x0 = this.x;
        int y0 = this.y;
        ctx.fill(x0, y0, x0 + backgroundWidth, y0 + backgroundHeight, 0xCC141814);
        ctx.drawText(this.textRenderer, "ИНВЕНТАРЬ", x0 + 8, y0 + 8, 0xFFE8E4D8, true);
        for (Slot s : this.handler.slots) {
            ctx.fill(x0 + s.x - 1, y0 + s.y - 1, x0 + s.x + 17, y0 + s.y + 17, 0x55FFFFFF);
        }
        // шкалы справа от инвентаря
        int px = x0 + backgroundWidth + 8;
        int hp = this.client != null && this.client.player != null
                ? Math.round(this.client.player.getHealth() / this.client.player.getMaxHealth() * 100f) : 100;
        stat(ctx, px, y0 + 8, "Здоровье", hp, 100, 0xFF86AE33);
        stat(ctx, px, y0 + 38, "Вода", RustState.water, PlayerStats.MAX_WATER, 0xFF459CCB);
        stat(ctx, px, y0 + 68, "Еда", RustState.food, PlayerStats.MAX_FOOD, 0xFFC8793F);
    }

    private void stat(DrawContext ctx, int x, int y, String label, int value, int max, int color) {
        ctx.drawText(this.textRenderer, label + ": " + value, x, y, 0xFFFFFFFF, true);
        ctx.fill(x, y + 12, x + 90, y + 20, 0x99000000);
        ctx.fill(x, y + 12, x + Math.round(90f * Math.max(0, Math.min(max, value)) / max), y + 20, color);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }
}
