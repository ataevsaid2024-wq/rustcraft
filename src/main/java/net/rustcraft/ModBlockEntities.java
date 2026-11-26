package net.rustcraft;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static BlockEntityType<TreeBlockEntity> TREE;

    public static void init() {
        TREE = Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(RustCraft.MOD_ID, "tree"),
                FabricBlockEntityTypeBuilder.<TreeBlockEntity>create(TreeBlockEntity::new, ModBlocks.TREE_BASE).build());
    }
}
