package uk.co.extraspecialstudio.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.story.StoryRegistry;

@EventBusSubscriber(modid = Dead_letters.MODID)
public final class StoryRegistrySync {
    private StoryRegistrySync() {
    }

    public static void sendTo(ServerPlayer player) {
        DeadLettersNetwork.sendToPlayer(player, SyncStoryRegistryPacket.fromSnapshot(StoryRegistry.snapshot()));
    }

    public static void sendToAll() {
        DeadLettersNetwork.sendToAllPlayers(SyncStoryRegistryPacket.fromSnapshot(StoryRegistry.snapshot()));
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendTo(player);
        }
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            sendTo(event.getPlayer());
            return;
        }
        sendToAll();
    }
}
