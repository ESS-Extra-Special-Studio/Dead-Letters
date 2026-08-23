package uk.co.extraspecialstudio.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import uk.co.extraspecialstudio.story.PlayerStoryData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class RequestNotebookDataPacket {
    public static void encode(RequestNotebookDataPacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestNotebookDataPacket decode(FriendlyByteBuf buffer) {
        return new RequestNotebookDataPacket();
    }

    public static void handle(RequestNotebookDataPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            List<String> entries = new ArrayList<>(PlayerStoryData.getNotebookEntriesInInsertionOrder(player));
            DeadLettersNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncNotebookDataPacket(entries));
        });
        context.setPacketHandled(true);
    }
}
