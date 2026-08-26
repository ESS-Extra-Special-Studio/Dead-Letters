package uk.co.extraspecialstudio.client;

import mod.azure.azurelib.common.render.item.AzItemRenderer;
import mod.azure.azurelib.common.render.item.AzItemRendererConfig;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import uk.co.extraspecialstudio.Dead_letters;

public final class ScrapbookItemRenderer extends AzItemRenderer {
    private static final ResourceLocation SCRAPBOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "textures/item/scrapbook.png");

    public ScrapbookItemRenderer() {
        super(AzItemRendererConfig.builder(
                ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "geo/scrapbook.geo.json"),
                SCRAPBOOK_TEXTURE
        ).setAnimatorProvider(ScrapbookItemAnimator::new)
                .setRenderType(RenderType.entityCutoutNoCull(SCRAPBOOK_TEXTURE))
                .build());
    }
}
