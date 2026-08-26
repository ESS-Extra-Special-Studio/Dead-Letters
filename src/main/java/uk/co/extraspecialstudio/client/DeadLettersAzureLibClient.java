package uk.co.extraspecialstudio.client;

import mod.azure.azurelib.common.render.item.AzItemRendererRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.registry.ModBlockEntities;
import uk.co.extraspecialstudio.registry.ModItems;

@EventBusSubscriber(modid = Dead_letters.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DeadLettersAzureLibClient {
    private DeadLettersAzureLibClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> AzItemRendererRegistry.register(ModItems.SCRAPBOOK.get(), ScrapbookItemRenderer::new));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.PLACED_SCRAPBOOK.get(), ctx -> new PlacedScrapbookRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.PLACED_NOTE.get(), PlacedNoteRenderer::new);
    }
}
