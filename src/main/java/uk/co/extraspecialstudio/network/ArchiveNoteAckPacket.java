package uk.co.extraspecialstudio.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import uk.co.extraspecialstudio.client.NoteScreen;

import java.util.function.Supplier;

/**
 * Server tells the client an "Add to Notebook" succeeded so the note UI can close and inventory refresh applies.
 */
public record ArchiveNoteAckPacket(String noteId) {
    public static void encode(ArchiveNoteAckPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.noteId);
    }

    public static ArchiveNoteAckPacket decode(FriendlyByteBuf buffer) {
        return new ArchiveNoteAckPacket(buffer.readUtf());
    }

    public static void handle(ArchiveNoteAckPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof NoteScreen noteScreen && packet.noteId.equals(noteScreen.getNoteId())) {
                mc.setScreen(null);
            }
        });
        context.setPacketHandled(true);
    }
}
