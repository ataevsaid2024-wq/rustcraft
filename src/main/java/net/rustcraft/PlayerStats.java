package net.rustcraft;

public class PlayerStats {
    public static final int MAX_WATER = 250;
    public static final int MAX_FOOD = 500;

    public boolean rust = false;
    public int water = MAX_WATER;
    public int food = MAX_FOOD;
    public int piece = 0;

    public void refill() {
        water = MAX_WATER;
        food = MAX_FOOD;
    }
}
