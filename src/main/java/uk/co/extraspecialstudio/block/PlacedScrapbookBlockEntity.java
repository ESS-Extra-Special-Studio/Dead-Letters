package uk.co.extraspecialstudio.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import uk.co.extraspecialstudio.registry.ModBlockEntities;

public class PlacedScrapbookBlockEntity extends BlockEntity {
    public PlacedScrapbookBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLACED_SCRAPBOOK.get(), pos, state);
    }
}
