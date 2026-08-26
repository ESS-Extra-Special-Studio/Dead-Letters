package uk.co.extraspecialstudio.mixin;

import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.extraspecialstudio.item.NoteItem;

@Mixin(LecternBlockEntity.class)
public class LecternBlockEntityNoteMixin {
    @Inject(method = "getRedstoneSignal()I", at = @At("HEAD"), cancellable = true)
    private void deadLetters$safeComparatorForNotes(CallbackInfoReturnable<Integer> cir) {
        LecternBlockEntity lectern = (LecternBlockEntity) (Object) this;
        if (lectern.getBook().getItem() instanceof NoteItem) {
            cir.setReturnValue(0);
        }
    }
}
