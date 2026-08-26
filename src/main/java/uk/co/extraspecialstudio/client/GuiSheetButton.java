package uk.co.extraspecialstudio.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Two-row sprite-sheet button (20×40 PNG: normal row + hover row).
 * NeoForge 1.21 {@code WidgetSprites} expects registered GUI sprites, not raw {@code textures/gui/*.png} paths.
 */
public final class GuiSheetButton extends AbstractWidget {
    private static final int SHEET_W = 20;
    private static final int SHEET_H = 40;
    private static final int ROW_H = 20;

    private final ResourceLocation texture;
    private final Runnable onPress;

    public GuiSheetButton(
        int x,
        int y,
        int size,
        ResourceLocation texture,
        Runnable onPress,
        Component message
    ) {
        super(x, y, size, size, message);
        this.texture = texture;
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        int v = (this.active && isHovered()) ? ROW_H : 0;
        graphics.blit(texture, getX(), getY(), 0, v, width, height, SHEET_W, SHEET_H);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (this.active) {
            this.onPress.run();
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
