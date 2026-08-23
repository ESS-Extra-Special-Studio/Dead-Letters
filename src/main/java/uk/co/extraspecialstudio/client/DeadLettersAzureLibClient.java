package uk.co.extraspecialstudio.client;

import mod.azure.azurelib.render.item.AzItemRendererRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.registry.ModBlockEntities;
import uk.co.extraspecialstudio.registry.ModItems;

@Mod.EventBusSubscriber(modid = Dead_letters.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
