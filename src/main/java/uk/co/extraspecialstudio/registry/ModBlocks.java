package uk.co.extraspecialstudio.registry;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.block.PlacedNoteBlock;
import uk.co.extraspecialstudio.block.PlacedScrapbookBlock;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Dead_letters.MODID);

    public static final RegistryObject<Block> PLACED_SCRAPBOOK = BLOCKS.register("placed_scrapbook", PlacedScrapbookBlock::new);
    public static final RegistryObject<Block> PLACED_NOTE = BLOCKS.register("placed_note", PlacedNoteBlock::new);

    private ModBlocks() {
    }
}
