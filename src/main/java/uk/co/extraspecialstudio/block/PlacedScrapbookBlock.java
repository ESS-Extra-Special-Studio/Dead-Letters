package uk.co.extraspecialstudio.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;
import uk.co.extraspecialstudio.client.DeadLettersClientHooks;

public class PlacedScrapbookBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            box(3, 0, 4, 13, 1, 12),
            box(3, 1, 4, 13, 2, 12)
    );

    public PlacedScrapbookBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_BROWN)
                .strength(0.35F)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .isSuffocating((s, g, p) -> false)
                .isViewBlocking((s, g, p) -> false));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlacedScrapbookBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlacedScrapbookBlockEntity scrapBe) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DeadLettersClientHooks.openScrapbookAt(scrapBe));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
