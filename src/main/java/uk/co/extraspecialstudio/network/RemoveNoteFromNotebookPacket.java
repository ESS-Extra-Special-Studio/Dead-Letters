package uk.co.extraspecialstudio.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import uk.co.extraspecialstudio.item.NoteItem;
import uk.co.extraspecialstudio.story.NoteDefinition;
import uk.co.extraspecialstudio.story.PlayerStoryData;
import uk.co.extraspecialstudio.story.StoryRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record RemoveNoteFromNotebookPacket(String noteId) {
    public static void encode(RemoveNoteFromNotebookPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.noteId);
    }

    public static RemoveNoteFromNotebookPacket decode(FriendlyByteBuf buffer) {
        return new RemoveNoteFromNotebookPacket(buffer.readUtf());
    }

    public static void handle(RemoveNoteFromNotebookPacket packet, Supplier<NetworkEvent.Context> supplier) {
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
            if (!PlayerStoryData.removeNotebookEntry(player, packet.noteId)) {
                return;
            }

            ItemStack restored = NoteItem.createStack(note);
            if (!player.getInventory().add(restored)) {
                ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), restored);
                drop.setPickUpDelay(0);
                player.level().addFreshEntity(drop);
            }
            player.getInventory().setChanged();
            List<String> entries = new ArrayList<>(PlayerStoryData.getNotebookEntriesInInsertionOrder(player));
            DeadLettersNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncNotebookDataPacket(entries));
        });
        context.setPacketHandled(true);
    }
}
