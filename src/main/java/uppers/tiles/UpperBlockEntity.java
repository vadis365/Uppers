package uppers.tiles;

import java.util.List;
import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import uppers.Uppers;
import uppers.blocks.UpperBlock;

public class UpperBlockEntity extends RandomizableContainerBlockEntity implements IUpper {
	public static final int MOVE_ITEM_SPEED = 8;
	public static final int UPPER_CONTAINER_SIZE = 5;
	private static final int[][] CACHED_SLOTS = new int[54][];
	private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
	private int cooldownTime = -1;	
	private long tickedGameTime;
	private Direction facing;
	public UpperBlockEntity(BlockPos pos, BlockState state) {
		super(Uppers.UPPER_TILE.get(), pos, state);
		this.facing = state.getValue(UpperBlock.FACING);
	}

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    	super.loadAdditional(tag, registries);
		items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		if (!tryLoadLootTable(tag))
			ContainerHelper.loadAllItems(tag, items, registries);
		cooldownTime = tag.getInt("TransferCooldown");
	}

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
		if (!trySaveLootTable(tag))
			ContainerHelper.saveAllItems(tag, items, registries);
		tag.putInt("TransferCooldown", cooldownTime);
	}

	   public int getContainerSize() {
		      return this.items.size();
		   }

	@Override
	public ItemStack removeItem(int index, int count) {
		unpackLootTable((Player) null);
		return ContainerHelper.removeItem(getItems(), index, count);
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		unpackLootTable((Player) null);
		getItems().set(index, stack);
		if (stack.getCount() > getMaxStackSize())
			stack.setCount(getMaxStackSize());
	}
	
    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        facing = state.getValue(UpperBlock.FACING);
    }

	protected Component getDefaultName() {
		return Component.translatable("container.upper", new Object[0]);
	}

	 public static void pushItemsTick(Level level, BlockPos pos, BlockState state, UpperBlockEntity blockEntity) {
	      --blockEntity.cooldownTime;
	      blockEntity.tickedGameTime = level.getGameTime();
	      if (!blockEntity.isOnCooldown()) {
	         blockEntity.setCooldown(0);
	         tryMoveItems(level, pos, state, blockEntity, () -> {
	            return suckInItems(level, blockEntity);
	         });
	      }
	}

	private static boolean tryMoveItems(Level level, BlockPos pos, BlockState state, UpperBlockEntity blockEntity, BooleanSupplier supplier) {
		if (level.isClientSide) {
			return false;
    	} else {
			if (!blockEntity.isOnCooldown() && state.getValue(UpperBlock.ENABLED)) {
				boolean flag = false;
				if (!blockEntity.isEmpty())
					flag = ejectItems(level, pos, blockEntity);

				if (!blockEntity.inventoryFull())
					flag |= supplier.getAsBoolean();

				if (flag) {
					blockEntity.setCooldown(8);
					setChanged(level, pos, state);
					return true;
				}
			}
			return false;
    	}
	}

	private boolean inventoryFull() {
		for (ItemStack itemstack : items)
			if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize())
				return false;
		return true;
	}

	private static boolean ejectItems(Level level, BlockPos pos, UpperBlockEntity blockEntity) {
		if (InventoryCodeHooksTweaked.insertHook(blockEntity))
			return true;
		Container container = getAttachedContainer(level, pos, blockEntity);
		if (container == null)
			return false;
		else {
			Direction direction = blockEntity.facing.getOpposite();
			if (isFullContainer(container, direction))
				return false;
			else {
                for (int i = 0; i < blockEntity.getContainerSize(); i++) {
                    ItemStack itemstack = blockEntity.getItem(i);
                    if (!itemstack.isEmpty()) {
                        int j = itemstack.getCount();
                        ItemStack itemstack1 = addItem(blockEntity, container, blockEntity.removeItem(i, 1), direction);
                        if (itemstack1.isEmpty()) {
                            container.setChanged();
                            return true;
                        }

                        itemstack.setCount(j);
                        if (j == 1) {
                            blockEntity.setItem(i, itemstack);
                        }
                    }
                }

				return false;
			}
		}
	}

	private static int[] getSlots(Container pContainer, Direction pDirection) {
        if (pContainer instanceof WorldlyContainer worldlycontainer) {
            return worldlycontainer.getSlotsForFace(pDirection);
        } else {
            int i = pContainer.getContainerSize();
            if (i < CACHED_SLOTS.length) {
                int[] aint = CACHED_SLOTS[i];
                if (aint != null) {
                    return aint;
                } else {
                    int[] aint1 = createFlatSlots(i);
                    CACHED_SLOTS[i] = aint1;
                    return aint1;
                }
            } else {
                return createFlatSlots(i);
            }
        }
    }

    private static int[] createFlatSlots(int pSize) {
        int[] aint = new int[pSize];
        int i = 0;

        while (i < aint.length) {
            aint[i] = i++;
        }

        return aint;
    }
    
    private static boolean isFullContainer(Container pContainer, Direction pDirection) {
        int[] aint = getSlots(pContainer, pDirection);

        for (int i : aint) {
            ItemStack itemstack = pContainer.getItem(i);
            if (itemstack.getCount() < itemstack.getMaxStackSize()) {
                return false;
            }
        }

        return true;
    }
