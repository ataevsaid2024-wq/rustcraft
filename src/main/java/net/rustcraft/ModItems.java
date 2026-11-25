package net.rustcraft;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItems {
    public static Item ROCK;
    public static Item WOOD;
    public static Item STONE;

    private static Item register(String name, Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(RustCraft.MOD_ID, name));
        return Registry.register(Registries.ITEM, key, new Item(settings.registryKey(key)));
    }

    public static void init() {
        ROCK = register("rock", new Item.Settings().maxCount(1));
        WOOD = register("wood", new Item.Settings());
        STONE = register("stone", new Item.Settings());
    }
}
