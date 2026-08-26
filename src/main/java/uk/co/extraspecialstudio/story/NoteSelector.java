package uk.co.extraspecialstudio.story;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import uk.co.extraspecialstudio.Config;
import uk.co.extraspecialstudio.Dead_letters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NoteSelector {
    private NoteSelector() {
    }

    public static NoteDefinition selectNext(ServerPlayer player, RandomSource random) {
        return selectNextExcluding(player, random, Collections.emptySet());
    }

    /**
     * Weighted pick among literal "part 1" notes the player can still receive (progression still on part 1 for that story).
     */
    public static NoteDefinition selectEligiblePartOneExcluding(ServerPlayer player, RandomSource random, Set<String> batchExclude) {
        StoryRegistrySnapshot snapshot = StoryRegistry.snapshot();
        List<WeightedNote> pool = buildPartOneEligiblePool(player, snapshot, batchExclude, random);
        if (pool.isEmpty()) {
            return null;
        }
        int totalWeight = 0;
        for (WeightedNote weighted : pool) {
            totalWeight += weighted.weight();
        }
        int pick = random.nextInt(totalWeight);
        int cursor = 0;
        for (WeightedNote weighted : pool) {
            cursor += weighted.weight();
            if (pick < cursor) {
                return weighted.note();
            }
        }
        return null;
    }

    /**
     * Picks one weighted note, excluding ids in {@code batchExclude} (same-chest duplicate guard).
     */
    public static NoteDefinition selectNextExcluding(ServerPlayer player, RandomSource random, Set<String> batchExclude) {
        StoryRegistrySnapshot snapshot = StoryRegistry.snapshot();
        List<WeightedNote> pool = buildValidPool(player, snapshot, batchExclude, random);
        if (pool.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (WeightedNote weighted : pool) {
            totalWeight += weighted.weight();
        }

        int pick = random.nextInt(totalWeight);
        int cursor = 0;
        for (WeightedNote weighted : pool) {
            cursor += weighted.weight();
            if (pick < cursor) {
                return weighted.note();
            }
        }
        return null;
    }

    public static List<WeightedNote> buildValidPool(ServerPlayer player, StoryRegistrySnapshot snapshot) {
        return buildValidPool(player, snapshot, Collections.emptySet(), player.getRandom());
    }

    public static List<WeightedNote> buildValidPool(ServerPlayer player, StoryRegistrySnapshot snapshot, Set<String> batchExclude) {
        return buildValidPool(player, snapshot, batchExclude, player.getRandom());
    }

    public static List<WeightedNote> buildValidPool(ServerPlayer player, StoryRegistrySnapshot snapshot, Set<String> batchExclude, RandomSource random) {
        Set<String> discovered = PlayerStoryData.getDiscoveredSet(player);
        Set<String> acquired = PlayerStoryData.getAcquiredSet(player);
        Set<String> batch = batchExclude == null ? Collections.emptySet() : batchExclude;
        List<WeightedNote> pool = new ArrayList<>();
        Map<String, Integer> maxPartByStory = computeMaxPartByStory(snapshot);

        for (StoryDefinition story : snapshot.storiesById().values()) {
            int targetOrder = Config.useProgression ? PlayerStoryData.getProgressionLevel(player, story.id()) : 1;
            int maxOrder = Math.max(1, maxPartByStory.getOrDefault(story.id(), 1));
            for (NoteDefinition note : snapshot.notesById().values()) {
                if (!note.storyId().equals(story.id())) {
                    continue;
                }
                if (note.order() != targetOrder) {
                    continue;
                }
                if (batch.contains(note.id())) {
                    continue;
                }
                if (!Config.allowDuplicates) {
                    if (discovered.contains(note.id()) || acquired.contains(note.id())) {
                        continue;
                    }
                }
                int baseWeight = Math.max(1, story.weight()) * Math.max(1, note.weight());
                double partDropWeight = calculatePartDropWeight(note.order(), maxOrder);
                int weight = Math.max(1, (int) Math.round(baseWeight * partDropWeight * 100.0D));
                pool.add(new WeightedNote(note, weight));
            }
        }

        pool = restrictToSingleStoryIfConfigured(pool, random);

        if (Config.debugLogLootDecisions) {
            Dead_letters.LOGGER.info("Dead Letters pool for {} has {} candidates", player.getScoreboardName(), pool.size());
        }

        return pool;
    }

    private static List<WeightedNote> buildPartOneEligiblePool(ServerPlayer player, StoryRegistrySnapshot snapshot, Set<String> batchExclude, RandomSource random) {
        Set<String> discovered = PlayerStoryData.getDiscoveredSet(player);
        Set<String> acquired = PlayerStoryData.getAcquiredSet(player);
        Set<String> batch = batchExclude == null ? Collections.emptySet() : batchExclude;
        List<WeightedNote> pool = new ArrayList<>();
        Map<String, Integer> maxPartByStory = computeMaxPartByStory(snapshot);

        for (StoryDefinition story : snapshot.storiesById().values()) {
            int targetOrder = Config.useProgression ? PlayerStoryData.getProgressionLevel(player, story.id()) : 1;
            if (targetOrder != 1) {
                continue;
            }
            int maxOrder = Math.max(1, maxPartByStory.getOrDefault(story.id(), 1));
            for (NoteDefinition note : snapshot.notesById().values()) {
                if (!note.storyId().equals(story.id())) {
                    continue;
                }
                if (note.order() != 1) {
                    continue;
                }
                if (batch.contains(note.id())) {
                    continue;
                }
                if (!Config.allowDuplicates) {
                    if (discovered.contains(note.id()) || acquired.contains(note.id())) {
                        continue;
                    }
                }
                int baseWeight = Math.max(1, story.weight()) * Math.max(1, note.weight());
                double partDropWeight = calculatePartDropWeight(note.order(), maxOrder);
                int weight = Math.max(1, (int) Math.round(baseWeight * partDropWeight * 100.0D));
                pool.add(new WeightedNote(note, weight));
            }
        }
        return restrictToSingleStoryIfConfigured(pool, random);
    }

    /**
     * When {@link Config#enableMultipleStories} is false, each roll only considers one story at a time (weighted by
     * combined note weights in the pool) so chests do not mix parallel starters in one pick pass.
     */
    private static List<WeightedNote> restrictToSingleStoryIfConfigured(List<WeightedNote> pool, RandomSource random) {
        if (Config.enableMultipleStories || pool.size() <= 1) {
            return pool;
        }
        Set<String> storyIds = new HashSet<>();
        for (WeightedNote w : pool) {
            storyIds.add(w.note().storyId());
        }
        if (storyIds.size() <= 1) {
            return pool;
        }
        Map<String, Integer> weightByStory = new HashMap<>();
        for (WeightedNote w : pool) {
            weightByStory.merge(w.note().storyId(), w.weight(), Integer::sum);
        }
        int total = 0;
        for (int w : weightByStory.values()) {
            total += w;
        }
        if (total <= 0) {
            return pool;
        }
        int pick = random.nextInt(total);
        int cursor = 0;
        String chosen = null;
        for (Map.Entry<String, Integer> entry : weightByStory.entrySet()) {
            cursor += entry.getValue();
            if (pick < cursor) {
                chosen = entry.getKey();
                break;
            }
        }
        if (chosen == null) {
            return pool;
        }
        List<WeightedNote> out = new ArrayList<>();
        for (WeightedNote w : pool) {
            if (chosen.equals(w.note().storyId())) {
                out.add(w);
            }
        }
        return out;
    }

    private static Map<String, Integer> computeMaxPartByStory(StoryRegistrySnapshot snapshot) {
        Map<String, Integer> maxByStory = new HashMap<>();
        for (NoteDefinition note : snapshot.notesById().values()) {
            Integer existing = maxByStory.get(note.storyId());
            if (existing == null || note.order() > existing) {
                maxByStory.put(note.storyId(), note.order());
            }
        }
        return maxByStory;
    }

    private static double calculatePartDropWeight(int order, int maxOrder) {
        if (maxOrder <= 1) {
            return Config.partOneDropWeight;
        }
        double clampedOrder = Math.max(1, Math.min(order, maxOrder));
        double progress = (clampedOrder - 1.0D) / (maxOrder - 1.0D);
        return Config.partOneDropWeight + (Config.finalPartDropWeight - Config.partOneDropWeight) * progress;
    }

    public record WeightedNote(NoteDefinition note, int weight) {
    }
}
