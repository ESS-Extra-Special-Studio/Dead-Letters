package uk.co.extraspecialstudio.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.item.NoteItem;
import uk.co.extraspecialstudio.item.ScrapbookItem;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Dead_letters.MODID);
    public static final DeferredHolder<Item, Item> SCRAPBOOK = ITEMS.register("scrapbook",
            () -> new ScrapbookItem(ModBlocks.PLACED_SCRAPBOOK.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> NOTE = ITEMS.register("note",
            () -> new NoteItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
