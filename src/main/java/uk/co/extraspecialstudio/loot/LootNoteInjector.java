package uk.co.extraspecialstudio.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import uk.co.extraspecialstudio.Config;
import uk.co.extraspecialstudio.item.NoteItem;
import uk.co.extraspecialstudio.registry.ModItems;
import uk.co.extraspecialstudio.story.NoteDefinition;
import uk.co.extraspecialstudio.story.NoteSelector;
import uk.co.extraspecialstudio.story.PlayerStoryData;

import java.util.HashSet;
import java.util.Set;

public final class LootNoteInjector {
    private LootNoteInjector() {
    }

    /**
     * Remembers which chest block was clicked so vanilla double-chest menus can get a stable dedupe key.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickChestBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof ChestBlock)) {
            return;
        }
        PlayerStoryData.armChestOpenInteraction(player, event.getPos(), event.getLevel().dimension(), event.getLevel().getGameTime());
    }

    /**
     * Injects into the {@link Container} backing the chest menu. Lootr uses {@code SpecialChestInventory} here, so
     * mutating only the {@link net.minecraft.world.level.block.entity.BlockEntity} slots would not show up in the GUI.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onChestMenuOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        if (!(event.getContainer() instanceof ChestMenu menu)) {
            return;
        }
        if (menu.slots.isEmpty()) {
            return;
        }
        Container chestInventory = menu.getSlot(0).container;
        if (chestInventory == player.getInventory()) {
            return;
        }

        String containerKey = resolveContainerKey(player, chestInventory);

        // Starter book: per-player flag defaults to "not done" when absent (existing saves / mod upgrade).
        if (Config.guaranteeStarterScrapbookFirstChest && !PlayerStoryData.isStarterScrapbookFirstChestDone(player)) {
            PlayerStoryData.markStarterScrapbookFirstChestDone(player);
            if (!PlayerStoryData.hasScrapbookInInventory(player)) {
                ItemStack book = new ItemStack(ModItems.SCRAPBOOK.get());
                if (!tryPutInContainer(chestInventory, book)) {
                    spawnDropNear(player, chestInventory, book);
                }
            }
        }

        if (!PlayerStoryData.isScrapbookUnlocked(player)) {
            return;
        }

        if (PlayerStoryData.hasInjectedContainer(player, containerKey)) {
            return;
        }

        RandomSource random = player.getRandom();
        Set<String> batchExcluded = new HashSet<>();
        int remaining = Math.max(0, Config.maxNotesPerChest);
        boolean didAnything = false;

        if (Config.guaranteeFirstLootrChest
                && LootrInventorySupport.isLootrModLoaded()
                && LootrInventorySupport.isLootrSpecialChestInventory(chestInventory)
                && !PlayerStoryData.isFirstLootrChestGuaranteeDone(player)) {
            PlayerStoryData.markFirstLootrChestGuaranteeDone(player);
            if (remaining > 0) {
                NoteDefinition guarantee = NoteSelector.selectEligiblePartOneExcluding(player, random, batchExcluded);
                if (guarantee != null) {
                    ItemStack stack = noteStack(guarantee);
                    if (!tryPutInContainer(chestInventory, stack)) {
                        spawnDropNear(player, chestInventory, stack);
                    }
                    PlayerStoryData.markAcquired(player, guarantee.id());
                    batchExcluded.add(guarantee.id());
                    remaining--;
                    didAnything = true;
                }
            }
        }

        if (!didAnything && random.nextDouble() > Config.spawnChance) {
            return;
        }

        PlayerStoryData.markInjectedContainer(player, containerKey);

        while (remaining > 0) {
            NoteDefinition selected = NoteSelector.selectNextExcluding(player, random, batchExcluded);
            if (selected == null) {
                break;
            }
            ItemStack stack = noteStack(selected);
            if (!tryPutInContainer(chestInventory, stack)) {
                spawnDropNear(player, chestInventory, stack);
            }
            PlayerStoryData.markAcquired(player, selected.id());
            batchExcluded.add(selected.id());
            remaining--;
        }

        if (Config.debugTestingMode) {
            player.sendSystemMessage(Component.literal("Dead Letters chest menu inject done for " + containerKey));
        }
    }

    private static String resolveContainerKey(ServerPlayer player, Container chestInventory) {
        if (LootrInventorySupport.isLootrSpecialChestInventory(chestInventory)) {
            return LootrInventorySupport.lootrContainerKey(player, chestInventory);
        }
        BlockPos pending = PlayerStoryData.takeChestOpenInteraction(player, player.level().dimension(), player.level().getGameTime());
        if (pending != null) {
            String dim = player.level().dimension().location().toString();
            return dim + "|b:" + pending.getX() + "," + pending.getY() + "," + pending.getZ();
        }
        return LootrInventorySupport.vanillaOrUnknownContainerKey(player, chestInventory);
    }

    private static ItemStack noteStack(NoteDefinition note) {
        return NoteItem.createStack(note);
    }

    private static boolean tryPutInContainer(Container container, ItemStack stack) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).isEmpty()) {
                container.setItem(slot, stack.copy());
                return true;
            }
        }
        return false;
    }

    private static void spawnDropNear(ServerPlayer player, Container chestInventory, ItemStack stack) {
        Vec3 at = resolveDropVec(player, chestInventory);
        ItemEntity drop = new ItemEntity(player.level(), at.x, at.y, at.z, stack);
        drop.setPickUpDelay(10);
        player.level().addFreshEntity(drop);
    }

    private static Vec3 resolveDropVec(ServerPlayer player, Container chestInventory) {
        if (LootrInventorySupport.isLootrSpecialChestInventory(chestInventory)) {
            try {
                Object posObj = chestInventory.getClass().getMethod("getPos").invoke(chestInventory);
                if (posObj instanceof BlockPos pos) {
                    return Vec3.atBottomCenterOf(pos).add(0.0D, 1.0D, 0.0D);
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return player.position();
    }
}
