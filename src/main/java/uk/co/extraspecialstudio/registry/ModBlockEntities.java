package uk.co.extraspecialstudio.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.block.PlacedNoteBlockEntity;
import uk.co.extraspecialstudio.block.PlacedScrapbookBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Dead_letters.MODID);

    public static final RegistryObject<BlockEntityType<PlacedScrapbookBlockEntity>> PLACED_SCRAPBOOK =
            BLOCK_ENTITY_TYPES.register("placed_scrapbook", () -> BlockEntityType.Builder.of(PlacedScrapbookBlockEntity::new, ModBlocks.PLACED_SCRAPBOOK.get()).build(null));
    public static final RegistryObject<BlockEntityType<PlacedNoteBlockEntity>> PLACED_NOTE =
            BLOCK_ENTITY_TYPES.register("placed_note", () -> BlockEntityType.Builder.of(PlacedNoteBlockEntity::new, ModBlocks.PLACED_NOTE.get()).build(null));

    private ModBlockEntities() {
    }
}
