package uk.co.extraspecialstudio;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Common config for Dead Letters ({@code config/dead_letters-common.toml}).
 * <p>
 * Section banners and push/pop match the RadioTowers / Dead Air style so the
 * in-game config screen and toml stay easy to scan. Key paths are unchanged
 * for existing worlds and modpacks.
 */
@Mod.EventBusSubscriber(modid = Dead_letters.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // --- Loot / chests ---
    private static final ForgeConfigSpec.DoubleValue SPAWN_CHANCE;
    private static final ForgeConfigSpec.IntValue MAX_NOTES_PER_CHEST;
    private static final ForgeConfigSpec.BooleanValue GUARANTEE_STARTER_SCRAPBOOK_FIRST_CHEST;
    private static final ForgeConfigSpec.BooleanValue GUARANTEE_FIRST_LOOTR_CHEST;

    // --- Progression / stories ---
    private static final ForgeConfigSpec.BooleanValue USE_PROGRESSION;
    private static final ForgeConfigSpec.BooleanValue ALLOW_DUPLICATES;
    private static final ForgeConfigSpec.DoubleValue PART_ONE_DROP_WEIGHT;
    private static final ForgeConfigSpec.DoubleValue FINAL_PART_DROP_WEIGHT;
    private static final ForgeConfigSpec.BooleanValue ENABLE_MULTIPLE_STORIES;

    // --- Placement ---
    private static final ForgeConfigSpec.BooleanValue PLACE_NOTES_REQUIRE_SNEAK;
    private static final ForgeConfigSpec.BooleanValue PLACE_SCRAPBOOK_REQUIRE_SNEAK;

    // --- Custom stories ---
    private static final ForgeConfigSpec.BooleanValue CUSTOM_STORIES_ENABLE;
    private static final ForgeConfigSpec.ConfigValue<String> CUSTOM_STORIES_PATH;
    private static final ForgeConfigSpec.BooleanValue CUSTOM_STORIES_ALLOW_OVERRIDES;
    private static final ForgeConfigSpec.BooleanValue CUSTOM_STORIES_LOG_LOADING;

    // --- Debug ---
    private static final ForgeConfigSpec.BooleanValue DEBUG_TESTING_MODE;
    private static final ForgeConfigSpec.BooleanValue DEBUG_LOG_LOOT_DECISIONS;

    static {
        // ========== LOOT / CHESTS ==========
        BUILDER.comment(
                "============================================================",
                "LOOT AND CHESTS",
                "How often Dead Letters notes appear in loot chests,",
                "and the one-shot starter scrapbook / first Lootr guarantees.",
                "Notes only inject after the player has unlocked the scrapbook",
                "(picking up the book, or finding it in the first chest).",
                "============================================================"
        ).push("general");

        SPAWN_CHANCE = BUILDER
                .comment(
                        "----- START HERE: CHEST NOTES -----",
                        "Chance that opening an eligible loot chest tries to inject a note.",
                        "0.0 = never, 1.0 = always. Default: 0.25."
                )
                .defineInRange("spawnChance", 0.25D, 0.0D, 1.0D);
        MAX_NOTES_PER_CHEST = BUILDER
                .comment(
                        "Maximum notes added to one chest open (including the first-Lootr guarantee).",
                        "Default: 1."
                )
                .defineInRange("maxNotesPerChest", 1, 0, 64);
        GUARANTEE_STARTER_SCRAPBOOK_FIRST_CHEST = BUILDER
                .comment(
                        "First loot chest you open places a scrapbook in that chest (or drops it)",
                        "if you do not already have one. Notes still require owning the book first."
                )
                .define("guaranteeStarterScrapbookFirstChest", true);
        GUARANTEE_FIRST_LOOTR_CHEST = BUILDER
                .comment(
                        "When Lootr is installed: the first Lootr chest you open grants one eligible",
                        "story part 1 (if any exist), even if spawn chance would fail.",
                        "Counts toward maxNotesPerChest."
                )
                .define("guaranteeFirstLootrChest", true);

        USE_PROGRESSION = BUILDER
                .comment(
                        "----- STORY PROGRESSION -----",
                        "true: each storyline advances in order per player (part 1, then 2, …).",
                        "false: any eligible part can roll regardless of order."
                )
                .define("useProgression", true);
        ALLOW_DUPLICATES = BUILDER
                .comment(
                        "true: notes you already discovered or received can roll again.",
                        "false: each note is granted at most once per player (recommended)."
                )
                .define("allowDuplicates", false);
        PART_ONE_DROP_WEIGHT = BUILDER
                .comment(
                        "Relative weight for part-1 notes when several stories compete.",
                        "Higher = more common. Suggested range 0–1. Default: 0.75."
                )
                .defineInRange("partOneDropWeight", 0.75D, 0.0D, 10.0D);
        FINAL_PART_DROP_WEIGHT = BUILDER
                .comment(
                        "Relative weight for final-part notes.",
                        "Keep low so endings stay rare. Default: 0.10."
                )
                .defineInRange("finalPartDropWeight", 0.10D, 0.0D, 10.0D);
        BUILDER.pop();

        // ========== STORIES ==========
        BUILDER.comment(
                "============================================================",
                "STORY SELECTION",
                "Controls whether one chest can draw from every storyline",
                "or sticks to a single story for that roll.",
                "============================================================"
        ).push("stories");
        ENABLE_MULTIPLE_STORIES = BUILDER
                .comment(
                        "true: one chest roll can pick among all eligible stories (weighted).",
                        "false: each roll narrows to a single story first, then picks a part."
                )
                .define("enableMultipleStories", true);
        BUILDER.pop();

        // ========== PLACEMENT ==========
        BUILDER.comment(
                "============================================================",
                "PLACEMENT",
                "World placement vs reading / opening on right-click.",
                "============================================================"
        ).push("placement");
        PLACE_NOTES_REQUIRE_SNEAK = BUILDER
                .comment(
                        "true: sneak + right-click places a letter in the world;",
                        "plain right-click reads it. Recommended for servers."
                )
                .define("notesRequireSneak", true);
        PLACE_SCRAPBOOK_REQUIRE_SNEAK = BUILDER
                .comment(
                        "true: sneak + right-click places the scrapbook;",
                        "plain right-click opens the archive. Recommended for servers."
                )
                .define("scrapbookRequireSneak", true);
        BUILDER.pop();

        // ========== CUSTOM STORIES ==========
        BUILDER.comment(
                "============================================================",
                "CUSTOM STORIES",
                "Load extra (or override) stories from the config folder.",
                "Folder layout can match the jar: story.json + notes/part_n.json,",
                "or flat part_n.txt / part_n.json beside story.json.",
                "============================================================"
        ).push("customStories");
        CUSTOM_STORIES_ENABLE = BUILDER
                .comment(
                        "----- CUSTOM CONTENT -----",
                        "Load stories from the path below under the Minecraft config directory."
                )
                .define("enable", true);
        CUSTOM_STORIES_PATH = BUILDER
                .comment(
                        "Folder relative to config/. Default: deadletters/stories",
                        "Example full path: <instance>/config/deadletters/stories/<story_id>/"
                )
                .define("path", "deadletters/stories");
        CUSTOM_STORIES_ALLOW_OVERRIDES = BUILDER
                .comment(
                        "true: a config story with the same id replaces the datapack/jar version",
                        "(and its notes). false: skip config stories that collide with built-ins."
                )
                .define("allowOverrides", true);
        CUSTOM_STORIES_LOG_LOADING = BUILDER
                .comment("Log each custom story that loads successfully (useful while authoring).")
                .define("logLoading", true);
        BUILDER.pop();

        // ========== DEBUG ==========
        BUILDER.comment(
                "============================================================",
                "DEBUG",
                "Leave these off for normal play. testingMode forces spawnChance",
                "to 100% and ensures at least one note per eligible chest.",
                "============================================================"
        ).push("debug");
        DEBUG_TESTING_MODE = BUILDER
                .comment(
                        "Aggressive testing: spawnChance becomes 1.0 and maxNotesPerChest",
                        "is raised to at least 1 while this is true."
                )
                .define("testingMode", false);
        DEBUG_LOG_LOOT_DECISIONS = BUILDER
                .comment("Log note-pool and weighted roll decisions to the server log.")
                .define("logLootDecisions", false);
        BUILDER.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static double spawnChance;
    public static int maxNotesPerChest;
    public static boolean useProgression;
    public static boolean allowDuplicates;
    public static double partOneDropWeight;
    public static double finalPartDropWeight;
    public static boolean guaranteeFirstLootrChest;
    public static boolean guaranteeStarterScrapbookFirstChest;
    public static boolean enableMultipleStories;
    public static boolean customStoriesEnable;
    public static String customStoriesPath;
    public static boolean customStoriesAllowOverrides;
    public static boolean customStoriesLogLoading;
    public static boolean placeNotesRequireSneak;
    public static boolean placeScrapbookRequireSneak;
    public static boolean debugTestingMode;
    public static boolean debugLogLootDecisions;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        spawnChance = SPAWN_CHANCE.get();
        maxNotesPerChest = MAX_NOTES_PER_CHEST.get();
        useProgression = USE_PROGRESSION.get();
        allowDuplicates = ALLOW_DUPLICATES.get();
        partOneDropWeight = PART_ONE_DROP_WEIGHT.get();
        finalPartDropWeight = FINAL_PART_DROP_WEIGHT.get();
        guaranteeFirstLootrChest = GUARANTEE_FIRST_LOOTR_CHEST.get();
        guaranteeStarterScrapbookFirstChest = GUARANTEE_STARTER_SCRAPBOOK_FIRST_CHEST.get();
        enableMultipleStories = ENABLE_MULTIPLE_STORIES.get();
        customStoriesEnable = CUSTOM_STORIES_ENABLE.get();
        customStoriesPath = CUSTOM_STORIES_PATH.get();
        customStoriesAllowOverrides = CUSTOM_STORIES_ALLOW_OVERRIDES.get();
        customStoriesLogLoading = CUSTOM_STORIES_LOG_LOADING.get();
        placeNotesRequireSneak = PLACE_NOTES_REQUIRE_SNEAK.get();
        placeScrapbookRequireSneak = PLACE_SCRAPBOOK_REQUIRE_SNEAK.get();
        debugTestingMode = DEBUG_TESTING_MODE.get();
        debugLogLootDecisions = DEBUG_LOG_LOOT_DECISIONS.get();

        if (debugTestingMode) {
            spawnChance = 1.0D;
            maxNotesPerChest = Math.max(maxNotesPerChest, 1);
        }
    }
}
