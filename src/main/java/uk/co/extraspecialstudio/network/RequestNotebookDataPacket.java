package uk.co.extraspecialstudio.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.story.PlayerStoryData;

import java.util.ArrayList;
import java.util.List;

public record RequestNotebookDataPacket() implements CustomPacketPayload {
    public static final Type<RequestNotebookDataPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "request_notebook_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestNotebookDataPacket> STREAM_CODEC =
            StreamCodec.unit(new RequestNotebookDataPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestNotebookDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            List<String> entries = new ArrayList<>(PlayerStoryData.getNotebookEntriesInInsertionOrder(player));
            DeadLettersNetwork.sendToPlayer(player, new SyncNotebookDataPacket(entries));
        });
    }
}
