package uk.co.extraspecialstudio.story;

import java.util.List;

public record NoteDefinition(String id, String storyId, int order, String title, List<String> body, int weight, String source) {
}
