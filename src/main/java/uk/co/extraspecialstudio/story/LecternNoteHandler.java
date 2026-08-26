package uk.co.extraspecialstudio.story;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.item.NoteItem;
import uk.co.extraspecialstudio.item.NoteNbt;
import uk.co.extraspecialstudio.item.ScrapbookItem;

@EventBusSubscriber(modid = Dead_letters.MODID)
public final class LecternNoteHandler {
    private LecternNoteHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof LecternBlock)) {
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (!(be instanceof LecternBlockEntity lectern)) {
            return;
        }
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        ItemStack onStand = lectern.getBook();
        InteractionHand hand = event.getHand() != null ? event.getHand() : InteractionHand.MAIN_HAND;
        ItemStack held = event.getItemStack();

        if (!onStand.isEmpty() && onStand.getItem() instanceof NoteItem) {
            String noteId = NoteNbt.getNoteId(onStand);
            if (level.isClientSide) {
                blockVanillaLectern(event);
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    uk.co.extraspecialstudio.client.DeadLettersClientHooks.openNoteScreen(noteId);
                }
                event.setCancellationResult(InteractionResult.SUCCESS);
            } else if (player instanceof ServerPlayer serverPlayer) {
                blockVanillaLectern(event);
                NoteItem.applyServerRead(serverPlayer, noteId);
                serverPlayer.swing(hand, true);
                event.setCancellationResult(InteractionResult.CONSUME);
            } else {
                blockVanillaLectern(event);
                event.setCancellationResult(InteractionResult.CONSUME);
            }
            return;
        }

        if (!onStand.isEmpty() && onStand.getItem() instanceof ScrapbookItem) {
            if (level.isClientSide) {
                blockVanillaLectern(event);
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    uk.co.extraspecialstudio.client.DeadLettersClientHooks.openScrapbookInHand(onStand);
                }
                event.setCancellationResult(InteractionResult.SUCCESS);
            } else if (player instanceof ServerPlayer serverPlayer) {
                blockVanillaLectern(event);
                serverPlayer.swing(hand, true);
                event.setCancellationResult(InteractionResult.CONSUME);
            } else {
                blockVanillaLectern(event);
                event.setCancellationResult(InteractionResult.CONSUME);
            }
            return;
        }

        if (onStand.isEmpty() && isDeadLettersLecternBook(held)) {
            if (level.isClientSide) {
                blockVanillaLectern(event);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
            BlockState state = level.getBlockState(pos);
            if (player instanceof ServerPlayer serverPlayer) {
                if (placeDeadLettersBook(serverPlayer, level, pos, state, hand)) {
                    blockVanillaLectern(event);
                    level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                    serverPlayer.swing(hand, true);
                    event.setCancellationResult(InteractionResult.CONSUME);
                }
            }
        }
    }

    private static boolean isDeadLettersLecternBook(ItemStack held) {
        Item item = held.getItem();
        return item instanceof NoteItem || item instanceof ScrapbookItem;
    }

    /**
     * Vanilla {@link LecternBlock#tryPlaceBook} only accepts writable/written books. Our note and scrapbook stacks
     * must be placed by updating the lectern entity and {@link LecternBlock#HAS_BOOK} like vanilla does internally.
     */
    private static boolean placeDeadLettersBook(ServerPlayer player, Level level, BlockPos pos, BlockState state, InteractionHand hand) {
        if (!(level.getBlockEntity(pos) instanceof LecternBlockEntity lectern)) {
            return false;
        }
        ItemStack inHand = player.getItemInHand(hand);
        if (lectern.hasBook() || inHand.isEmpty() || !isDeadLettersLecternBook(inHand)) {
            return false;
        }
        ItemStack placed = inHand.copyWithCount(1);
        lectern.setBook(placed);
        BlockState withBook = state.setValue(LecternBlock.HAS_BOOK, true);
        if (!level.setBlock(pos, withBook, 3)) {
            lectern.setBook(ItemStack.EMPTY);
            return false;
        }
        lectern.setChanged();
        if (!player.getAbilities().instabuild) {
            inHand.shrink(1);
        }
        return true;
    }

    private static void blockVanillaLectern(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setUseBlock(TriState.FALSE);
        event.setUseItem(TriState.FALSE);
    }
}
