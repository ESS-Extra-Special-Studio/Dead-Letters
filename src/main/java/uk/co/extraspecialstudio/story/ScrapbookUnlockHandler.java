package uk.co.extraspecialstudio.story;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import uk.co.extraspecialstudio.registry.ModItems;

public final class ScrapbookUnlockHandler {
    private ScrapbookUnlockHandler() {
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getCurrentStack().is(ModItems.SCRAPBOOK.get()) || event.getOriginalStack().is(ModItems.SCRAPBOOK.get())) {
            PlayerStoryData.markScrapbookUnlocked(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
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
