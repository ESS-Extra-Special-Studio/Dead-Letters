package uk.co.extraspecialstudio.story;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.registry.ModItems;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PlayerStoryData {
    private static final String ROOT = Dead_letters.MODID + "_data";
    private static final String STORIES = "stories";
    private static final String LEVEL = "level";
    private static final String DISCOVERED = "discovered";
    /** Note ids already granted via loot injection or commands; suppresses duplicate spawns before read. */
    private static final String ACQUIRED = "acquired";
    /** Note ids the player has explicitly archived into the scrapbook notebook. */
    private static final String NOTEBOOK = "notebook";
    private static final String CONTAINERS = "containers";
    /** One-shot: first Lootr chest inventory this player opens gets a guaranteed eligible part 1 (if any exist). */
    private static final String FIRST_LOOTR_CHEST_DONE = "firstLootrChestDone";
    /** Right-clicked chest block before menu opens; used for stable keys (e.g. vanilla double chest). */
    private static final String PENDING_CHEST_OPEN = "pendingChestOpen";
    /** Once true, loot chests may inject Dead Letters notes for this player. */
    private static final String SCRAPBOOK_UNLOCKED = "scrapbookUnlocked";
    /**
     * First loot chest menu this player opens gets a starter scrapbook if they do not already have one.
     * If this key is missing (player or world predates the feature), {@code CompoundTag#getBoolean} is false, so
     * upgrading the mod on an established world still grants the book on the next first chest open.
     */
    private static final String STARTER_SCRAPBOOK_FIRST_CHEST_DONE = "starterScrapbookFirstChestDone";

    private PlayerStoryData() {
    }

    public static int getProgressionLevel(ServerPlayer player, String storyId) {
        CompoundTag storyTag = getOrCreateStoryTag(player, storyId);
        return Math.max(1, storyTag.getInt(LEVEL));
    }

    public static void setProgressionLevel(ServerPlayer player, String storyId, int level) {
        CompoundTag storyTag = getOrCreateStoryTag(player, storyId);
        storyTag.putInt(LEVEL, Math.max(1, level));
    }

    public static boolean isDiscovered(ServerPlayer player, String noteId) {
        CompoundTag root = getOrCreateRoot(player);
        ListTag discovered = root.getList(DISCOVERED, Tag.TAG_STRING);
        for (Tag tag : discovered) {
            if (noteId.equals(tag.getAsString())) {
                return true;
            }
        }
        return false;
    }

    public static void markDiscovered(ServerPlayer player, String noteId) {
        if (isDiscovered(player, noteId)) {
            return;
        }
        CompoundTag root = getOrCreateRoot(player);
        ListTag discovered = root.getList(DISCOVERED, Tag.TAG_STRING);
        discovered.add(StringTag.valueOf(noteId));
        root.put(DISCOVERED, discovered);
    }

    public static boolean isAcquired(ServerPlayer player, String noteId) {
        CompoundTag root = getOrCreateRoot(player);
        ListTag acquired = root.getList(ACQUIRED, Tag.TAG_STRING);
        for (Tag tag : acquired) {
            if (noteId.equals(tag.getAsString())) {
                return true;
            }
        }
        return false;
    }

    public static void markAcquired(ServerPlayer player, String noteId) {
        if (isAcquired(player, noteId)) {
            return;
        }
        CompoundTag root = getOrCreateRoot(player);
        ListTag acquired = root.getList(ACQUIRED, Tag.TAG_STRING);
        acquired.add(StringTag.valueOf(noteId));
        root.put(ACQUIRED, acquired);
    }

    public static Set<String> getDiscoveredSet(ServerPlayer player) {
        Set<String> out = new HashSet<>();
        CompoundTag root = getOrCreateRoot(player);
        ListTag discovered = root.getList(DISCOVERED, Tag.TAG_STRING);
        for (Tag tag : discovered) {
            out.add(tag.getAsString());
        }
        return out;
    }

    public static Set<String> getAcquiredSet(ServerPlayer player) {
        Set<String> out = new HashSet<>();
        CompoundTag root = getOrCreateRoot(player);
        ListTag acquired = root.getList(ACQUIRED, Tag.TAG_STRING);
        for (Tag tag : acquired) {
            out.add(tag.getAsString());
        }
        return out;
    }

    public static boolean hasNotebookEntry(ServerPlayer player, String noteId) {
        CompoundTag root = getOrCreateRoot(player);
        ListTag notebook = root.getList(NOTEBOOK, Tag.TAG_STRING);
        for (Tag tag : notebook) {
            if (noteId.equals(tag.getAsString())) {
                return true;
            }
        }
        return false;
    }

    public static boolean addNotebookEntry(ServerPlayer player, String noteId) {
        if (hasNotebookEntry(player, noteId)) {
            return false;
        }
        CompoundTag root = getOrCreateRoot(player);
        ListTag notebook = root.getList(NOTEBOOK, Tag.TAG_STRING);
        notebook.add(StringTag.valueOf(noteId));
        root.put(NOTEBOOK, notebook);
        return true;
    }

    public static boolean removeNotebookEntry(ServerPlayer player, String noteId) {
        CompoundTag root = getOrCreateRoot(player);
        ListTag notebook = root.getList(NOTEBOOK, Tag.TAG_STRING);
        ListTag rewritten = new ListTag();
        boolean removed = false;
        for (Tag tag : notebook) {
            String value = tag.getAsString();
            if (!removed && noteId.equals(value)) {
                removed = true;
                continue;
            }
            rewritten.add(StringTag.valueOf(value));
        }
        if (!removed) {
            return false;
        }
        root.put(NOTEBOOK, rewritten);
        return true;
    }

    public static Set<String> getNotebookEntries(ServerPlayer player) {
        Set<String> out = new HashSet<>();
        CompoundTag root = getOrCreateRoot(player);
        ListTag notebook = root.getList(NOTEBOOK, Tag.TAG_STRING);
        for (Tag tag : notebook) {
            out.add(tag.getAsString());
        }
        return out;
    }

    public static List<String> getNotebookEntriesInInsertionOrder(ServerPlayer player) {
        List<String> out = new ArrayList<>();
        CompoundTag root = getOrCreateRoot(player);
        ListTag notebook = root.getList(NOTEBOOK, Tag.TAG_STRING);
        for (Tag tag : notebook) {
            out.add(tag.getAsString());
        }
        return out;
    }

    public static boolean hasInjectedContainer(ServerPlayer player, String containerKey) {
        CompoundTag root = getOrCreateRoot(player);
        ListTag containers = root.getList(CONTAINERS, Tag.TAG_STRING);
        for (Tag tag : containers) {
            if (containerKey.equals(tag.getAsString())) {
                return true;
            }
        }
        return false;
    }

    public static void markInjectedContainer(ServerPlayer player, String containerKey) {
        if (hasInjectedContainer(player, containerKey)) {
            return;
        }
        CompoundTag root = getOrCreateRoot(player);
        ListTag containers = root.getList(CONTAINERS, Tag.TAG_STRING);
        containers.add(StringTag.valueOf(containerKey));
        root.put(CONTAINERS, containers);
    }

    public static boolean isFirstLootrChestGuaranteeDone(ServerPlayer player) {
        return getOrCreateRoot(player).getBoolean(FIRST_LOOTR_CHEST_DONE);
    }

    public static void markFirstLootrChestGuaranteeDone(ServerPlayer player) {
        CompoundTag root = getOrCreateRoot(player);
        root.putBoolean(FIRST_LOOTR_CHEST_DONE, true);
    }

    public static void armChestOpenInteraction(ServerPlayer player, BlockPos pos, ResourceKey<Level> dimension, long gameTime) {
        CompoundTag root = getOrCreateRoot(player);
        CompoundTag pending = new CompoundTag();
        pending.putLong("pos", pos.asLong());
        pending.putString("dim", dimension.location().toString());
        pending.putLong("time", gameTime);
        root.put(PENDING_CHEST_OPEN, pending);
    }

    @Nullable
    public static BlockPos takeChestOpenInteraction(ServerPlayer player, ResourceKey<Level> dimension, long gameTime) {
        CompoundTag root = getOrCreateRoot(player);
        if (!root.contains(PENDING_CHEST_OPEN, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag pending = root.getCompound(PENDING_CHEST_OPEN);
        root.remove(PENDING_CHEST_OPEN);
        if (!dimension.location().toString().equals(pending.getString("dim"))) {
            return null;
        }
        if (gameTime - pending.getLong("time") > 8L) {
            return null;
        }
        return BlockPos.of(pending.getLong("pos"));
    }

    public static void resetAll(ServerPlayer player) {
        player.getPersistentData().remove(ROOT);
    }

    /**
     * Forge does not copy {@link net.minecraft.world.entity.Entity#getPersistentData()} on respawn.
     * Call from {@link net.minecraftforge.event.entity.player.PlayerEvent.Clone}.
     */
    public static void copyPersistentData(ServerPlayer from, ServerPlayer to) {
        CompoundTag root = from.getPersistentData().getCompound(ROOT);
        if (!root.isEmpty()) {
            to.getPersistentData().put(ROOT, root.copy());
        }
    }

    public static boolean isScrapbookUnlocked(ServerPlayer player) {
        return getOrCreateRoot(player).getBoolean(SCRAPBOOK_UNLOCKED);
    }

    public static void markScrapbookUnlocked(ServerPlayer player) {
        if (isScrapbookUnlocked(player)) {
            return;
        }
        CompoundTag root = getOrCreateRoot(player);
        root.putBoolean(SCRAPBOOK_UNLOCKED, true);
    }

    public static boolean isStarterScrapbookFirstChestDone(ServerPlayer player) {
        return getOrCreateRoot(player).getBoolean(STARTER_SCRAPBOOK_FIRST_CHEST_DONE);
    }

    public static void markStarterScrapbookFirstChestDone(ServerPlayer player) {
        CompoundTag root = getOrCreateRoot(player);
        root.putBoolean(STARTER_SCRAPBOOK_FIRST_CHEST_DONE, true);
    }

    public static boolean hasScrapbookInInventory(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.SCRAPBOOK.get())) {
                return true;
            }
        }
        return false;
    }

    private static CompoundTag getOrCreateStoryTag(ServerPlayer player, String storyId) {
        CompoundTag root = getOrCreateRoot(player);
        CompoundTag stories = root.getCompound(STORIES);
        CompoundTag storyTag = stories.getCompound(storyId);
        if (!storyTag.contains(LEVEL)) {
            storyTag.putInt(LEVEL, 1);
        }
        stories.put(storyId, storyTag);
        root.put(STORIES, stories);
        return storyTag;
    }

    private static CompoundTag getOrCreateRoot(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData();
        CompoundTag root = persisted.getCompound(ROOT);
        if (!root.contains(STORIES)) {
            root.put(STORIES, new CompoundTag());
        }
        if (!root.contains(DISCOVERED)) {
            root.put(DISCOVERED, new ListTag());
        }
        if (!root.contains(ACQUIRED)) {
            root.put(ACQUIRED, new ListTag());
        }
        if (!root.contains(NOTEBOOK)) {
            root.put(NOTEBOOK, new ListTag());
        }
        if (!root.contains(CONTAINERS)) {
            root.put(CONTAINERS, new ListTag());
        }
        persisted.put(ROOT, root);
        return root;
    }
}
