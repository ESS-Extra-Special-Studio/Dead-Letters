package uk.co.extraspecialstudio.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.item.NoteItem;
import uk.co.extraspecialstudio.story.NoteDefinition;
import uk.co.extraspecialstudio.story.PlayerStoryData;
import uk.co.extraspecialstudio.story.StoryRegistry;

import java.util.ArrayList;
import java.util.List;

public record RemoveNoteFromNotebookPacket(String noteId) implements CustomPacketPayload {
    public static final Type<RemoveNoteFromNotebookPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "remove_note_from_notebook"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveNoteFromNotebookPacket> STREAM_CODEC =
            StreamCodec.of(RemoveNoteFromNotebookPacket::encode, RemoveNoteFromNotebookPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, RemoveNoteFromNotebookPacket packet) {
        buffer.writeUtf(packet.noteId);
    }

    private static RemoveNoteFromNotebookPacket decode(RegistryFriendlyByteBuf buffer) {
        return new RemoveNoteFromNotebookPacket(buffer.readUtf());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RemoveNoteFromNotebookPacket packet, IPayloadContext context) {
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
            DeadLettersNetwork.sendToPlayer(player, new SyncNotebookDataPacket(entries));
        });
    }
}
