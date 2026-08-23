package uk.co.extraspecialstudio.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import uk.co.extraspecialstudio.client.ScrapbookScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SyncNotebookDataPacket(List<String> noteIds) {
    public static void encode(SyncNotebookDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.noteIds.size());
        for (String noteId : packet.noteIds) {
            buffer.writeUtf(noteId);
        }
    }

    public static SyncNotebookDataPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<String> noteIds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            noteIds.add(buffer.readUtf());
        }
        return new SyncNotebookDataPacket(noteIds);
    }

    public static void handle(SyncNotebookDataPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof ScrapbookScreen scrapbookScreen) {
                scrapbookScreen.updateNotebookEntries(packet.noteIds);
            }
        });
        context.setPacketHandled(true);
    }
}
