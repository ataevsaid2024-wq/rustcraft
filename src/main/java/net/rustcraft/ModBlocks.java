package net.rustcraft;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSoundGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static Block STONE_NODE;

    public static void init() {
        Identifier id = Identifier.of(RustCraft.MOD_ID, "stone_node");
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);
        STONE_NODE = Registry.register(Registries.BLOCK, blockKey,
                new Block(AbstractBlock.Settings.create()
                        .registryKey(blockKey)
                        .strength(50.0f)
                        .nonOpaque()
                        .dropsNothing()
                        .sounds(BlockSoundGroup.STONE)));
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);
        Registry.register(Registries.ITEM, itemKey,
                new BlockItem(STONE_NODE, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey()));
    }
}
