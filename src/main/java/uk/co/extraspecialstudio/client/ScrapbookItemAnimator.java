package uk.co.extraspecialstudio.client;

import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzItemAnimator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import uk.co.extraspecialstudio.Dead_letters;

public final class ScrapbookItemAnimator extends AzItemAnimator {
    public static final String CONTROLLER = "scrapbook_controller";

    @Override
    public void registerControllers(AzAnimationControllerContainer<ItemStack> container) {
        container.add(AzAnimationController.builder(this, CONTROLLER).build());
    }

    @Override
    public ResourceLocation getAnimationLocation(ItemStack animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "animations/scrapbook.animation.json");
    }
}
