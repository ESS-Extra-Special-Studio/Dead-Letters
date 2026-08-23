package uk.co.extraspecialstudio.story;

import org.jetbrains.annotations.Nullable;

public record StoryDefinition(
        String id,
        String name,
        int maxOrder,
        int weight,
        @Nullable Integer textureSlot,
        @Nullable String pageTexture,
        @Nullable Boolean pageTextureLined,
        String source
) {
}
