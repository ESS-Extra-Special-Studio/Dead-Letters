package uk.co.extraspecialstudio.story;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StoryRegistrySnapshot {
    private final Map<String, StoryDefinition> storiesById;
    private final Map<String, NoteDefinition> notesById;

    public StoryRegistrySnapshot(Map<String, StoryDefinition> storiesById, Map<String, NoteDefinition> notesById) {
        this.storiesById = Collections.unmodifiableMap(new LinkedHashMap<>(storiesById));
        this.notesById = Collections.unmodifiableMap(new LinkedHashMap<>(notesById));
    }

    public Map<String, StoryDefinition> storiesById() {
        return storiesById;
    }

    public Map<String, NoteDefinition> notesById() {
        return notesById;
    }

    /**
     * {@code CustomModelData} value 1…{@link StoryPageTextures#VARIANT_COUNT} for per-story note item icons.
     * Uses explicit {@code texture_slot} when provided; otherwise a stable hash of the story id.
     */
    public int storyTextureSlot(String storyId) {
        StoryDefinition story = storiesById.get(storyId);
        if (story != null && story.textureSlot() != null) {
            int raw = story.textureSlot();
            return Math.min(Math.max(raw, 1), StoryPageTextures.VARIANT_COUNT);
        }
        if (storyId == null || storyId.isBlank()) {
            return 1;
        }
        return Math.floorMod(storyId.hashCode(), StoryPageTextures.VARIANT_COUNT) + 1;
    }

    public String storyPageTexture(String storyId) {
        StoryDefinition story = storiesById.get(storyId);
        if (story == null) {
            return null;
        }
        String raw = story.pageTexture();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw;
    }

    public boolean storyPageTextureLined(String storyId) {
        StoryDefinition story = storiesById.get(storyId);
        if (story == null || story.pageTextureLined() == null) {
            return false;
        }
        return story.pageTextureLined();
    }
}
