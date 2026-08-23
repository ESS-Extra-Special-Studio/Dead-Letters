package uk.co.extraspecialstudio.registry;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.item.NoteItem;
import uk.co.extraspecialstudio.item.ScrapbookItem;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Dead_letters.MODID);
    public static final RegistryObject<Item> SCRAPBOOK = ITEMS.register("scrapbook", () -> new ScrapbookItem(ModBlocks.PLACED_SCRAPBOOK.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> NOTE = ITEMS.register("note", () -> new NoteItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
