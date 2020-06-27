package uppers.tiles;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
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
    public static Boolean extractHook(IUpper dest)
    {
        // Lie, real direction is Down & Up, but Down & Down is more compatible with vanilla containers.
        return getItemHandler(dest, Direction.DOWN, Direction.DOWN).map(itemHandlerResult -> {

        IItemHandler handler = itemHandlerResult.getKey();

        for (int i = 0; i < handler.getSlots(); i++)
        {
            ItemStack extractItem = handler.extractItem(i, 1, true);
            if (!extractItem.isEmpty())
            {
                for (int j = 0; j < dest.getSizeInventory(); j++)
                {
                    ItemStack destStack = dest.getStackInSlot(j);
                    if (dest.isItemValidForSlot(j, extractItem) && (destStack.isEmpty() || destStack.getCount() < destStack.getMaxStackSize() && destStack.getCount() < dest.getInventoryStackLimit() && ItemHandlerHelper.canItemStacksStack(extractItem, destStack)))
                    {
                        extractItem = handler.extractItem(i, 1, false);
                        if (destStack.isEmpty())
                            dest.setInventorySlotContents(j, extractItem);
                        else
                        {
                            destStack.grow(1);
                            dest.setInventorySlotContents(j, destStack);
                        }
                        dest.markDirty();
                        return true;
                    }
                }
            }
        }
			return false;
		}).orElse(null); // TODO bad null
	}

    public static boolean insertHook(UpperTileEntity tileEntityUpper)
    {
    	Direction upperFacing = tileEntityUpper.getBlockState().get(UpperBlock.FACING);
        return getItemHandler(tileEntityUpper, upperFacing)
                .map(destinationResult -> {
                    IItemHandler itemHandler = destinationResult.getKey();
                    Object destination = destinationResult.getValue();
                    if (isFull(itemHandler))
                    {
                        return false;
                    }
            else
            {
                for (int i = 0; i < tileEntityUpper.getSizeInventory(); ++i)
                {
                    if (!tileEntityUpper.getStackInSlot(i).isEmpty())
                    {
                        ItemStack originalSlotContents = tileEntityUpper.getStackInSlot(i).copy();
                        ItemStack insertStack = tileEntityUpper.decrStackSize(i, 1);
                        ItemStack remainder = putStackInInventoryAllSlots(tileEntityUpper, destination, itemHandler, insertStack);

                        if (remainder.isEmpty())
                        {
                            return true;
                        }

                        tileEntityUpper.setInventorySlotContents(i, originalSlotContents);
                    }
                }

                return false;
            }
                })
                .orElse(false);
    }

    private static ItemStack putStackInInventoryAllSlots(TileEntity source, Object destination, IItemHandler destInventory, ItemStack stack)
    {
        for (int slot = 0; slot < destInventory.getSlots() && !stack.isEmpty(); slot++)
        {
            stack = insertStack(source, destination, destInventory, stack, slot);
        }
        return stack;
    }

    private static ItemStack insertStack(TileEntity source, Object destination, IItemHandler destInventory, ItemStack stack, int slot)
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
                if (inventoryWasEmpty && destination instanceof UpperTileEntity)
                {
                    UpperTileEntity destinationUpper = (UpperTileEntity)destination;

                    if (!destinationUpper.mayTransfer())
                    {
                        int k = 0;

                        if (source instanceof UpperTileEntity)
                        {
                            if (destinationUpper.getLastUpdateTime() >= ((UpperTileEntity) source).getLastUpdateTime())
                            {
                                k = 1;
                            }
                        }

                        destinationUpper.setTransferCooldown(8 - k);
                    }
                }
            }
        }

        return stack;
    }

    private static LazyOptional<Pair<IItemHandler, Object>> getItemHandler(IUpper upper, Direction upperFacing) {
        return getItemHandler(upper, upperFacing, upperFacing.getOpposite());
    }
    private static LazyOptional<Pair<IItemHandler, Object>> getItemHandler(IUpper upper, Direction upperFacing, Direction invetoryFacing)
    {
        double x = upper.getXPos() + (double) upperFacing.getXOffset();
        double y = upper.getYPos() + (double) upperFacing.getYOffset();
        double z = upper.getZPos() + (double) upperFacing.getZOffset();
        return getItemHandler(upper.getWorld(), x, y, z, invetoryFacing);
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

    public static LazyOptional<Pair<IItemHandler, Object>> getItemHandler(World worldIn, double x, double y, double z, final Direction side)
    {
        int i = MathHelper.floor(x);
        int j = MathHelper.floor(y);
        int k = MathHelper.floor(z);
        BlockPos blockpos = new BlockPos(i, j, k);
        BlockState state = worldIn.getBlockState(blockpos);

        if (state.hasTileEntity())
        {
            TileEntity tileentity = worldIn.getTileEntity(blockpos);
            if (tileentity != null)
            {
                return tileentity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)
                    .map(capability -> ImmutablePair.<IItemHandler, Object>of(capability, tileentity));
            }
        }

        return LazyOptional.empty();
    }
}