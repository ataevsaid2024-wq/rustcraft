package net.rustcraft.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.rustcraft.RustInventoryHandler;
import net.rustcraft.RustLayout;

public class RustInventoryScreen extends HandledScreen<RustInventoryHandler> {
    public RustInventoryScreen(RustInventoryHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.titleY = -200;
        this.playerInventoryTitleY = -200;
    }

    @Override
    protected void init() {
        // окно инвентаря занимает весь экран, слоты лежат в экранных координатах
        this.backgroundWidth = this.width;
        this.backgroundHeight = this.height;
        super.init();
    }

    private void label(DrawContext ctx, String text, int x, int y) {
        ctx.drawText(this.textRenderer, text, x, y, 0xFFF2EEE0, true);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        // лёгкое затемнение вместо чёрного фона
        ctx.fill(0, 0, this.width, this.height, 0x22000000);

        int P = RustLayout.P;
        int gy = RustLayout.GRID_Y;
        String name = (this.client != null && this.client.player != null)
                ? this.client.player.getName().getString() : "";

        label(ctx, "Rust in Minecraft", RustLayout.X0, 8);
        label(ctx, name, RustLayout.X0, 20);

        for (int i = 0; i < 4; i++) {
            String[] t = {"Взрыв 0%", "Холод 0%", "Радиация 0%", "Укусы 0%"};
            label(ctx, t[i], RustLayout.X0, gy + i * 14);
        }
        label(ctx, "РЮКЗАК", RustLayout.X0, RustLayout.HOTBAR_Y - 12);

        label(ctx, "ИНВЕНТАРЬ", RustLayout.X1, gy - 12);
        label(ctx, "ВООРУЖЕНИЕ", RustLayout.X2, gy - 12);
        label(ctx, "ОРУЖИЕ", RustLayout.X2 + P + 6, gy + P / 2 - 4);
        label(ctx, "БОЕПРИПАСЫ ДЛЯ ОРУЖИЯ", RustLayout.X2, gy + 2 * P - 12);
        label(ctx, "РАКЕТЫ", RustLayout.X2, gy + 4 * P - 12);

        for (Slot s : this.handler.slots) {
            int bx = s.x - RustLayout.O;
            int by = s.y - RustLayout.O;
            ctx.fill(bx, by, bx + P - 2, by + P - 2, 0x55A0A89C);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }
}
