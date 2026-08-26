package uk.co.extraspecialstudio;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import uk.co.extraspecialstudio.command.DeadLettersDebugCommands;
import uk.co.extraspecialstudio.loot.LootNoteInjector;
import uk.co.extraspecialstudio.network.DeadLettersNetwork;
import uk.co.extraspecialstudio.registry.ModBlockEntities;
import uk.co.extraspecialstudio.registry.ModBlocks;
import uk.co.extraspecialstudio.registry.ModCreativeTabs;
import uk.co.extraspecialstudio.registry.ModItems;
import uk.co.extraspecialstudio.story.ScrapbookUnlockHandler;
import uk.co.extraspecialstudio.story.StoryReloadListener;

@Mod(Dead_letters.MODID)
public class Dead_letters {
    public static final String MODID = "dead_letters";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Dead_letters(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(DeadLettersNetwork::register);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.register(LootNoteInjector.class);
        NeoForge.EVENT_BUS.register(ScrapbookUnlockHandler.class);
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(StoryReloadListener.INSTANCE);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        DeadLettersDebugCommands.register(event.getDispatcher());
    }
}
