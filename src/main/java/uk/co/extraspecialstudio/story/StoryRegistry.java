package uk.co.extraspecialstudio.story;

import java.util.Map;

public final class StoryRegistry {
    private static volatile StoryRegistrySnapshot snapshot = new StoryRegistrySnapshot(Map.of(), Map.of());

    private StoryRegistry() {
    }

    public static StoryRegistrySnapshot snapshot() {
        return snapshot;
    }

    public static void replace(StoryRegistrySnapshot nextSnapshot) {
        snapshot = nextSnapshot;
    }
}
