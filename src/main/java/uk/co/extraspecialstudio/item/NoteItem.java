package uk.co.extraspecialstudio.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import uk.co.extraspecialstudio.Config;
import uk.co.extraspecialstudio.block.PlacedNoteBlockEntity;
import uk.co.extraspecialstudio.client.DeadLettersClientHooks;
import uk.co.extraspecialstudio.registry.ModBlocks;
import uk.co.extraspecialstudio.registry.ModItems;
import uk.co.extraspecialstudio.story.NoteDefinition;
import uk.co.extraspecialstudio.story.PlayerStoryData;
import uk.co.extraspecialstudio.story.StoryDefinition;
import uk.co.extraspecialstudio.story.StoryRegistry;

public class NoteItem extends Item {
    public static final String NOTE_ID_TAG = "NoteID";
    public static final String DISPLAY_NAME_TAG = "DeadLetterDisplayName";

    public NoteItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createStack(NoteDefinition note) {
        ItemStack stack = new ItemStack(ModItems.NOTE.get());
        stack.getOrCreateTag().putString(NOTE_ID_TAG, note.id());
        int cmd = StoryRegistry.snapshot().storyTextureSlot(note.storyId());
        stack.getOrCreateTag().putInt("CustomModelData", cmd);
        StoryDefinition story = StoryRegistry.snapshot().storiesById().get(note.storyId());
        String storyName = story != null ? story.name() : note.storyId();
        stack.getOrCreateTag().putString(DISPLAY_NAME_TAG, storyName + " Part " + note.order());
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        String noteId = stack.getOrCreateTag().getString(NOTE_ID_TAG);
        if (!noteId.isBlank()) {
            NoteDefinition note = StoryRegistry.snapshot().notesById().get(noteId);
            if (note != null) {
                StoryDefinition story = StoryRegistry.snapshot().storiesById().get(note.storyId());
                String storyName = story != null ? story.name() : note.storyId();
                return Component.literal(storyName + " Part " + note.order());
            }
            String cached = stack.getOrCreateTag().getString(DISPLAY_NAME_TAG);
            if (!cached.isBlank()) {
                return Component.literal(cached);
            }
        }
        return super.getName(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String noteId = stack.getOrCreateTag().getString(NOTE_ID_TAG);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DeadLettersClientHooks.openNoteScreen(noteId));
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!applyServerRead(serverPlayer, noteId)) {
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || stack.isEmpty()) {
            return InteractionResult.PASS;
        }
        // Plain right-click should read the letter; only sneak(+config) places it.
        // When targeting a block, use() is not always called after useOn PASS — open here.
        if (Config.placeNotesRequireSneak && !player.isShiftKeyDown()) {
            String noteId = stack.getOrCreateTag().getString(NOTE_ID_TAG);
            if (noteId.isBlank()) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DeadLettersClientHooks.openNoteScreen(noteId));
                return InteractionResult.SUCCESS;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                applyServerRead(serverPlayer, noteId);
            }
            return InteractionResult.SUCCESS;
        }
        String noteId = stack.getOrCreateTag().getString(NOTE_ID_TAG);
        if (noteId.isBlank()) {
            return InteractionResult.PASS;
        }
        if (context.getClickedFace() == null) {
            return InteractionResult.PASS;
        }
        if (level.getBlockState(context.getClickedPos()).getBlock() instanceof LecternBlock) {
            return InteractionResult.PASS;
        }
        net.minecraft.core.BlockPos placePos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(placePos).isAir()) {
            return InteractionResult.PASS;
        }
        if (!player.mayUseItemAt(placePos, context.getClickedFace(), stack)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockState placed = ModBlocks.PLACED_NOTE.get().defaultBlockState()
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, faceFromDirection(context.getClickedFace()))
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACING, horizontalFacing(context.getClickedFace(), player));
        if (!level.setBlock(placePos, placed, 3)) {
            return InteractionResult.FAIL;
        }
        if (level.getBlockEntity(placePos) instanceof PlacedNoteBlockEntity noteBe) {
            noteBe.setNoteId(noteId);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static AttachFace faceFromDirection(net.minecraft.core.Direction face) {
        return switch (face) {
            case UP -> AttachFace.FLOOR;
            case DOWN -> AttachFace.CEILING;
            default -> AttachFace.WALL;
        };
    }

    private static net.minecraft.core.Direction horizontalFacing(net.minecraft.core.Direction face, Player player) {
        if (face.getAxis().isHorizontal()) {
            return face;
        }
        return player.getDirection().getOpposite();
    }

    /**
     * Marks acquisition, discovery, and story progression when a note is read (in hand or on a lectern).
     *
     * @return false if the id is unknown (a message is sent to the player).
     */
    public static boolean applyServerRead(ServerPlayer serverPlayer, String noteId) {
        NoteDefinition note = StoryRegistry.snapshot().notesById().get(noteId);
        if (note == null) {
            serverPlayer.sendSystemMessage(Component.literal("Unknown Dead Letter: " + (noteId.isBlank() ? "<none>" : noteId)));
            return false;
        }

        PlayerStoryData.markAcquired(serverPlayer, note.id());
        boolean alreadyDiscovered = PlayerStoryData.isDiscovered(serverPlayer, note.id());
        if (!alreadyDiscovered) {
            PlayerStoryData.markDiscovered(serverPlayer, note.id());
        }

        int progression = PlayerStoryData.getProgressionLevel(serverPlayer, note.storyId());
        if (note.order() == progression) {
            PlayerStoryData.setProgressionLevel(serverPlayer, note.storyId(), progression + 1);
        }
        return true;
    }
}
