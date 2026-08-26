package uk.co.extraspecialstudio.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Places the animated scrapbook in the world, or right-click in the air to open the archive from the item in hand.
 */
public class ScrapbookItem extends BlockItem {
    public ScrapbookItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && stack.getItem() == this) {
            if (level.isClientSide) {
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    uk.co.extraspecialstudio.client.DeadLettersClientHooks.openScrapbookInHand(stack);
                }
                return InteractionResultHolder.sidedSuccess(stack, true);
            }
            if (player instanceof ServerPlayer) {
                return InteractionResultHolder.sidedSuccess(stack, false);
            }
        }
        return InteractionResultHolder.pass(stack);
    }
}
