package uk.co.extraspecialstudio.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import uk.co.extraspecialstudio.Dead_letters;

import java.util.Optional;

public final class DeadLettersNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static boolean initialized;

    private DeadLettersNetwork() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        CHANNEL.registerMessage(packetId++, AddNoteToNotebookPacket.class,
                AddNoteToNotebookPacket::encode,
                AddNoteToNotebookPacket::decode,
                AddNoteToNotebookPacket::handle);

        CHANNEL.registerMessage(packetId++, RequestNotebookDataPacket.class,
                RequestNotebookDataPacket::encode,
                RequestNotebookDataPacket::decode,
                RequestNotebookDataPacket::handle);

        CHANNEL.registerMessage(packetId++, SyncNotebookDataPacket.class,
                SyncNotebookDataPacket::encode,
                SyncNotebookDataPacket::decode,
                SyncNotebookDataPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(packetId++, ArchiveNoteAckPacket.class,
                ArchiveNoteAckPacket::encode,
                ArchiveNoteAckPacket::decode,
                ArchiveNoteAckPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(packetId++, RemoveNoteFromNotebookPacket.class,
                RemoveNoteFromNotebookPacket::encode,
                RemoveNoteFromNotebookPacket::decode,
                RemoveNoteFromNotebookPacket::handle);
    }
}
