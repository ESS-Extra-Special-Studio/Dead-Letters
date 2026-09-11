package uk.co.extraspecialstudio.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.story.StoryRegistry;

@Mod.EventBusSubscriber(modid = Dead_letters.MODID)
public final class StoryRegistrySync {
    private StoryRegistrySync() {
    }

    public static void sendTo(ServerPlayer player) {
        DeadLettersNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                SyncStoryRegistryPacket.fromSnapshot(StoryRegistry.snapshot()));
    }

    public static void sendToAll() {
        DeadLettersNetwork.CHANNEL.send(
                PacketDistributor.ALL.noArg(),
                SyncStoryRegistryPacket.fromSnapshot(StoryRegistry.snapshot()));
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
