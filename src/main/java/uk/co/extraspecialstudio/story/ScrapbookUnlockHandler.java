package uk.co.extraspecialstudio.story;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import uk.co.extraspecialstudio.registry.ModItems;

public final class ScrapbookUnlockHandler {
    private ScrapbookUnlockHandler() {
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getStack().is(ModItems.SCRAPBOOK.get())) {
            PlayerStoryData.markScrapbookUnlocked(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 40 != 0) {
            return;
        }
        if (PlayerStoryData.isScrapbookUnlocked(player)) {
            return;
        }
        if (PlayerStoryData.hasScrapbookInInventory(player)) {
            PlayerStoryData.markScrapbookUnlocked(player);
        }
    }
}
