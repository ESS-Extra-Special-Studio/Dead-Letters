package uk.co.extraspecialstudio.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.client.DeadLettersClientHooks;

/**
 * Server tells the client an "Add to Notebook" succeeded so the note UI can close and inventory refresh applies.
 */
public record ArchiveNoteAckPacket(String noteId) implements CustomPacketPayload {
    public static final Type<ArchiveNoteAckPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "archive_note_ack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArchiveNoteAckPacket> STREAM_CODEC =
            StreamCodec.of(ArchiveNoteAckPacket::encode, ArchiveNoteAckPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, ArchiveNoteAckPacket packet) {
        buffer.writeUtf(packet.noteId);
    }

    private static ArchiveNoteAckPacket decode(RegistryFriendlyByteBuf buffer) {
        return new ArchiveNoteAckPacket(buffer.readUtf());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ArchiveNoteAckPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                DeadLettersClientHooks.applyArchiveNoteAck(packet.noteId);
            }
        });
    }
}
