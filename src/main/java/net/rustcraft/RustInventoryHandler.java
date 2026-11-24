package net.rustcraft;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

public class RustInventoryHandler extends ScreenHandler {
    public static ScreenHandlerType<RustInventoryHandler> TYPE;

    public RustInventoryHandler(int syncId, PlayerInventory inv) {
        super(TYPE, syncId);
        // 27 слотов рюкзака: 9 колонок x 3 ряда
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(inv, 9 + c + r * 9, 8 + c * 18, 24 + r * 18));
            }
        }
        // 9 слотов хотбара
        for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(inv, c, 8 + c * 18, 24 + 3 * 18 + 10));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack original = slot.getStack();
            result = original.copy();
            if (index < 27) {
                if (!this.insertItem(original, 27, 36, false)) return ItemStack.EMPTY;
            } else if (!this.insertItem(original, 0, 27, false)) {
                return ItemStack.EMPTY;
            }
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
