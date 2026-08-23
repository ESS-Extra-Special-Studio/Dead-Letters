package uk.co.extraspecialstudio.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * Lootr stores opened loot in {@code SpecialChestInventory}, not in the {@link ChestBlockEntity} item list.
 * Detect that case without a compile-time dependency on Lootr.
 */
public final class LootrInventorySupport {
    private static final String SPECIAL_CHEST_INVENTORY = "noobanidus.mods.lootr.data.SpecialChestInventory";

    private LootrInventorySupport() {
    }

    public static boolean isLootrModLoaded() {
        return ModList.get().isLoaded("lootr");
    }

    public static boolean isLootrSpecialChestInventory(Container container) {
        return isLootrModLoaded() && SPECIAL_CHEST_INVENTORY.equals(container.getClass().getName());
    }

    /**
     * Stable key for per-player "already injected into this Lootr inventory" tracking.
     */
    public static String lootrContainerKey(ServerPlayer player, Container container) {
        String dim = player.level().dimension().location().toString();
        try {
            Object posObj = container.getClass().getMethod("getPos").invoke(container);
            if (posObj instanceof BlockPos pos) {
                return dim + "|lootr:b:" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
            }
            Object idObj = container.getClass().getMethod("getTileId").invoke(container);
            if (idObj instanceof UUID id) {
                return dim + "|lootr:t:" + id;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return dim + "|lootr:i:" + System.identityHashCode(container);
    }

    public static String vanillaOrUnknownContainerKey(ServerPlayer player, Container container) {
        String dim = player.level().dimension().location().toString();
        if (container instanceof BlockEntity be) {
            BlockPos pos = be.getBlockPos();
            return dim + "|b:" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
        }
        BlockPos fromCompound = firstBlockPosInCompoundContainer(container);
        if (fromCompound != null) {
            return dim + "|b:" + fromCompound.getX() + "," + fromCompound.getY() + "," + fromCompound.getZ();
        }
        return dim + "|c:" + System.identityHashCode(container);
    }

    private static BlockPos firstBlockPosInCompoundContainer(Container container) {
        if (!container.getClass().getName().equals("net.minecraft.world.CompoundContainer")) {
            return null;
        }
        for (Field field : container.getClass().getDeclaredFields()) {
            if (!Container.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object sub = field.get(container);
                if (sub instanceof ChestBlockEntity be) {
                    return be.getBlockPos();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }
}
