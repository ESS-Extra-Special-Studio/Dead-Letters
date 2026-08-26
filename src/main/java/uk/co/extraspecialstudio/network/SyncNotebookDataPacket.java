package uk.co.extraspecialstudio.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.client.ScrapbookScreen;

import java.util.ArrayList;
import java.util.List;

public record SyncNotebookDataPacket(List<String> noteIds) implements CustomPacketPayload {
    public static final Type<SyncNotebookDataPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "sync_notebook_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncNotebookDataPacket> STREAM_CODEC =
            StreamCodec.of(SyncNotebookDataPacket::encode, SyncNotebookDataPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, SyncNotebookDataPacket packet) {
        buffer.writeVarInt(packet.noteIds.size());
        for (String noteId : packet.noteIds) {
            buffer.writeUtf(noteId);
        }
    }

    private static SyncNotebookDataPacket decode(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<String> noteIds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            noteIds.add(buffer.readUtf());
        }
        return new SyncNotebookDataPacket(noteIds);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncNotebookDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof ScrapbookScreen scrapbookScreen) {
                scrapbookScreen.updateNotebookEntries(packet.noteIds);
            }
        });
    }
}
