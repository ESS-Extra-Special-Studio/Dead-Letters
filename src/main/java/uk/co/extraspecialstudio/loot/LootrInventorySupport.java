package uk.co.extraspecialstudio.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * Lootr stores opened loot in per-player inventories, not in the {@link ChestBlockEntity} item list.
 * Detect that case without a compile-time dependency on Lootr.
 */
public final class LootrInventorySupport {
    private static final String LEGACY_SPECIAL_CHEST = "noobanidus.mods.lootr.data.SpecialChestInventory";
    private static final String LOOTR_INVENTORY = "noobanidus.mods.lootr.common.data.LootrInventory";
    private static final String ILOOTR_INVENTORY = "noobanidus.mods.lootr.common.api.data.inventory.ILootrInventory";

    private LootrInventorySupport() {
    }

    public static boolean isLootrModLoaded() {
        return ModList.get().isLoaded("lootr");
    }

    public static boolean isLootrSpecialChestInventory(Container container) {
        if (!isLootrModLoaded() || container == null) {
            return false;
        }
        String name = container.getClass().getName();
        if (LEGACY_SPECIAL_CHEST.equals(name) || LOOTR_INVENTORY.equals(name)) {
            return true;
        }
        for (Class<?> iface : container.getClass().getInterfaces()) {
            if (ILOOTR_INVENTORY.equals(iface.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stable key for per-player "already injected into this Lootr inventory" tracking.
     */
    public static String lootrContainerKey(ServerPlayer player, Container container) {
        String dim = player.level().dimension().location().toString();
        try {
            Object info = container.getClass().getMethod("getInfo").invoke(container);
            if (info != null) {
                Object posObj = info.getClass().getMethod("getInfoPos").invoke(info);
                if (posObj instanceof BlockPos pos) {
                    return dim + "|lootr:b:" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
                }
                Object idObj = info.getClass().getMethod("getInfoUUID").invoke(info);
                if (idObj instanceof UUID id) {
                    return dim + "|lootr:t:" + id;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
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
        String name = container.getClass().getName();
        if (!name.equals("net.minecraft.world.CompoundContainer")
                && !name.equals("net.minecraft.world.inventory.CompoundContainer")) {
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
