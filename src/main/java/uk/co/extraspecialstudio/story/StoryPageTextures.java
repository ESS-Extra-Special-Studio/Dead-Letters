package uk.co.extraspecialstudio.story;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import uk.co.extraspecialstudio.Dead_letters;

/**
 * Per-story parchment variants (T-01 = first story’s parts, T-02 = second story, …).
 * Textures live under {@code assets/dead_letters/textures/item/story_pages/}.
 * Custom stories can later pick a variant via datapack/config; until then use {@link #forStoryOrdinal(int)}.
 */
public final class StoryPageTextures {
    /** T-01 … T-06 on disk ({@code story_page_t01.png} …); use first five for gameplay. */
    public static final int VARIANT_COUNT = 6;
    /** Parchment blit width/height in pixels (matches {@link uk.co.extraspecialstudio.client.NoteScreen} GUI scale). */
    public static final int NOTE_PAGE_TEX_W = 163;
    public static final int NOTE_PAGE_TEX_H = 216;

    private StoryPageTextures() {
    }

    /**
     * @param ordinal 1-based index among stories (first story in sorted order = 1 → T-01).
     */
    public static ResourceLocation forStoryOrdinal(int ordinal) {
        int slot = Math.min(Math.max(ordinal, 1), VARIANT_COUNT);
        return ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, texturePath(slot));
    }

    private static String texturePath(int slot1To6) {
        return "item/story_pages/story_page_t" + String.format("%02d", slot1To6);
    }

    /**
     * Full path for {@link net.minecraft.client.gui.GuiGraphics#blit(net.minecraft.resources.ResourceLocation, int, int, int, int, int, int)}.
     */
    public static ResourceLocation blitTextureForStory(String storyId, StoryRegistrySnapshot snapshot) {
        ResourceLocation explicit = explicitPageTextureForStory(storyId, snapshot);
        if (explicit != null) {
            return explicit;
        }
        int slot = snapshot.storyTextureSlot(storyId);
        return ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "textures/item/story_pages/story_page_t" + String.format("%02d", slot) + ".png");
    }

    @Nullable
    private static ResourceLocation explicitPageTextureForStory(String storyId, StoryRegistrySnapshot snapshot) {
        String raw = snapshot.storyPageTexture(storyId);
        if (raw == null) {
            return null;
        }
        try {
            ResourceLocation parsed = ResourceLocation.tryParse(raw);
            if (parsed == null) {
                Dead_letters.LOGGER.warn("Ignoring invalid page_texture '{}' for story '{}'", raw, storyId);
            }
            return parsed;
        } catch (RuntimeException ex) {
            Dead_letters.LOGGER.warn("Ignoring invalid page_texture '{}' for story '{}'", raw, storyId);
            return null;
        }
    }

    /** GUI parchment when no per-story texture exists or for non-story pages (index, guide). */
    public static ResourceLocation guiFallbackTexture() {
        return ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "textures/gui/note_page_clean.png");
    }

    /**
     * Same parchment the note reader uses: per-story {@link #blitTextureForStory} when the file is present,
     * otherwise {@link #guiFallbackTexture()}. Non-story pages pass {@code null} or blank {@code storyId}.
     */
    public static ResourceLocation resolveGuiPageTexture(@Nullable String storyId, StoryRegistrySnapshot snapshot, ResourceManager resourceManager) {
        if (storyId == null || storyId.isEmpty()) {
            return guiFallbackTexture();
        }
        ResourceLocation candidate = blitTextureForStory(storyId, snapshot);
        if (resourceManager.getResource(candidate).isPresent()) {
            return candidate;
        }
        return guiFallbackTexture();
    }

    /**
     * Lined “school paper” story sheets ({@code textures/item/story_pages/story_page_t##.png}) use tighter margins
     * and rule-based vertical rhythm; the clean GUI parchment does not.
     */
    public static boolean isLinedStoryParchment(ResourceLocation texture) {
        return Dead_letters.MODID.equals(texture.getNamespace())
                && texture.getPath().startsWith("textures/item/story_pages/story_page_t")
                && texture.getPath().endsWith(".png");
    }

    /**
     * Story-level override for custom textures that follow lined-paper geometry but do not match
     * {@link #isLinedStoryParchment(ResourceLocation)} naming.
     */
    public static ParchmentTextLayout layoutForStoryPage(@Nullable String storyId, ResourceLocation texture, StoryRegistrySnapshot snapshot, float scale) {
        if (storyId != null && !storyId.isBlank() && snapshot.storyPageTextureLined(storyId)) {
            return ParchmentTextLayout.lined(scale);
        }
        return ParchmentTextLayout.forTexture(texture, scale);
    }

    /**
     * Text column metrics: first three fields are inset from the parchment texture edges (texture pixels);
     * {@code lineStepScreen} is screen pixels between baselines (includes {@code scale} for lined pages).
     */
    public record ParchmentTextLayout(int marginLeftTex, int marginTopTex, int marginRightTex, int lineStepScreen) {
        private static final int CLEAN_LEFT = 38;
        private static final int CLEAN_TOP = 38;
        private static final int CLEAN_RIGHT = 34;
        private static final int CLEAN_LINE_STEP_SCREEN = 11;

        /** Past the pink margin rule; tweak with the texture if art changes. */
        private static final int LINED_LEFT_TEX = 47;
        /** First baseline on the first blue rule; tweak with the texture if art changes. */
        private static final int LINED_TOP_TEX = 41;
        private static final int LINED_RIGHT_TEX = 28;
        /** Vertical distance between blue rules on the lined texture (texture pixels). */
        private static final float LINED_RULE_PERIOD_TEX = 12F;

        public static ParchmentTextLayout lined(float scale) {
            int step = Math.max(9, (int) Math.round(LINED_RULE_PERIOD_TEX * scale));
            return new ParchmentTextLayout(LINED_LEFT_TEX, LINED_TOP_TEX, LINED_RIGHT_TEX, step);
        }

        public static ParchmentTextLayout forTexture(ResourceLocation texture, float scale) {
            if (isLinedStoryParchment(texture)) {
                return lined(scale);
            }
            return new ParchmentTextLayout(CLEAN_LEFT, CLEAN_TOP, CLEAN_RIGHT, CLEAN_LINE_STEP_SCREEN);
        }

        public int innerWidthTex() {
            return NOTE_PAGE_TEX_W - marginLeftTex - marginRightTex;
        }

        public int innerWidthPixels(float scale) {
            return Math.max(40, (int) (innerWidthTex() * scale));
        }
    }
}
