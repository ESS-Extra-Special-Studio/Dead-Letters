package uk.co.extraspecialstudio.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.item.NoteItem;
import uk.co.extraspecialstudio.story.NoteDefinition;
import uk.co.extraspecialstudio.story.StoryRegistry;

import java.util.Comparator;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Dead_letters.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NOTES =
            CREATIVE_MODE_TABS.register("notes", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.SCRAPBOOK.get()))
                    .title(Component.translatable("itemGroup.dead_letters.notes"))
                    .displayItems((parameters, output) -> {
                        output.accept(new ItemStack(ModItems.SCRAPBOOK.get()));
                        StoryRegistry.snapshot().notesById().values().stream()
                                .sorted(Comparator.comparing(NoteDefinition::storyId).thenComparingInt(NoteDefinition::order))
                                .forEach(note -> output.accept(NoteItem.createStack(note)));
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
