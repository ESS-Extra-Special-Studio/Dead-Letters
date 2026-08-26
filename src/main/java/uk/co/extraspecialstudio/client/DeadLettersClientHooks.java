package uk.co.extraspecialstudio.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import uk.co.extraspecialstudio.block.PlacedScrapbookBlockEntity;
import uk.co.extraspecialstudio.network.DeadLettersNetwork;
import uk.co.extraspecialstudio.network.RequestNotebookDataPacket;
import uk.co.extraspecialstudio.story.NoteDefinition;
import uk.co.extraspecialstudio.story.StoryRegistry;

public final class DeadLettersClientHooks {
    private DeadLettersClientHooks() {
    }

    public static void openNoteScreen(String noteId) {
        if (noteId == null || noteId.isBlank()) {
            return;
        }
        NoteDefinition note = StoryRegistry.snapshot().notesById().get(noteId);
        if (note == null) {
            return;
        }
        Minecraft.getInstance().setScreen(new NoteScreen(note));
    }

    public static void openScrapbookAt(PlacedScrapbookBlockEntity blockEntity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || blockEntity == null) {
            return;
        }
        ScrapbookAnimationController.playOpen(mc.player, blockEntity);
        mc.setScreen(new ScrapbookScreen(ItemStack.EMPTY, blockEntity));
        DeadLettersNetwork.sendToServer(new RequestNotebookDataPacket());
    }

    /** Opens the archive from the scrapbook item (right-click in air / no block use). */
    public static void openScrapbookInHand(ItemStack scrapbookStack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || scrapbookStack.isEmpty()) {
            return;
        }
        ScrapbookAnimationController.playOpen(mc.player, scrapbookStack);
        mc.setScreen(new ScrapbookScreen(scrapbookStack, null));
        DeadLettersNetwork.sendToServer(new RequestNotebookDataPacket());
    }
}
