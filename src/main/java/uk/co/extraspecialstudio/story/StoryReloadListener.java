package uk.co.extraspecialstudio.story;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.loading.FMLPaths;
import uk.co.extraspecialstudio.Config;
import uk.co.extraspecialstudio.Dead_letters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StoryReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    public static final StoryReloadListener INSTANCE = new StoryReloadListener();
    private static final Pattern PART_FILE_PATTERN = Pattern.compile("part_(\\d+)\\.(txt|json)$");

    private StoryReloadListener() {
        super(GSON, "deadletters/stories");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> datapackFiles, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, StoryDefinition> mergedStories = new LinkedHashMap<>();
        Map<String, NoteDefinition> mergedNotes = new LinkedHashMap<>();

        loadDatapackStories(datapackFiles, mergedStories, mergedNotes);
        if (Config.customStoriesEnable) {
            loadConfigStories(mergedStories, mergedNotes);
        }

        StoryRegistry.replace(new StoryRegistrySnapshot(mergedStories, mergedNotes));
        Dead_letters.LOGGER.info("Dead Letters registry ready: {} stories, {} notes", mergedStories.size(), mergedNotes.size());
    }

    private void loadDatapackStories(Map<ResourceLocation, JsonElement> datapackFiles, Map<String, StoryDefinition> stories, Map<String, NoteDefinition> notes) {
        for (Map.Entry<ResourceLocation, JsonElement> entry : datapackFiles.entrySet()) {
            String path = entry.getKey().getPath();
            // SimpleJsonResourceReloadListener returns paths relative to the configured directory,
            // typically "story_id/story" and "story_id/notes/part_n".
            String relative = path.startsWith("deadletters/stories/")
                    ? path.substring("deadletters/stories/".length())
                    : path;
            String source = "datapack:" + entry.getKey();

            if (relative.endsWith("/story")) {
                StoryDefinition story = parseStoryDefinition(entry.getValue().getAsJsonObject(), source);
                if (story != null) {
                    stories.put(story.id(), story);
                }
                continue;
            }

            if (relative.contains("/notes/")) {
                NoteDefinition note = parseNoteDefinition(entry.getValue().getAsJsonObject(), source);
                if (note != null) {
                    notes.put(note.id(), note);
                }
            }
        }
    }

    private void loadConfigStories(Map<String, StoryDefinition> stories, Map<String, NoteDefinition> notes) {
        Path customStoriesRoot = FMLPaths.CONFIGDIR.get().resolve(Config.customStoriesPath).normalize();
        if (!Files.exists(customStoriesRoot) || !Files.isDirectory(customStoriesRoot)) {
            return;
        }

        try {
            List<Path> storyFolders = Files.list(customStoriesRoot)
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();

            for (Path storyFolder : storyFolders) {
                loadSingleConfigStoryFolder(storyFolder, stories, notes);
            }
        } catch (IOException exception) {
            Dead_letters.LOGGER.error("Failed reading custom stories root {}", customStoriesRoot, exception);
        }
    }

    private void loadSingleConfigStoryFolder(Path storyFolder, Map<String, StoryDefinition> stories, Map<String, NoteDefinition> notes) {
        Path storyFile = storyFolder.resolve("story.json");
        if (!Files.exists(storyFile)) {
            Dead_letters.LOGGER.warn("Skipping custom story folder without story.json: {}", storyFolder);
            return;
        }

        try {
            JsonObject storyJson = JsonParser.parseString(Files.readString(storyFile)).getAsJsonObject();
            StoryDefinition story = parseStoryDefinition(storyJson, "config:" + storyFile);
            if (story == null) {
                return;
            }

            if (!Config.customStoriesAllowOverrides && stories.containsKey(story.id())) {
                Dead_letters.LOGGER.warn("Skipping custom story override for {}", story.id());
                return;
            }

            Map<Integer, NoteDefinition> orderedNotes = new LinkedHashMap<>();
            List<Path> partFiles = Files.list(storyFolder)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("part_"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();

            for (Path partFile : partFiles) {
                String fileName = partFile.getFileName().toString();
                Matcher matcher = PART_FILE_PATTERN.matcher(fileName);
                if (!matcher.matches()) {
                    continue;
                }

                int order = Integer.parseInt(matcher.group(1));
                String extension = matcher.group(2);
                NoteDefinition note = "txt".equals(extension)
                        ? parseTxtPart(story.id(), order, partFile)
                        : parseJsonPart(story.id(), order, partFile);
                if (note != null) {
                    orderedNotes.put(order, note);
                }
            }

            validateAndPublishStory(story, orderedNotes, stories, notes);
        } catch (IOException exception) {
            Dead_letters.LOGGER.error("Failed loading custom story folder {}", storyFolder, exception);
        }
    }

    private void validateAndPublishStory(StoryDefinition story, Map<Integer, NoteDefinition> orderedNotes, Map<String, StoryDefinition> stories, Map<String, NoteDefinition> notes) {
        if (!orderedNotes.containsKey(1)) {
            Dead_letters.LOGGER.warn("Custom story '{}' skipped because part_1 is missing", story.id());
            return;
        }

        int maxFound = orderedNotes.keySet().stream().max(Integer::compareTo).orElse(0);
        for (int i = 1; i <= maxFound; i++) {
            if (!orderedNotes.containsKey(i)) {
                Dead_letters.LOGGER.warn("Custom story '{}' skipped because part_{} is missing", story.id(), i);
                return;
            }
        }

        stories.put(story.id(), story);
        for (NoteDefinition note : orderedNotes.values()) {
            notes.put(note.id(), note);
        }

        if (Config.customStoriesLogLoading) {
            Dead_letters.LOGGER.info("Loaded custom story '{}' with {} parts", story.id(), orderedNotes.size());
        }
    }

    private StoryDefinition parseStoryDefinition(JsonObject json, String source) {
        if (!json.has("id")) {
            Dead_letters.LOGGER.warn("Skipping story with missing id from {}", source);
            return null;
        }

        String id = json.get("id").getAsString();
        String name = json.has("name") ? json.get("name").getAsString() : id;
        int maxOrder = json.has("max_order") ? json.get("max_order").getAsInt() : 1;
        int weight = json.has("weight") ? json.get("weight").getAsInt() : 1;
        Integer textureSlot = json.has("texture_slot") ? json.get("texture_slot").getAsInt() : null;
        String pageTexture = json.has("page_texture") ? json.get("page_texture").getAsString() : null;
        Boolean pageTextureLined = json.has("page_texture_lined") ? json.get("page_texture_lined").getAsBoolean() : null;
        return new StoryDefinition(id, name, maxOrder, weight, textureSlot, pageTexture, pageTextureLined, source);
    }

    private NoteDefinition parseNoteDefinition(JsonObject json, String source) {
        if (!json.has("id") || !json.has("story") || !json.has("order") || !json.has("title") || !json.has("body")) {
            Dead_letters.LOGGER.warn("Skipping note with missing required fields from {}", source);
            return null;
        }

        String id = json.get("id").getAsString();
        String storyId = json.get("story").getAsString();
        int order = json.get("order").getAsInt();
        String title = json.get("title").getAsString();
        int weight = json.has("weight") ? json.get("weight").getAsInt() : 1;
        List<String> body = jsonArrayToStringList(json.getAsJsonArray("body"));
        return new NoteDefinition(id, storyId, order, title, body, weight, source);
    }

    private NoteDefinition parseTxtPart(String storyId, int order, Path partFile) {
        try {
            List<String> lines = Files.readAllLines(partFile, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                Dead_letters.LOGGER.warn("Skipping empty custom part file {}", partFile);
                return null;
            }

            String title = lines.get(0).trim();
            List<String> body = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (!line.isEmpty()) {
                    body.add(line);
                }
            }

            String noteId = storyId + "_" + order;
            return new NoteDefinition(noteId, storyId, order, title.isEmpty() ? "Untitled" : title, body, 1, "config:" + partFile);
        } catch (IOException exception) {
            Dead_letters.LOGGER.error("Failed parsing text part {}", partFile, exception);
            return null;
        }
    }

    private NoteDefinition parseJsonPart(String storyId, int order, Path partFile) {
        try {
            JsonObject json = JsonParser.parseString(Files.readString(partFile)).getAsJsonObject();
            String title = json.has("title") ? json.get("title").getAsString() : "Untitled";
            int weight = json.has("weight") ? json.get("weight").getAsInt() : 1;
            List<String> body = json.has("body") ? jsonArrayToStringList(json.getAsJsonArray("body")) : List.of();
            String noteId = json.has("id") ? json.get("id").getAsString() : storyId + "_" + order;
            return new NoteDefinition(noteId, storyId, order, title, body, weight, "config:" + partFile);
        } catch (IOException exception) {
            Dead_letters.LOGGER.error("Failed parsing json part {}", partFile, exception);
            return null;
        }
    }

    private List<String> jsonArrayToStringList(JsonArray array) {
        List<String> lines = new ArrayList<>();
        for (JsonElement element : array) {
            lines.add(element.getAsString());
        }
        return lines;
    }
}