/*
	private static boolean isEmptyContainer(Container inventoryIn, Direction side) {
		return getSlots(inventoryIn, side).allMatch((stackInSlot) -> {
			return inventoryIn.getItem(stackInSlot).isEmpty();
		});
	}
*/
	public static boolean suckInItems(Level level, IUpper upper) {
		BlockPos blockpos = BlockPos.containing(upper.getLevelX(), upper.getLevelY() + 1.0, upper.getLevelZ());
	    BlockState blockstate = level.getBlockState(blockpos);
		Boolean ret = InventoryCodeHooksTweaked.extractHook(level, upper);
		if (ret != null)
			return ret;
		Container container = getSourceContainer(level, upper, blockpos, blockstate);
		if (container != null) {
			Direction direction = Direction.DOWN;

            for (int i : getSlots(container, direction)) {
                if (tryTakeInItemFromSlot(upper, container, i, direction)) {
                    return true;
                }
            }

            return false;
        } else {
            boolean flag = upper.isGridAligned()
                && blockstate.isCollisionShapeFullBlock(level, blockpos)
                && !blockstate.is(BlockTags.DOES_NOT_BLOCK_HOPPERS);
            if (!flag) {
                for (ItemEntity itementity : getItemsAtAndAbove(level, upper)) {
                    if (addItem(upper, itementity)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

	private static boolean tryTakeInItemFromSlot(IUpper upper, Container container, int slot, Direction direction) {
		ItemStack itemstack = container.getItem(slot);
		if (!itemstack.isEmpty() && canTakeItemFromContainer(upper, container, itemstack, slot, direction)) {
			int i = itemstack.getCount();
			 ItemStack itemstack1 = addItem(container, upper, container.removeItem(slot, 1), null);
	            if (itemstack1.isEmpty()) {
	                container.setChanged();
	                return true;
	            }

	            itemstack.setCount(i);
	            if (i == 1) {
	                container.setItem(slot, itemstack);
	            }
	        }

	        return false;
	    }

	public static boolean addItem(Container destination, ItemEntity entity) {
		boolean flag = false;
		ItemStack itemstack = entity.getItem().copy();
		ItemStack itemstack1 = addItem((Container) null, destination, itemstack, (Direction) null);
		if (itemstack1.isEmpty()) {
			flag = true;
			entity.setItem(ItemStack.EMPTY);
			entity.discard();
		} else
			entity.setItem(itemstack1);
		return flag;
	}

	public static ItemStack addItem(@Nullable Container source, Container destination, ItemStack stack, @Nullable Direction direction) {
		if (destination instanceof WorldlyContainer worldlycontainer && direction != null) {
			int[] aint = worldlycontainer.getSlotsForFace(direction);
			for (int k = 0; k < aint.length && !stack.isEmpty(); k++)
				stack = tryMoveInItem(source, destination, stack, aint[k], direction);
			return stack;
		}
		int i = destination.getContainerSize();
		for (int j = 0; j < i && !stack.isEmpty(); j++)
			stack = tryMoveInItem(source, destination, stack, j, direction);
		return stack;
	}

	private static boolean canPlaceItemInContainer(Container inventoryIn, ItemStack stack, int slot, @Nullable Direction side) {
		if (!inventoryIn.canPlaceItem(slot, stack))
			return false;
		else {
			if (inventoryIn instanceof WorldlyContainer worldlycontainer && !worldlycontainer.canPlaceItemThroughFace(slot, stack, side)) {
				return false;
			}
			return true;
		}
	}

	private static boolean canTakeItemFromContainer(Container source, Container destination, ItemStack stack, int slot, Direction direction) {
        if (!destination.canTakeItem(source, slot, stack)) {
            return false;
        } else {
            if (destination instanceof WorldlyContainer worldlycontainer && !worldlycontainer.canTakeItemThroughFace(slot, stack, direction))
                return false;
            return true;
        }
    }

	private static ItemStack tryMoveInItem(@Nullable Container source, Container destination, ItemStack stack, int slot, @Nullable Direction direction) {
		ItemStack itemstack = destination.getItem(slot);
		if (canPlaceItemInContainer(destination, stack, slot, direction)) {
			boolean flag = false;
			boolean flag1 = destination.isEmpty();
			if (itemstack.isEmpty()) {
				destination.setItem(slot, stack);
				stack = ItemStack.EMPTY;
				flag = true;
			} else if (canMergeItems(itemstack, stack)) {
				int i = stack.getMaxStackSize() - itemstack.getCount();
				int j = Math.min(stack.getCount(), i);
				stack.shrink(j);
				itemstack.grow(j);
				flag = j > 0;
			}
			if (flag) {
				if (flag1 && destination instanceof UpperBlockEntity upperblockentity1 && !upperblockentity1.isOnCustomCooldown()) {
					 int k = 0;
	                    if (source instanceof UpperBlockEntity upperblockentity && upperblockentity1.tickedGameTime >= upperblockentity.tickedGameTime) {
	                        k = 1;
	                    }
	                    upperblockentity1.setCooldown(8 - k);
	                }
	               destination.setChanged();
	            }
	        }
	        return stack;
	    }

	@Nullable
	private static Container getAttachedContainer(Level level, BlockPos pos, UpperBlockEntity blockEntity) {
		return getContainerAt(level, pos.relative(blockEntity.facing));
	}

	public static Container getSourceContainer(Level level, IUpper upper, BlockPos pos, BlockState state) {
		return getContainerAt(level, pos, state, upper.getLevelX(), upper.getLevelY() - 1.0D, upper.getLevelZ());
	}

    public static List<ItemEntity> getItemsAtAndAbove(Level pLevel, IUpper upper) {
        AABB aabb = upper.getSuckAabb().move(upper.getLevelX() - 0.5, upper.getLevelY() - 0.5, upper.getLevelZ() - 0.5);
        return pLevel.getEntitiesOfClass(ItemEntity.class, aabb, EntitySelector.ENTITY_STILL_ALIVE);
    }

	@Nullable
	public static Container getContainerAt(Level level, BlockPos pos) {
		return getContainerAt(level, pos, level.getBlockState(pos), (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D);
	}

    @Nullable
    private static Container getContainerAt(Level level, BlockPos pos, BlockState state, double x, double y, double z) {
        Container container = getBlockContainer(level, pos, state);
        if (container == null) {
            container = getEntityContainer(level, x, y, z);
        }

        return container;
    }
    
    @Nullable
    private static Container getBlockContainer(Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof WorldlyContainerHolder) {
            return ((WorldlyContainerHolder)block).getContainer(state, level, pos);
        } else if (state.hasBlockEntity() && level.getBlockEntity(pos) instanceof Container container) {
            if (container instanceof ChestBlockEntity && block instanceof ChestBlock) {
                container = ChestBlock.getContainer((ChestBlock)block, state, level, pos, true);
            }

            return container;
        } else {
            return null;
        }
    }

    @Nullable
    private static Container getEntityContainer(Level level, double x, double y, double z) {
        List<Entity> list = level.getEntities(
            (Entity)null,
            new AABB(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5),
            EntitySelector.CONTAINER_ENTITY_SELECTOR
        );
        return !list.isEmpty() ? (Container)list.get(level.random.nextInt(list.size())) : null;
    }

    private static boolean canMergeItems(ItemStack stack1, ItemStack stack2) {
        return stack1.getCount() <= stack1.getMaxStackSize() && ItemStack.isSameItemSameComponents(stack1, stack2);
    }

	@Override
	public double getLevelX() {
		return (double) this.worldPosition.getX() + 0.5D;
	}

	@Override
	public double getLevelY() {
		return (double) this.worldPosition.getY() + 0.5D;
	}

	@Override
	public double getLevelZ() {
		return (double) this.worldPosition.getZ() + 0.5D;
	}

    @Override
    public boolean isGridAligned() {
        return true;
    }

	public void setCooldown(int ticks) {
		cooldownTime = ticks;
	}

	private boolean isOnCooldown() {
		return cooldownTime > 0;
	}

	public boolean isOnCustomCooldown() {
		return cooldownTime > 8;
	}
//TODO
	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new HopperMenu(id, inventory, this);
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> itemsIn) {
		items = itemsIn;
	}
	
    public static void entityInside(Level level, BlockPos pos, BlockState state, Entity entity, UpperBlockEntity blockEntity) {
        if (entity instanceof ItemEntity itementity
            && !itementity.getItem().isEmpty()
            && entity.getBoundingBox()
                .move((double)(-pos.getX()), (double)(-pos.getY()), (double)(-pos.getZ()))
                .intersects(blockEntity.getSuckAabb())) {
            tryMoveItems(level, pos, state, blockEntity, () -> addItem(blockEntity, itementity));
        }
    }

	public long getLastUpdateTime() {
		return tickedGameTime;
	}
}