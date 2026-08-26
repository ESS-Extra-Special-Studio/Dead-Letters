package uk.co.extraspecialstudio.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.item.NoteItem;
import uk.co.extraspecialstudio.item.NoteNbt;
import uk.co.extraspecialstudio.registry.ModItems;
import uk.co.extraspecialstudio.story.NoteDefinition;
import uk.co.extraspecialstudio.story.PlayerStoryData;
import uk.co.extraspecialstudio.story.StoryRegistry;

import java.util.ArrayList;
import java.util.List;

public record AddNoteToNotebookPacket(String noteId) implements CustomPacketPayload {
    public static final Type<AddNoteToNotebookPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "add_note_to_notebook"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AddNoteToNotebookPacket> STREAM_CODEC =
            StreamCodec.of(AddNoteToNotebookPacket::encode, AddNoteToNotebookPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, AddNoteToNotebookPacket packet) {
        buffer.writeUtf(packet.noteId);
    }

    private static AddNoteToNotebookPacket decode(RegistryFriendlyByteBuf buffer) {
        return new AddNoteToNotebookPacket(buffer.readUtf());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AddNoteToNotebookPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (packet.noteId == null || packet.noteId.isBlank()) {
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
    }

    private static boolean tryConsumeNoteFromCarried(ServerPlayer player, String noteId) {
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty() || !carried.is(ModItems.NOTE.get())) {
            return false;
        }
        if (!noteId.equals(NoteNbt.getNoteId(carried))) {
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
        DeadLettersNetwork.sendToPlayer(player, new ArchiveNoteAckPacket(noteId));
        List<String> entries = new ArrayList<>(PlayerStoryData.getNotebookEntriesInInsertionOrder(player));
        DeadLettersNetwork.sendToPlayer(player, new SyncNotebookDataPacket(entries));
    }

    private static int findNoteInInventory(ServerPlayer player, String noteId) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.is(ModItems.NOTE.get())) {
                continue;
            }
            if (noteId.equals(NoteNbt.getNoteId(stack))) {
                return i;
            }
        }
        return -1;
    }
}
