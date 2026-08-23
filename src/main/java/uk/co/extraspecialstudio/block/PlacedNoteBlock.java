package uk.co.extraspecialstudio.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;
import uk.co.extraspecialstudio.client.DeadLettersClientHooks;
import uk.co.extraspecialstudio.item.NoteItem;
import uk.co.extraspecialstudio.registry.ModItems;
import uk.co.extraspecialstudio.story.NoteDefinition;
import uk.co.extraspecialstudio.story.StoryRegistry;

public class PlacedNoteBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {
    private static final VoxelShape SHAPE_FLOOR = box(2.0D, 0.0D, 2.0D, 14.0D, 1.0D, 14.0D);
    private static final VoxelShape SHAPE_CEILING = box(2.0D, 15.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    private static final VoxelShape SHAPE_WALL_NORTH = box(2.0D, 2.0D, 15.0D, 14.0D, 14.0D, 16.0D);
    private static final VoxelShape SHAPE_WALL_SOUTH = box(2.0D, 2.0D, 0.0D, 14.0D, 14.0D, 1.0D);
    private static final VoxelShape SHAPE_WALL_WEST = box(15.0D, 2.0D, 2.0D, 16.0D, 14.0D, 14.0D);
    private static final VoxelShape SHAPE_WALL_EAST = box(0.0D, 2.0D, 2.0D, 1.0D, 14.0D, 14.0D);

    public PlacedNoteBlock() {
        super(Properties.of()
                .mapColor(MapColor.WOOD)
                .sound(SoundType.WOOD)
                .strength(0.2F)
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACE, AttachFace.FLOOR)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        AttachFace face = state.getValue(FACE);
        Direction facing = state.getValue(FACING);
        return switch (face) {
            case FLOOR -> SHAPE_FLOOR;
            case CEILING -> SHAPE_CEILING;
            case WALL -> switch (facing) {
                case NORTH -> SHAPE_WALL_NORTH;
                case SOUTH -> SHAPE_WALL_SOUTH;
                case EAST -> SHAPE_WALL_EAST;
                case WEST -> SHAPE_WALL_WEST;
                default -> Shapes.block();
            };
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlacedNoteBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PlacedNoteBlockEntity noteBe)) {
            return InteractionResult.PASS;
        }
        String noteId = noteBe.getNoteId();
        if (noteId.isBlank()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DeadLettersClientHooks.openNoteScreen(noteId));
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            NoteItem.applyServerRead(serverPlayer, noteId);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.isCreative()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlacedNoteBlockEntity noteBe) {
                String noteId = noteBe.getNoteId();
                if (!noteId.isBlank()) {
                    NoteDefinition note = StoryRegistry.snapshot().notesById().get(noteId);
                    ItemStack stack = note != null ? NoteItem.createStack(note) : new ItemStack(ModItems.NOTE.get());
                    stack.getOrCreateTag().putString(NoteItem.NOTE_ID_TAG, noteId);
                    popResource(level, pos, stack);
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
