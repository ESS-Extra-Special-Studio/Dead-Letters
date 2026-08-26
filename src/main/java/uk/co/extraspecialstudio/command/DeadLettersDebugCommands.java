package uk.co.extraspecialstudio.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import uk.co.extraspecialstudio.item.NoteItem;
import uk.co.extraspecialstudio.registry.ModItems;
import uk.co.extraspecialstudio.story.NoteDefinition;
import uk.co.extraspecialstudio.story.NoteSelector;
import uk.co.extraspecialstudio.story.PlayerStoryData;
import uk.co.extraspecialstudio.story.StoryDefinition;
import uk.co.extraspecialstudio.story.StoryRegistry;
import uk.co.extraspecialstudio.story.StoryRegistrySnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DeadLettersDebugCommands {
    private DeadLettersDebugCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("deadletters")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("debug")
                        .then(Commands.literal("stories").executes(context -> showStories(context.getSource())))
                        .then(Commands.literal("roll")
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 5000))
                                        .executes(context -> simulateRolls(context.getSource(), IntegerArgumentType.getInteger(context, "count")))))
                        .then(Commands.literal("state")
                                .executes(context -> showState(context.getSource())))
                        .then(Commands.literal("reset")
                                .executes(context -> resetState(context.getSource())))
                        .then(Commands.literal("give")
                                .then(Commands.argument("noteId", StringArgumentType.string())
                                        .executes(context -> giveNote(context.getSource(), StringArgumentType.getString(context, "noteId")))))));
    }

    private static int showStories(CommandSourceStack source) {
        StoryRegistrySnapshot snapshot = StoryRegistry.snapshot();
        source.sendSuccess(() -> Component.literal("Dead Letters: " + snapshot.storiesById().size() + " stories, " + snapshot.notesById().size() + " notes"), false);

        for (StoryDefinition story : snapshot.storiesById().values()) {
            long noteCount = snapshot.notesById().values().stream().filter(note -> note.storyId().equals(story.id())).count();
            source.sendSuccess(() -> Component.literal("- " + story.id() + " (" + noteCount + " parts, weight " + story.weight() + ")"), false);
        }
        return 1;
    }

    private static int simulateRolls(CommandSourceStack source, int count) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command requires a player."));
            return 0;
        }

        StoryRegistrySnapshot snapshot = StoryRegistry.snapshot();
        if (NoteSelector.buildValidPool(player, snapshot).isEmpty()) {
            source.sendFailure(Component.literal("Dead Letters pool is empty."));
            return 0;
        }

        Map<String, Integer> hitsByStory = new HashMap<>();
        Map<String, Integer> hitsByNote = new HashMap<>();
        RandomSource random = RandomSource.create();

        for (int i = 0; i < count; i++) {
            List<NoteSelector.WeightedNote> pool = NoteSelector.buildValidPool(player, snapshot, Collections.emptySet(), random);
            if (pool.isEmpty()) {
                continue;
            }
            NoteSelector.WeightedNote selected = roll(pool, random);
            if (selected == null) {
                continue;
            }
            hitsByStory.merge(selected.note().storyId(), 1, Integer::sum);
            hitsByNote.merge(selected.note().id(), 1, Integer::sum);
        }

        source.sendSuccess(() -> Component.literal("Dead Letters debug roll complete: " + count + " iterations"), false);
        source.sendSuccess(() -> Component.literal("Top stories:"), false);
        hitsByStory.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> source.sendSuccess(() -> Component.literal("- " + entry.getKey() + ": " + entry.getValue()), false));

        source.sendSuccess(() -> Component.literal("Top notes:"), false);
        hitsByNote.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> source.sendSuccess(() -> Component.literal("- " + entry.getKey() + ": " + entry.getValue()), false));
        return 1;
    }

    private static int showState(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command requires a player."));
            return 0;
        }

        StoryRegistrySnapshot snapshot = StoryRegistry.snapshot();
        source.sendSuccess(() -> Component.literal("Dead Letters progression state:"), false);
        for (StoryDefinition story : snapshot.storiesById().values()) {
            int level = PlayerStoryData.getProgressionLevel(player, story.id());
            source.sendSuccess(() -> Component.literal("- " + story.id() + ": part " + level), false);
        }
        source.sendSuccess(() -> Component.literal("Discovered notes: " + PlayerStoryData.getDiscoveredSet(player).size()), false);
        source.sendSuccess(() -> Component.literal("Acquired notes: " + PlayerStoryData.getAcquiredSet(player).size()), false);
        source.sendSuccess(() -> Component.literal("First Lootr chest guarantee used: " + PlayerStoryData.isFirstLootrChestGuaranteeDone(player)), false);
        source.sendSuccess(() -> Component.literal("Scrapbook unlocked (notes may spawn in chests): " + PlayerStoryData.isScrapbookUnlocked(player)), false);
        source.sendSuccess(() -> Component.literal("Starter scrapbook first-chest flag: " + PlayerStoryData.isStarterScrapbookFirstChestDone(player)), false);
        return 1;
    }

    private static int resetState(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command requires a player."));
            return 0;
        }
        PlayerStoryData.resetAll(player);
        source.sendSuccess(() -> Component.literal("Dead Letters progression and container history reset."), false);
        return 1;
    }

    private static int giveNote(CommandSourceStack source, String noteId) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command requires a player."));
            return 0;
        }

        NoteDefinition note = StoryRegistry.snapshot().notesById().get(noteId);
        if (note == null) {
            source.sendFailure(Component.literal("Unknown note id: " + noteId));
            return 0;
        }

        ItemStack stack = NoteItem.createStack(note);
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
        PlayerStoryData.markAcquired(player, note.id());
        source.sendSuccess(() -> Component.literal("Gave note: " + note.id()), false);
        return 1;
    }

    private static NoteSelector.WeightedNote roll(java.util.List<NoteSelector.WeightedNote> pool, RandomSource random) {
        int total = 0;
        for (NoteSelector.WeightedNote weighted : pool) {
            total += weighted.weight();
        }
        if (total <= 0) {
            return null;
        }

        int pick = random.nextInt(total);
        int cursor = 0;
        for (NoteSelector.WeightedNote weighted : pool) {
            cursor += weighted.weight();
            if (pick < cursor) {
                return weighted;
            }
        }
        return null;
    }
}
