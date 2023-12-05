package uppers.tiles;

import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import uppers.blocks.UpperBlock;

public class InventoryCodeHooksTweaked
{
    /**
     * Copied from FORGE!!!
     * @return Null if we did nothing {no IItemHandler}, True if we moved an item, False if we moved no items
     */
    @Nullable
    public static Boolean extractHook(Level level, IUpper dest)
    {
    	return getItemHandler(level, dest, Direction.DOWN).map(itemHandlerResult -> {

        IItemHandler handler = itemHandlerResult.getKey();

        for (int i = 0; i < handler.getSlots(); i++)
        {
            ItemStack extractItem = handler.extractItem(i, 1, true);
            if (!extractItem.isEmpty())
            {
                for (int j = 0; j < dest.getContainerSize(); j++)
                {
                    ItemStack destStack = dest.getItem(j);
                    if (dest.canPlaceItem(j, extractItem) && (destStack.isEmpty() || destStack.getCount() < destStack.getMaxStackSize() && destStack.getCount() < dest.getMaxStackSize() && ItemHandlerHelper.canItemStacksStack(extractItem, destStack)))
                    {
                        extractItem = handler.extractItem(i, 1, false);
                        if (destStack.isEmpty())
                            dest.setItem(j, extractItem);
                        else
                        {
                            destStack.grow(1);
                            dest.setItem(j, destStack);
                        }
                        dest.setChanged();
                        return true;
                    }
                }
            }
        }
			return false;
		}).orElse(null); // TODO bad null
	}

    public static boolean insertHook(UpperBlockEntity tileEntityUpper)
    {
    	Direction upperFacing = tileEntityUpper.getBlockState().getValue(UpperBlock.FACING);
        return getItemHandler(tileEntityUpper.getLevel(), tileEntityUpper, upperFacing)
                .map(destinationResult -> {
                    IItemHandler itemHandler = destinationResult.getKey();
                    Object destination = destinationResult.getValue();
                    if (isFull(itemHandler))
                    {
                        return false;
                    }
            else
            {
                for (int i = 0; i < tileEntityUpper.getContainerSize(); ++i)
                {
                    if (!tileEntityUpper.getItem(i).isEmpty())
                    {
                        ItemStack originalSlotContents = tileEntityUpper.getItem(i).copy();
                        ItemStack insertStack = tileEntityUpper.removeItem(i, 1);
                        ItemStack remainder = putStackInInventoryAllSlots(tileEntityUpper, destination, itemHandler, insertStack);

                        if (remainder.isEmpty())
                        {
                            return true;
                        }

                        tileEntityUpper.setItem(i, originalSlotContents);
                    }
                }

                return false;
            }
                })
                .orElse(false);
    }

    private static ItemStack putStackInInventoryAllSlots(BlockEntity source, Object destination, IItemHandler destInventory, ItemStack stack)
    {
        for (int slot = 0; slot < destInventory.getSlots() && !stack.isEmpty(); slot++)
        {
            stack = insertStack(source, destination, destInventory, stack, slot);
        }
        return stack;
    }

    private static ItemStack insertStack(BlockEntity source, Object destination, IItemHandler destInventory, ItemStack stack, int slot)
    {
        ItemStack itemstack = destInventory.getStackInSlot(slot);

        if (destInventory.insertItem(slot, stack, true).isEmpty())
        {
            boolean insertedItem = false;
            boolean inventoryWasEmpty = isEmpty(destInventory);

            if (itemstack.isEmpty())
            {
                destInventory.insertItem(slot, stack, false);
                stack = ItemStack.EMPTY;
                insertedItem = true;
            }
            else if (ItemHandlerHelper.canItemStacksStack(itemstack, stack))
            {
                int originalSize = stack.getCount();
                stack = destInventory.insertItem(slot, stack, false);
                insertedItem = originalSize < stack.getCount();
            }

            if (insertedItem)
            {
                if (inventoryWasEmpty && destination instanceof UpperBlockEntity)
                {
                    UpperBlockEntity destinationUpper = (UpperBlockEntity)destination;

                    if (!destinationUpper.isOnCustomCooldown())
                    {
                        int k = 0;

                        if (source instanceof UpperBlockEntity)
                        {
                            if (destinationUpper.getLastUpdateTime() >= ((UpperBlockEntity) source).getLastUpdateTime())
                            {
                                k = 1;
                            }
                        }

                        destinationUpper.setCooldown(8 - k);
                    }
                }
            }
        }

        return stack;
    }

    private static Optional<Pair<IItemHandler, Object>> getItemHandler(Level level, IUpper upper, Direction upperFacing)
    {
        double x = upper.getLevelX() + (double) upperFacing.getStepX();
        double y = upper.getLevelY() + (double) upperFacing.getStepY();
        double z = upper.getLevelZ() + (double) upperFacing.getStepZ();
        return getItemHandler(level, x, y, z, upperFacing.getOpposite());
    }

    private static boolean isFull(IItemHandler itemHandler)
    {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            ItemStack stackInSlot = itemHandler.getStackInSlot(slot);
            if (stackInSlot.isEmpty() || stackInSlot.getCount() != stackInSlot.getMaxStackSize())
            {
                return false;
            }
        }
        return true;
    }

    private static boolean isEmpty(IItemHandler itemHandler)
    {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            ItemStack stackInSlot = itemHandler.getStackInSlot(slot);
            if (stackInSlot.getCount() > 0)
            {
                return false;
            }
        }
        return true;
    }

    public static Optional<Pair<IItemHandler, Object>> getItemHandler(Level level, double x, double y, double z, final Direction side)
    {
        int i = Mth.floor(x);
        int j = Mth.floor(y);
        int k = Mth.floor(z);
        BlockPos blockpos = new BlockPos(i, j, k);
        BlockState state = level.getBlockState(blockpos);

        if (state.hasBlockEntity())
        {
            BlockEntity tileentity = level .getBlockEntity(blockpos);
            if (tileentity != null)
            {
                return tileentity.getCapability(ForgeCapabilities.ITEM_HANDLER, side)
                    .map(capability -> ImmutablePair.<IItemHandler, Object>of(capability, tileentity));
            }
        }

        return Optional.empty();
    }
}