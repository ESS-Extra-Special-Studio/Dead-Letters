package uk.co.extraspecialstudio.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.block.PlacedNoteBlock;
import uk.co.extraspecialstudio.block.PlacedScrapbookBlock;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Dead_letters.MODID);

    public static final DeferredHolder<Block, Block> PLACED_SCRAPBOOK = BLOCKS.register("placed_scrapbook", () -> new PlacedScrapbookBlock());
    public static final DeferredHolder<Block, Block> PLACED_NOTE = BLOCKS.register("placed_note", () -> new PlacedNoteBlock());

    private ModBlocks() {
    }
}
