package uk.co.extraspecialstudio.client;

import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import uk.co.extraspecialstudio.block.PlacedScrapbookBlockEntity;
import uk.co.extraspecialstudio.item.ScrapbookItem;

public final class ScrapbookAnimationController {
    public static final String OPEN = "scrapbook.open";
    public static final String CLOSE = "scrapbook.close";
    public static final String PAGE_FORWARD = "scrapbook.page_forward";
    public static final String PAGE_BACKWARD = "scrapbook.page_backward";

    private ScrapbookAnimationController() {
    }

    public static void playOpen(Player player, ItemStack stack) {
        playItem(player, stack, OPEN);
    }

    public static void playClose(Player player, ItemStack stack) {
        playItem(player, stack, CLOSE);
    }

    public static void playPageForward(Player player, ItemStack stack) {
        playItem(player, stack, PAGE_FORWARD);
    }

    public static void playPageBackward(Player player, ItemStack stack) {
        playItem(player, stack, PAGE_BACKWARD);
    }

    public static void playOpen(Player player, PlacedScrapbookBlockEntity blockEntity) {
        playBlockEntity(blockEntity, OPEN);
    }

    public static void playClose(Player player, PlacedScrapbookBlockEntity blockEntity) {
        playBlockEntity(blockEntity, CLOSE);
    }

    public static void playPageForward(Player player, PlacedScrapbookBlockEntity blockEntity) {
        playBlockEntity(blockEntity, PAGE_FORWARD);
    }

    public static void playPageBackward(Player player, PlacedScrapbookBlockEntity blockEntity) {
        playBlockEntity(blockEntity, PAGE_BACKWARD);
    }

    private static void playItem(Player player, ItemStack stack, String animation) {
        if (player == null || stack == null || stack.isEmpty() || !(stack.getItem() instanceof ScrapbookItem)) {
            return;
        }
        AzCommand.create(ScrapbookItemAnimator.CONTROLLER, animation, AzPlayBehaviors.PLAY_ONCE).sendForItem(player, stack);
    }

    private static void playBlockEntity(BlockEntity blockEntity, String animation) {
        if (!(blockEntity instanceof PlacedScrapbookBlockEntity)) {
            return;
        }
        AzCommand.create(ScrapbookItemAnimator.CONTROLLER, animation, AzPlayBehaviors.PLAY_ONCE).sendForBlockEntity(blockEntity);
    }
}
