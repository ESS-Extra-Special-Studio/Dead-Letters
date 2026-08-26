package uk.co.extraspecialstudio.client;

import mod.azure.azurelib.common.animation.AzAnimatorConfig;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzBlockAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.block.PlacedScrapbookBlockEntity;

public final class PlacedScrapbookAnimator extends AzBlockAnimator<PlacedScrapbookBlockEntity> {
    public PlacedScrapbookAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<PlacedScrapbookBlockEntity> container) {
        container.add(AzAnimationController.builder(this, ScrapbookItemAnimator.CONTROLLER).build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(PlacedScrapbookBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "animations/scrapbook.animation.json");
    }
}
