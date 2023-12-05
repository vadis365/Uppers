package uppers.tiles;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.items.IItemHandler;
import uppers.Uppers;
import uppers.blocks.UpperBlock;

public class UpperBlockEntity extends RandomizableContainerBlockEntity implements IUpper {
	private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
	private int cooldownTime = -1;	
	private long tickedGameTime;

	public UpperBlockEntity(BlockPos pos, BlockState state) {
		super(Uppers.UPPER_TILE.get(), pos, state);
	}

	@Override
	public void load(CompoundTag compound) {
		super.load(compound);
		items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		if (!tryLoadLootTable(compound))
			ContainerHelper.loadAllItems(compound, items);
	//	if (compound.contains("CustomName", 8))
		//	setCustomName(Component.Serializer.fromJson(compound.getString("CustomName")));
		cooldownTime = compound.getInt("TransferCooldown");
	}

	@Override
	protected void saveAdditional(CompoundTag compound) {
		super.saveAdditional(compound);
		if (!trySaveLootTable(compound))
			ContainerHelper.saveAllItems(compound, items);
		compound.putInt("TransferCooldown", cooldownTime);
	//	Component itextcomponent = getCustomName();
	//	if (itextcomponent != null)
		//	compound.putString("CustomName", Component.Serializer.toJson(itextcomponent));
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
					flag = ejectItems(level, pos, state, blockEntity);

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

	private static boolean ejectItems(Level level, BlockPos pos, BlockState state, UpperBlockEntity blockEntity) {
		if (InventoryCodeHooksTweaked.insertHook(blockEntity))
			return true;
		Container container = getAttachedContainer(level, pos, state);
		if (container == null)
			return false;
		else {
			Direction direction = state.getValue(UpperBlock.FACING).getOpposite();
			if (isFullContainer(container, direction))
				return false;
			else {
				for (int i = 0; i < blockEntity.getContainerSize(); ++i) {
					if (!blockEntity.getItem(i).isEmpty()) {
						ItemStack itemstack = blockEntity.getItem(i).copy();
						ItemStack itemstack1 = addItem(blockEntity, container, blockEntity.removeItem(i, 1), direction);
						if (itemstack1.isEmpty()) {
							container.setChanged();
							return true;
						}
						blockEntity.setItem(i, itemstack);
					}
				}
				return false;
			}
		}
	}

	private static IntStream getSlots(Container inventoryIn, Direction direction) {
		      return inventoryIn instanceof WorldlyContainer ? IntStream.of(((WorldlyContainer)inventoryIn).getSlotsForFace(direction)) : IntStream.range(0, inventoryIn.getContainerSize());
	}

	private static boolean isFullContainer(Container inventoryIn, Direction side) {
		return getSlots(inventoryIn, side).allMatch((stackInSlot) -> {
			ItemStack itemstack = inventoryIn.getItem(stackInSlot);
			return itemstack.getCount() >= itemstack.getMaxStackSize();
		});
	}

	private static boolean isEmptyContainer(Container inventoryIn, Direction side) {
		return getSlots(inventoryIn, side).allMatch((stackInSlot) -> {
			return inventoryIn.getItem(stackInSlot).isEmpty();
		});
	}

	public static boolean suckInItems(Level level, IUpper upper) {
		Boolean ret = InventoryCodeHooksTweaked.extractHook(level, upper);
		if (ret != null)
			return ret;
		Container container = getSourceContainer(level, upper);
		if (container != null) {
			Direction direction = Direction.DOWN;
	         return isEmptyContainer(container, direction) ? false : getSlots(container, direction).anyMatch((inventoryIn) -> {
	             return tryTakeInItemFromSlot(upper, container, inventoryIn, direction);
	          });
	       } else {
	          for(ItemEntity itementity : getItemsAtAndAbove(level, upper)) {
	             if (addItem(upper, itementity)) {
	                return true;
	             }
	          }
	          return false;
	       }
	    }

	private static boolean tryTakeInItemFromSlot(IUpper upper, Container inventoryIn, int index, Direction direction) {
		ItemStack itemstack = inventoryIn.getItem(index);
		if (!itemstack.isEmpty() && canTakeItemFromContainer(inventoryIn, itemstack, index, direction)) {
			ItemStack itemstack1 = itemstack.copy();
			ItemStack itemstack2 = addItem(inventoryIn, upper, inventoryIn.removeItem(index, 1), (Direction) null);
			if (itemstack2.isEmpty()) {
				inventoryIn.setChanged();
				return true;
			}
			inventoryIn.setItem(index, itemstack1);
		}
		return false;
	}

	public static boolean addItem(Container destination, ItemEntity entity) {
		boolean flag = false;
		ItemStack itemstack = entity.getItem().copy();
		ItemStack itemstack1 = addItem((Container) null, destination, itemstack, (Direction) null);
		if (itemstack1.isEmpty()) {
			flag = true;
			entity.discard();
		} else
			entity.setItem(itemstack1);
		return flag;
	}

	@Override
	protected IItemHandler createUnSidedHandler() {
		return new UpperItemHandler(this);
	}

	public static ItemStack addItem(@Nullable Container source, Container destination, ItemStack stack, @Nullable Direction direction) {
		if (destination instanceof WorldlyContainer && direction != null) {
			WorldlyContainer isidedinventory = (WorldlyContainer) destination;
			int[] aint = isidedinventory.getSlotsForFace(direction);
			for (int k = 0; k < aint.length && !stack.isEmpty(); ++k)
				stack = tryMoveInItem(source, destination, stack, aint[k], direction);
		} else {
			int i = destination.getContainerSize();
			for (int j = 0; j < i && !stack.isEmpty(); ++j)
				stack = tryMoveInItem(source, destination, stack, j, direction);
		}
		return stack;
	}

	private static boolean canPlaceItemInContainer(Container inventoryIn, ItemStack stack, int index, @Nullable Direction side) {
		if (!inventoryIn.canPlaceItem(index, stack))
			return false;
		else
			return !(inventoryIn instanceof WorldlyContainer) || ((WorldlyContainer) inventoryIn).canPlaceItemThroughFace(index, stack, side);
	}

	private static boolean canTakeItemFromContainer(Container inventoryIn, ItemStack stack, int index, Direction side) {
		return !(inventoryIn instanceof WorldlyContainer) || ((WorldlyContainer) inventoryIn).canTakeItemThroughFace(index, stack, side);
	}

	private static ItemStack tryMoveInItem(@Nullable Container source, Container destination, ItemStack stack, int index, @Nullable Direction direction) {
		ItemStack itemstack = destination.getItem(index);
		if (canPlaceItemInContainer(destination, stack, index, direction)) {
			boolean flag = false;
			boolean flag1 = destination.isEmpty();
			if (itemstack.isEmpty()) {
				destination.setItem(index, stack);
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
				if (flag1 && destination instanceof UpperBlockEntity) {
					UpperBlockEntity tileentityupper1 = (UpperBlockEntity) destination;
					if (!tileentityupper1.isOnCustomCooldown()) {
						int k = 0;
						if (source instanceof UpperBlockEntity) {
							UpperBlockEntity tileentityupper = (UpperBlockEntity) source;
							if (tileentityupper1.tickedGameTime >= tileentityupper.tickedGameTime)
								k = 1;
						}
						tileentityupper1.setCooldown(8 - k);
					}
				}
				destination.setChanged();
			}
		}
		return stack;
	}

	@Nullable
	private static Container getAttachedContainer(Level level, BlockPos pos, BlockState state) {
		Direction direction = state.getValue(UpperBlock.FACING);
		return getContainerAt(level, pos.relative(direction));
	}

	public static Container getSourceContainer(Level level, IUpper upper) {
		return getContainerAt(level, upper.getLevelX(), upper.getLevelY() - 1.0D, upper.getLevelZ());
	}

	public static List<ItemEntity> getItemsAtAndAbove(Level level, IUpper upper) {
		return upper.getSuckShape().toAabbs().stream().flatMap((something) -> {
					return level.getEntitiesOfClass(ItemEntity.class, something.move(upper.getLevelX() - 0.5D, upper.getLevelY() - 0.5D, upper.getLevelZ() - 0.5D), EntitySelector.ENTITY_STILL_ALIVE).stream();
				}).collect(Collectors.toList());
	}

	@Nullable
	public static Container getContainerAt(Level level, BlockPos pos) {
		return getContainerAt(level, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D);
	}

	@Nullable
	public static Container getContainerAt(Level level, double x, double y, double z) {
		Container container = null;
		BlockPos blockpos = BlockPos.containing(x, y, z);
		BlockState state = level.getBlockState(blockpos);
		Block block = state.getBlock();
		if (block instanceof WorldlyContainerHolder) {
			container = ((WorldlyContainerHolder)block).getContainer(state, level, blockpos);
		} else if (state.hasBlockEntity()) {
			BlockEntity tileentity = level.getBlockEntity(blockpos);
			if (tileentity instanceof Container) {
				container = (Container) tileentity;
				if (container instanceof ChestBlockEntity && block instanceof ChestBlock) {
					container = ChestBlock.getContainer((ChestBlock)block, state, level, blockpos, true); //getInventory
				}
			}
		}
		if (container == null) {
			List<Entity> list = level.getEntities((Entity) null, new AABB(x - 0.5D, y - 0.5D, z - 0.5D, x + 0.5D, y + 0.5D, z + 0.5D), EntitySelector.CONTAINER_ENTITY_SELECTOR);

			if (!list.isEmpty())
				container = (Container) list.get(level.random.nextInt(list.size()));
		}
		return container;
	}

	private static boolean canMergeItems(ItemStack stack1, ItemStack stack2) {
		if (stack1.getItem() != stack2.getItem()) {
			return false;
		} else if (stack1.getDamageValue() != stack2.getDamageValue()) {
			return false;
		} else if (stack1.getCount() > stack1.getMaxStackSize()) {
			return false;
		} else {
			return ItemStack.matches(stack1, stack2);
		}
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

	public void setCooldown(int ticks) {
		cooldownTime = ticks;
	}

	private boolean isOnCooldown() {
		return cooldownTime > 0;
	}

	public boolean isOnCustomCooldown() {
		return cooldownTime > 8;
	}

	@Override
	protected AbstractContainerMenu createMenu(int id,Inventory inventory) {
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
		if (entity instanceof ItemEntity && Shapes.joinIsNotEmpty(
				Shapes.create(entity.getBoundingBox().move((double) (-pos.getX()), (double) (-pos.getY()), (double) (-pos.getZ()))), blockEntity.getSuckShape(), BooleanOp.AND)) {
			tryMoveItems(level, pos,state, blockEntity, () -> {
				return addItem(blockEntity, (ItemEntity) entity);
			});
		}

	}

	public long getLastUpdateTime() {
		return tickedGameTime;
	}
}