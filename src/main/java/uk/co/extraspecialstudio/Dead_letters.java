package uk.co.extraspecialstudio;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

    public Dead_letters() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        DeadLettersNetwork.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListener);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.register(LootNoteInjector.class);
        MinecraftForge.EVENT_BUS.register(ScrapbookUnlockHandler.class);
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(StoryReloadListener.INSTANCE);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        DeadLettersDebugCommands.register(event.getDispatcher());
    }
}
