package uk.co.extraspecialstudio.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import uk.co.extraspecialstudio.item.NoteItem;
import uk.co.extraspecialstudio.registry.ModItems;
import uk.co.extraspecialstudio.story.NoteDefinition;
import uk.co.extraspecialstudio.story.PlayerStoryData;
import uk.co.extraspecialstudio.story.StoryRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record AddNoteToNotebookPacket(String noteId) {
    public static void encode(AddNoteToNotebookPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.noteId);
    }

    public static AddNoteToNotebookPacket decode(FriendlyByteBuf buffer) {
        return new AddNoteToNotebookPacket(buffer.readUtf());
    }

    public static void handle(AddNoteToNotebookPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || packet.noteId == null || packet.noteId.isBlank()) {
                return;
            }

            NoteDefinition note = StoryRegistry.snapshot().notesById().get(packet.noteId);
            if (note == null) {
                return;
            }

            if (PlayerStoryData.hasNotebookEntry(player, packet.noteId)) {
                return;
            }

            if (tryConsumeNoteFromCarried(player, packet.noteId)) {
                finishArchive(player, packet.noteId);
                return;
            }

            int slot = findNoteInInventory(player, packet.noteId);
            if (slot < 0) {
                return;
            }

            ItemStack stack = player.getInventory().getItem(slot);
            PlayerStoryData.addNotebookEntry(player, packet.noteId);
            stack.shrink(1);
            player.getInventory().setChanged();
            finishArchive(player, packet.noteId);
        });
        context.setPacketHandled(true);
    }

    private static boolean tryConsumeNoteFromCarried(ServerPlayer player, String noteId) {
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty() || !carried.is(ModItems.NOTE.get())) {
            return false;
        }
        if (!noteId.equals(carried.getOrCreateTag().getString(NoteItem.NOTE_ID_TAG))) {
            return false;
        }
        PlayerStoryData.addNotebookEntry(player, noteId);
        carried.shrink(1);
        player.containerMenu.setCarried(carried);
        player.containerMenu.broadcastChanges();
        player.getInventory().setChanged();
        return true;
    }

    private static void finishArchive(ServerPlayer player, String noteId) {
        DeadLettersNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ArchiveNoteAckPacket(noteId));
        List<String> entries = new ArrayList<>(PlayerStoryData.getNotebookEntriesInInsertionOrder(player));
        DeadLettersNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncNotebookDataPacket(entries));
    }

    private static int findNoteInInventory(ServerPlayer player, String noteId) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.is(ModItems.NOTE.get())) {
                continue;
            }
            String stackNoteId = stack.getOrCreateTag().getString(NoteItem.NOTE_ID_TAG);
            if (noteId.equals(stackNoteId)) {
                return i;
            }
        }
        return -1;
    }
}
