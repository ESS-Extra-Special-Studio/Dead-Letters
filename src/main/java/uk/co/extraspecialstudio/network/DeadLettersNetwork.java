package uk.co.extraspecialstudio.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class DeadLettersNetwork {
    public static final String PROTOCOL = "2";

    private DeadLettersNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar(PROTOCOL);
        reg.playToServer(AddNoteToNotebookPacket.TYPE, AddNoteToNotebookPacket.STREAM_CODEC, AddNoteToNotebookPacket::handle);
        reg.playToServer(RequestNotebookDataPacket.TYPE, RequestNotebookDataPacket.STREAM_CODEC, RequestNotebookDataPacket::handle);
        reg.playToClient(SyncNotebookDataPacket.TYPE, SyncNotebookDataPacket.STREAM_CODEC, SyncNotebookDataPacket::handle);
        reg.playToClient(ArchiveNoteAckPacket.TYPE, ArchiveNoteAckPacket.STREAM_CODEC, ArchiveNoteAckPacket::handle);
        reg.playToServer(RemoveNoteFromNotebookPacket.TYPE, RemoveNoteFromNotebookPacket.STREAM_CODEC, RemoveNoteFromNotebookPacket::handle);
        reg.playToClient(SyncStoryRegistryPacket.TYPE, SyncStoryRegistryPacket.STREAM_CODEC, SyncStoryRegistryPacket::handle);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToAllPlayers(CustomPacketPayload payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }
}
