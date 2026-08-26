package uk.co.extraspecialstudio.client;

import mod.azure.azurelib.common.render.block.AzBlockEntityRenderer;
import mod.azure.azurelib.common.render.block.AzBlockEntityRendererConfig;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.block.PlacedScrapbookBlockEntity;

public final class PlacedScrapbookRenderer extends AzBlockEntityRenderer<PlacedScrapbookBlockEntity> {
    private static final ResourceLocation SCRAPBOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "textures/item/scrapbook.png");

    public PlacedScrapbookRenderer() {
        super(AzBlockEntityRendererConfig.<PlacedScrapbookBlockEntity>builder(
                ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "geo/scrapbook.geo.json"),
                SCRAPBOOK_TEXTURE
        ).setAnimatorProvider(PlacedScrapbookAnimator::new)
                .setRenderType(RenderType.entityCutoutNoCull(SCRAPBOOK_TEXTURE))
                .build());
    }
}
