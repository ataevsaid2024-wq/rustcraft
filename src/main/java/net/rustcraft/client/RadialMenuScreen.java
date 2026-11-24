package net.rustcraft.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.rustcraft.ActionPayload;
import net.rustcraft.RustState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.lwjgl.glfw.GLFW;

/** Круговое меню строительства: зажми C, наведи мышь, отпусти C (или кликни). */
public class RadialMenuScreen extends Screen {
    // 0 = вверх, 1 = вправо, 2 = вниз, 3 = влево
    private static final String[] LABELS = {"Фундамент", "Стена", "Проём", "Отмена"};
    private static final int[] PIECES = {1, 2, 3, 0};

    private boolean seenKey = false;
    private boolean prevMouse = true; // чтобы клик, открывший меню, не считался

    public RadialMenuScreen() {
        super(Text.literal("Строительство"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int hovered(int mouseX, int mouseY) {
        int dx = mouseX - this.width / 2;
        int dy = mouseY - this.height / 2;
        if (dx * dx + dy * dy < 20 * 20) return -1;
        if (Math.abs(dx) > Math.abs(dy)) return dx > 0 ? 1 : 3;
        return dy < 0 ? 0 : 2;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x66000000);
        int cx = this.width / 2;
        int cy = this.height / 2;
        int hov = hovered(mouseX, mouseY);

        int[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int i = 0; i < 4; i++) {
            int bx = cx + dirs[i][0] * 75;
            int by = cy + dirs[i][1] * 50;
            boolean selected = PIECES[i] == RustState.piece;
            int color = i == hov ? 0xCC459CCB : (selected ? 0xCC86AE33 : 0xAA141814);
            ctx.fill(bx - 40, by - 14, bx + 40, by + 14, color);
            int tw = this.textRenderer.getWidth(LABELS[i]);
            ctx.drawText(this.textRenderer, LABELS[i], bx - tw / 2, by - 4, 0xFFFFFFFF, true);
        }
        ctx.fill(cx - 4, cy - 4, cx + 4, cy + 4, 0xFFFFFFFF);

        // опрос клавиши и мыши напрямую через GLFW
        long handle = this.client.getWindow().getHandle();
        int code = KeyBindingHelper.getBoundKeyOf(RustCraftClient.buildKey).getCode();
        boolean keyDown = GLFW.glfwGetKey(handle, code) == GLFW.GLFW_PRESS;
        boolean mouseDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (keyDown) seenKey = true;
        boolean released = seenKey && !keyDown;
        boolean clicked = mouseDown && !prevMouse;
        prevMouse = mouseDown;
        if (released || clicked) {
            if (hov >= 0) {
                RustState.piece = PIECES[hov];
                ClientPlayNetworking.send(new ActionPayload(2, PIECES[hov]));
            }
            this.close();
        }
    }
}
