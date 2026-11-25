package net.rustcraft.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.rustcraft.RustInventoryHandler;

public class RustInventoryScreen extends HandledScreen<RustInventoryHandler> {
    public RustInventoryScreen(RustInventoryHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 380;
        this.backgroundHeight = 180;
        this.titleY = -200;
        this.playerInventoryTitleY = -200;
    }

    private void label(DrawContext ctx, String text, int x, int y) {
        ctx.drawText(this.textRenderer, text, this.x + x, this.y + y, 0xFFE8E4D8, true);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        ctx.fill(0, 0, this.width, this.height, 0x88000000);

        String name = (this.client != null && this.client.player != null)
                ? this.client.player.getName().getString() : "";
        label(ctx, "Rust in Minecraft", 4, 0);
        label(ctx, name, 4, 12);

        label(ctx, "Взрыв 0%", 4, 60);
        label(ctx, "Холод 0%", 4, 74);
        label(ctx, "Радиация 0%", 4, 88);
        label(ctx, "Укусы 0%", 4, 102);
        label(ctx, "РЮКЗАК", 4, 138);

        label(ctx, "ИНВЕНТАРЬ", 140, 22);
        label(ctx, "ВООРУЖЕНИЕ", 262, 8);
        label(ctx, "ОРУЖИЕ", 284, 39);
        label(ctx, "БОЕПРИПАСЫ ДЛЯ ОРУЖИЯ", 262, 60);
        label(ctx, "РАКЕТЫ", 262, 98);

        for (Slot s : this.handler.slots) {
            ctx.fill(this.x + s.x - 1, this.y + s.y - 1, this.x + s.x + 17, this.y + s.y + 17, 0x55FFFFFF);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }
}
