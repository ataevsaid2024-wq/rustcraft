package net.rustcraft;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

/**
 * Раскладка как в Rust:
 * id 0-5: хотбар (6), id 6-35: рюкзак 6x5 (30), id 36-42: доп. рюкзак (7),
 * id 43: оружие, id 44-49: боеприпасы (6), id 50-52: ракеты (3).
 * Слоты 36-52 лежат в эндер-сундуке игрока (сохраняются сами).
 */
public class RustInventoryHandler extends ScreenHandler {
    public static ScreenHandlerType<RustInventoryHandler> TYPE;
    public static final int MAIN_END = 36;
    public static final int TOTAL = 53;

    public RustInventoryHandler(int syncId, PlayerInventory inv) {
        super(TYPE, syncId);
        Inventory extra = inv.player.getEnderChestInventory();

        // хотбар: 6 слотов
        for (int c = 0; c < 6; c++) {
            this.addSlot(new Slot(inv, c, 140 + c * 18, 134));
        }
        // сетка 6 x 5
        for (int k = 0; k < 30; k++) {
            int invIndex = k < 27 ? 9 + k : 6 + (k - 27);
            this.addSlot(new Slot(inv, invIndex, 140 + (k % 6) * 18, 34 + (k / 6) * 18));
        }
        // доп. рюкзак (7)
        for (int c = 0; c < 7; c++) {
            this.addSlot(new Slot(extra, c, 4 + c * 18, 150));
        }
        // оружие (1)
        this.addSlot(new Slot(extra, 7, 262, 34));
        // боеприпасы (6)
        for (int c = 0; c < 6; c++) {
            this.addSlot(new Slot(extra, 8 + c, 262 + c * 18, 72));
        }
        // ракеты (3)
        for (int c = 0; c < 3; c++) {
            this.addSlot(new Slot(extra, 14 + c, 262 + c * 18, 110));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack original = slot.getStack();
            result = original.copy();
            boolean moved;
            if (index < 6) {
                moved = this.insertItem(original, 6, MAIN_END, false);
            } else if (index < MAIN_END) {
                moved = this.insertItem(original, 0, 6, false);
            } else {
                moved = this.insertItem(original, 0, MAIN_END, false);
            }
            if (!moved) return ItemStack.EMPTY;
            if (original.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }
        return result;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
