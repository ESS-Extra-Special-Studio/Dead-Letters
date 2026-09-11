package uk.co.extraspecialstudio.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import uk.co.extraspecialstudio.story.NoteDefinition;
import uk.co.extraspecialstudio.story.StoryDefinition;
import uk.co.extraspecialstudio.story.StoryRegistry;
import uk.co.extraspecialstudio.story.StoryRegistrySnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Dedicated-server clients never run datapack reload listeners for Dead Letters stories.
 * Sync the full registry so item names, note screens, and scrapbook text resolve.
 */
public record SyncStoryRegistryPacket(
        Map<String, StoryDefinition> storiesById,
        Map<String, NoteDefinition> notesById
) {
    public static SyncStoryRegistryPacket fromSnapshot(StoryRegistrySnapshot snapshot) {
        return new SyncStoryRegistryPacket(snapshot.storiesById(), snapshot.notesById());
    }

    public static void encode(SyncStoryRegistryPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.storiesById.size());
        for (StoryDefinition story : packet.storiesById.values()) {
            buffer.writeUtf(story.id());
            buffer.writeUtf(story.name());
            buffer.writeVarInt(story.maxOrder());
            buffer.writeVarInt(story.weight());
            buffer.writeBoolean(story.textureSlot() != null);
            if (story.textureSlot() != null) {
                buffer.writeVarInt(story.textureSlot());
            }
            buffer.writeBoolean(story.pageTexture() != null);
            if (story.pageTexture() != null) {
                buffer.writeUtf(story.pageTexture());
            }
            buffer.writeBoolean(story.pageTextureLined() != null);
            if (story.pageTextureLined() != null) {
                buffer.writeBoolean(story.pageTextureLined());
            }
            buffer.writeUtf(story.source() == null ? "" : story.source());
        }

        buffer.writeVarInt(packet.notesById.size());
        for (NoteDefinition note : packet.notesById.values()) {
            buffer.writeUtf(note.id());
            buffer.writeUtf(note.storyId());
            buffer.writeVarInt(note.order());
            buffer.writeUtf(note.title());
            buffer.writeVarInt(note.weight());
            List<String> body = note.body() == null ? List.of() : note.body();
            buffer.writeVarInt(body.size());
            for (String line : body) {
                buffer.writeUtf(line == null ? "" : line);
            }
            buffer.writeUtf(note.source() == null ? "" : note.source());
        }
    }

    public static SyncStoryRegistryPacket decode(FriendlyByteBuf buffer) {
        int storyCount = buffer.readVarInt();
        Map<String, StoryDefinition> stories = new LinkedHashMap<>(storyCount);
        for (int i = 0; i < storyCount; i++) {
            String id = buffer.readUtf();
            String name = buffer.readUtf();
            int maxOrder = buffer.readVarInt();
            int weight = buffer.readVarInt();
            Integer textureSlot = buffer.readBoolean() ? buffer.readVarInt() : null;
            String pageTexture = buffer.readBoolean() ? buffer.readUtf() : null;
            Boolean pageTextureLined = buffer.readBoolean() ? buffer.readBoolean() : null;
            String source = buffer.readUtf();
            stories.put(id, new StoryDefinition(id, name, maxOrder, weight, textureSlot, pageTexture, pageTextureLined, source));
        }

        int noteCount = buffer.readVarInt();
        Map<String, NoteDefinition> notes = new LinkedHashMap<>(noteCount);
        for (int i = 0; i < noteCount; i++) {
            String id = buffer.readUtf();
            String storyId = buffer.readUtf();
            int order = buffer.readVarInt();
            String title = buffer.readUtf();
            int weight = buffer.readVarInt();
            int bodyLines = buffer.readVarInt();
            List<String> body = new ArrayList<>(bodyLines);
            for (int line = 0; line < bodyLines; line++) {
                body.add(buffer.readUtf());
            }
            String source = buffer.readUtf();
            notes.put(id, new NoteDefinition(id, storyId, order, title, body, weight, source));
        }
        return new SyncStoryRegistryPacket(stories, notes);
    }

    public static void handle(SyncStoryRegistryPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                StoryRegistry.replace(new StoryRegistrySnapshot(packet.storiesById(), packet.notesById()))));
        context.setPacketHandled(true);
    }
}
