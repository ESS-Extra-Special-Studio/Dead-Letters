package uk.co.extraspecialstudio.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.block.PlacedNoteBlockEntity;
import uk.co.extraspecialstudio.block.PlacedScrapbookBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Dead_letters.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlacedScrapbookBlockEntity>> PLACED_SCRAPBOOK =
            BLOCK_ENTITY_TYPES.register("placed_scrapbook",
                    () -> BlockEntityType.Builder.of(PlacedScrapbookBlockEntity::new, ModBlocks.PLACED_SCRAPBOOK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlacedNoteBlockEntity>> PLACED_NOTE =
            BLOCK_ENTITY_TYPES.register("placed_note",
                    () -> BlockEntityType.Builder.of(PlacedNoteBlockEntity::new, ModBlocks.PLACED_NOTE.get()).build(null));

    private ModBlockEntities() {
    }
}
