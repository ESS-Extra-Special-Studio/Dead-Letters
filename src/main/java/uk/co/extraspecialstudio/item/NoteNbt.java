package uk.co.extraspecialstudio.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.function.Consumer;

/** Item NBT helpers for Dead Letters notes on {@link DataComponents#CUSTOM_DATA} (1.21+). */
public final class NoteNbt {
    private NoteNbt() {
    }

    public static CompoundTag get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    public static CompoundTag getOrCreate(ItemStack stack) {
        CompoundTag tag = get(stack);
        return tag != null ? tag : new CompoundTag();
    }

    public static void set(ItemStack stack, CompoundTag tag) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> mutator) {
        CompoundTag tag = getOrCreate(stack);
        mutator.accept(tag);
        set(stack, tag);
    }

    public static String getNoteId(ItemStack stack) {
        CompoundTag tag = get(stack);
        return tag == null ? "" : tag.getString(NoteItem.NOTE_ID_TAG);
    }

    public static void setNoteId(ItemStack stack, String noteId) {
        update(stack, tag -> tag.putString(NoteItem.NOTE_ID_TAG, noteId == null ? "" : noteId));
    }

    public static void setCustomModelData(ItemStack stack, int value) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(value));
    }
}
