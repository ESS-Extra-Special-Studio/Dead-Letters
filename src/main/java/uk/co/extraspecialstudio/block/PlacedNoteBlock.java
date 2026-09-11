package uk.co.extraspecialstudio.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;
import uk.co.extraspecialstudio.client.DeadLettersClientHooks;
import uk.co.extraspecialstudio.item.NoteItem;
import uk.co.extraspecialstudio.item.NoteNbt;
import uk.co.extraspecialstudio.registry.ModItems;
import uk.co.extraspecialstudio.story.NoteDefinition;
import uk.co.extraspecialstudio.story.StoryRegistry;

public class PlacedNoteBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<PlacedNoteBlock> CODEC = simpleCodec(PlacedNoteBlock::new);

    private static final VoxelShape SHAPE_FLOOR = box(2.0D, 0.0D, 2.0D, 14.0D, 1.0D, 14.0D);
    private static final VoxelShape SHAPE_CEILING = box(2.0D, 15.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    private static final VoxelShape SHAPE_WALL_NORTH = box(2.0D, 2.0D, 15.0D, 14.0D, 14.0D, 16.0D);
    private static final VoxelShape SHAPE_WALL_SOUTH = box(2.0D, 2.0D, 0.0D, 14.0D, 14.0D, 1.0D);
    private static final VoxelShape SHAPE_WALL_WEST = box(15.0D, 2.0D, 2.0D, 16.0D, 14.0D, 14.0D);
    private static final VoxelShape SHAPE_WALL_EAST = box(0.0D, 2.0D, 2.0D, 1.0D, 14.0D, 14.0D);

    public PlacedNoteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACE, AttachFace.FLOOR)
                .setValue(FACING, Direction.NORTH));
    }

    public PlacedNoteBlock() {
        this(Properties.of()
                .mapColor(MapColor.WOOD)
                .sound(SoundType.WOOD)
                .strength(0.2F)
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PlacedNoteBlockEntity noteBe)) {
            return InteractionResult.PASS;
        }
        String noteId = noteBe.getNoteId();
        if (noteId.isBlank()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                DeadLettersClientHooks.openNoteScreen(noteId);
            }
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            NoteItem.applyServerRead(serverPlayer, noteId);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Creative breaks should not drop; clear NoteID before onRemove runs.
        if (!level.isClientSide && player.isCreative()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlacedNoteBlockEntity noteBe) {
                noteBe.setNoteId("");
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlacedNoteBlockEntity noteBe) {
                String noteId = noteBe.getNoteId();
                if (!noteId.isBlank()) {
                    NoteDefinition note = StoryRegistry.snapshot().notesById().get(noteId);
                    ItemStack stack = note != null ? NoteItem.createStack(note) : new ItemStack(ModItems.NOTE.get());
                    NoteNbt.setNoteId(stack, noteId);
                    popResource(level, pos, stack);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
