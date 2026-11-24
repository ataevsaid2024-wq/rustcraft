package net.rustcraft;

/** Состояние на стороне клиента (заполняется из пакета с сервера). */
public class RustState {
    public static boolean rust = false;
    public static int water = PlayerStats.MAX_WATER;
    public static int food = PlayerStats.MAX_FOOD;
    /** Выбранная постройка: 0 нет, 1 фундамент, 2 стена, 3 проём. */
    public static int piece = 0;
}
