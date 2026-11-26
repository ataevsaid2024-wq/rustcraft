package net.rustcraft;

/** Раскладка инвентаря: подгоняется под размер окна, чтобы занимать почти весь экран. */
public class RustLayout {
    public static int W = 427, H = 240;
    public static int P = 18;      // шаг между слотами
    public static int O = 1;       // отступ предмета внутри клетки
    public static int X0, X1, X2;  // левый, центральный, правый блок
    public static int GRID_Y, HOTBAR_Y;

    static {
        compute(W, H);
    }

    public static void compute(int w, int h) {
        W = w;
        H = h;
        P = (int) Math.max(18, Math.min(40, Math.min(w * 0.042, h * 0.085)));
        O = (P - 16) / 2;
        int gap = (int) (1.2 * P);
        int totalW = 7 * P + 6 * P + 6 * P + 2 * gap;
        X0 = (w - totalW) / 2;
        X1 = X0 + 7 * P + gap;
        X2 = X1 + 6 * P + gap;
        GRID_Y = (int) (h * 0.22);
        HOTBAR_Y = GRID_Y + 5 * P + P / 2;
    }
}
