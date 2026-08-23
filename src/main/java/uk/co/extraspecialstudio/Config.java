package uk.co.extraspecialstudio;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Dead_letters.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.DoubleValue SPAWN_CHANCE = BUILDER
            .comment("Chance a loot roll attempts to inject a note.")
            .defineInRange("general.spawnChance", 0.25D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.IntValue MAX_NOTES_PER_CHEST = BUILDER
            .comment("Hard cap for notes added to each generated chest.")
            .defineInRange("general.maxNotesPerChest", 1, 0, 64);

    private static final ForgeConfigSpec.BooleanValue USE_PROGRESSION = BUILDER
            .comment("If true, each storyline advances linearly per player.")
            .define("general.useProgression", true);

    private static final ForgeConfigSpec.BooleanValue ALLOW_DUPLICATES = BUILDER
            .comment("If true, notes you already discovered or received can be rolled again for loot.")
            .define("general.allowDuplicates", false);

    private static final ForgeConfigSpec.DoubleValue PART_ONE_DROP_WEIGHT = BUILDER
            .comment("Relative weight multiplier for part 1 notes (0-1 range recommended).")
            .defineInRange("general.partOneDropWeight", 0.75D, 0.0D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue FINAL_PART_DROP_WEIGHT = BUILDER
            .comment("Relative weight multiplier for final-part notes (0-1 range recommended).")
            .defineInRange("general.finalPartDropWeight", 0.10D, 0.0D, 10.0D);

    private static final ForgeConfigSpec.BooleanValue GUARANTEE_FIRST_LOOTR_CHEST = BUILDER
            .comment("When Lootr is installed, the first Lootr chest inventory you open grants one eligible story part 1 (if any exist), even if spawn chance would fail.")
            .define("general.guaranteeFirstLootrChest", true);

    private static final ForgeConfigSpec.BooleanValue GUARANTEE_STARTER_SCRAPBOOK_FIRST_CHEST = BUILDER
            .comment("The first loot chest you open places a Dead Letters scrapbook in that chest (or drops it) if you do not already have one. Notes still require picking up the book first.")
            .define("general.guaranteeStarterScrapbookFirstChest", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_MULTIPLE_STORIES = BUILDER
            .comment("If true, one chest roll can pick among all eligible stories. If false, each roll narrows to a single story (still weighted).")
            .define("stories.enableMultipleStories", true);

    private static final ForgeConfigSpec.BooleanValue CUSTOM_STORIES_ENABLE = BUILDER
            .comment("Loads stories from config/deadletters/stories.")
            .define("customStories.enable", true);

    private static final ForgeConfigSpec.ConfigValue<String> CUSTOM_STORIES_PATH = BUILDER
            .comment("Relative to the Minecraft config directory.")
            .define("customStories.path", "deadletters/stories");

    private static final ForgeConfigSpec.BooleanValue CUSTOM_STORIES_ALLOW_OVERRIDES = BUILDER
            .comment("If true, config stories can override datapack stories by ID.")
            .define("customStories.allowOverrides", true);

    private static final ForgeConfigSpec.BooleanValue CUSTOM_STORIES_LOG_LOADING = BUILDER
            .comment("Logs custom story loading details.")
            .define("customStories.logLoading", true);

    private static final ForgeConfigSpec.BooleanValue DEBUG_TESTING_MODE = BUILDER
            .comment("Enables aggressive testing defaults for faster validation.")
            .define("debug.testingMode", false);

    private static final ForgeConfigSpec.BooleanValue DEBUG_LOG_LOOT_DECISIONS = BUILDER
            .comment("Logs note-pool and weighted roll decisions.")
            .define("debug.logLootDecisions", false);

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
        debugTestingMode = DEBUG_TESTING_MODE.get();
        debugLogLootDecisions = DEBUG_LOG_LOOT_DECISIONS.get();

        if (debugTestingMode) {
            spawnChance = 1.0D;
            maxNotesPerChest = Math.max(maxNotesPerChest, 1);
        }
    }
}
