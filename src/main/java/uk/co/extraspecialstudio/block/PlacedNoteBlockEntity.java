package uk.co.extraspecialstudio.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import uk.co.extraspecialstudio.registry.ModBlockEntities;

public class PlacedNoteBlockEntity extends BlockEntity {
    private String noteId = "";

    public PlacedNoteBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLACED_NOTE.get(), pos, state);
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        String normalized = noteId == null ? "" : noteId;
        if (normalized.equals(this.noteId)) {
            return;
        }
        this.noteId = normalized;
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("NoteID", noteId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.noteId = tag.getString("NoteID");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putString("NoteID", noteId);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        this.noteId = tag.getString("NoteID");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
