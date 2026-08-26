package uk.co.extraspecialstudio.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.joml.Matrix4f;
import uk.co.extraspecialstudio.block.PlacedNoteBlockEntity;
import uk.co.extraspecialstudio.item.NoteItem;
import uk.co.extraspecialstudio.item.NoteNbt;
import uk.co.extraspecialstudio.registry.ModItems;
import uk.co.extraspecialstudio.story.NoteDefinition;
import uk.co.extraspecialstudio.story.StoryRegistry;

import java.util.ArrayList;
import java.util.List;

public class PlacedNoteRenderer implements BlockEntityRenderer<PlacedNoteBlockEntity> {
    /** Font pixels before world scale; ~matches readable width on the note sheet. */
    private static final int TEXT_WRAP_WIDTH = 110;
    private static final int MAX_TEXT_LINES = 12;
    /** World scale for font quads (smaller = finer print on the page). */
    private static final float TEXT_WORLD_SCALE = 0.0048F;
    /** Push text in front of the item quads (block units) so it is not depth-tested away. */
    private static final float TEXT_Z_EPSILON = 0.055F;
    /** Max block+sky packed light so note text stays readable in the dark. */
    private static final int TEXT_FULL_BRIGHT = 15728880;
    private static final int TEXT_COLOR = 0xFF2A2318;

    public PlacedNoteRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public void render(PlacedNoteBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        String noteId = blockEntity.getNoteId();
        if (noteId == null || noteId.isBlank()) {
            return;
        }
        NoteDefinition note = StoryRegistry.snapshot().notesById().get(noteId);
        ItemStack displayStack = note != null ? NoteItem.createStack(note) : new ItemStack(ModItems.NOTE.get());
        NoteNbt.setNoteId(displayStack, noteId);

        BlockState state = blockEntity.getBlockState();
        AttachFace face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
        net.minecraft.core.Direction facing = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        switch (face) {
            case FLOOR -> {
                poseStack.translate(0.0D, -0.49D, 0.0D);
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            }
            case CEILING -> {
                poseStack.translate(0.0D, 0.49D, 0.0D);
                poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            }
            case WALL -> {
                net.minecraft.core.Direction towardSupport = facing.getOpposite();
                switch (towardSupport) {
                    case NORTH -> {
                        poseStack.translate(0.0D, 0.0D, -0.49D);
                        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                    }
                    case SOUTH -> poseStack.translate(0.0D, 0.0D, 0.49D);
                    case WEST -> {
                        poseStack.translate(-0.49D, 0.0D, 0.0D);
                        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                    }
                    case EAST -> {
                        poseStack.translate(0.49D, 0.0D, 0.0D);
                        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                    }
                    default -> {
                    }
                }
            }
        }
        poseStack.scale(0.95F, 0.95F, 0.95F);
        Minecraft.getInstance().getItemRenderer().renderStatic(displayStack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffer, blockEntity.getLevel(), 0);

        if (note != null) {
            renderNoteTextOverlay(note, poseStack, buffer);
        }

        poseStack.popPose();
    }

    private static void renderNoteTextOverlay(NoteDefinition note, PoseStack poseStack, MultiBufferSource buffer) {
        Font font = Minecraft.getInstance().font;
        List<String> rows = new ArrayList<>();
        appendWrapped(rows, note.title(), font, TEXT_WRAP_WIDTH, MAX_TEXT_LINES);
        for (String raw : note.body()) {
            if (rows.size() >= MAX_TEXT_LINES) {
                break;
            }
            if (raw.isBlank()) {
                continue;
            }
            appendWrapped(rows, raw, font, TEXT_WRAP_WIDTH, MAX_TEXT_LINES);
        }
        if (rows.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, TEXT_Z_EPSILON);
        poseStack.scale(TEXT_WORLD_SCALE, -TEXT_WORLD_SCALE, TEXT_WORLD_SCALE);

        float lineHeight = font.lineHeight + 1;
        float totalH = rows.size() * lineHeight;
        float y0 = -totalH * 0.5F;
        Matrix4f matrix = poseStack.last().pose();
        int light = TEXT_FULL_BRIGHT;
        RenderSystem.disableDepthTest();
        try {
            int i = 0;
            for (String row : rows) {
                float w = font.width(row);
                float x = -w * 0.5F;
                float y = y0 + i * lineHeight;
                font.drawInBatch(row, x, y, TEXT_COLOR, true, matrix, buffer, Font.DisplayMode.SEE_THROUGH, 0, light);
                i++;
            }
        } finally {
            RenderSystem.enableDepthTest();
        }
        poseStack.popPose();
    }

    private static void appendWrapped(List<String> rows, String text, Font font, int maxWidth, int maxRows) {
        String trimmed = text.stripLeading();
        if (trimmed.isEmpty()) {
            return;
        }
        List<String> words = new ArrayList<>();
        for (String w : trimmed.split("\\s+")) {
            if (!w.isEmpty()) {
                words.add(w);
            }
        }
        if (words.isEmpty()) {
            return;
        }
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (rows.size() >= maxRows) {
                return;
            }
            String trial = line.isEmpty() ? word : line + " " + word;
            if (font.width(trial) <= maxWidth) {
                if (!line.isEmpty()) {
                    line.append(' ');
                }
                line.append(word);
                continue;
            }
            if (!line.isEmpty()) {
                rows.add(line.toString());
                line.setLength(0);
            }
            if (font.width(word) <= maxWidth) {
                line.append(word);
            } else {
                rows.add(word);
            }
        }
        if (!line.isEmpty() && rows.size() < maxRows) {
            rows.add(line.toString());
        }
    }
}
