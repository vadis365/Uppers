package uppers.tiles;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ISidedInventoryProvider;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.HopperContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.IHopper;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import uppers.ModBlocks;
import uppers.blocks.UpperBlock;

public class UpperTileEntity  extends LockableLootTileEntity implements IHopper, ITickableTileEntity {
	private NonNullList<ItemStack> inventory = NonNullList.<ItemStack>withSize(5, ItemStack.EMPTY);
	private int transferCooldown = -1;
	private long tickedGameTime;
	
	public UpperTileEntity() {
		super(ModBlocks.UPPER_TILE);
	}
	
	@Override
	public void read(CompoundNBT compound) {
		super.read(compound);
		inventory = NonNullList.<ItemStack>withSize(getSizeInventory(), ItemStack.EMPTY);
		if (!checkLootAndRead(compound))
			ItemStackHelper.loadAllItems(compound, inventory);
		if (compound.contains("CustomName", 8))
			setCustomName(ITextComponent.Serializer.fromJson(compound.getString("CustomName")));
		transferCooldown = compound.getInt("TransferCooldown");
	}

	@Override
	public CompoundNBT write(CompoundNBT compound) {
		super.write(compound);
		if (!checkLootAndWrite(compound))
			ItemStackHelper.saveAllItems(compound, inventory);
		compound.putInt("TransferCooldown", transferCooldown);
		ITextComponent itextcomponent = getCustomName();
		if (itextcomponent != null)
			compound.putString("CustomName", ITextComponent.Serializer.toJson(itextcomponent));
		return compound;
	}

	@Override
	public int getSizeInventory() {
		return inventory.size();
	}

	@Override
	public ItemStack decrStackSize(int index, int count) {
		fillWithLoot((PlayerEntity) null);
		return ItemStackHelper.getAndSplit(getItems(), index, count);
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		fillWithLoot((PlayerEntity) null);
		getItems().set(index, stack);
		if (stack.getCount() > getInventoryStackLimit())
			stack.setCount(getInventoryStackLimit());
	}

	@Override
	public ITextComponent getName() {
		return (ITextComponent) (getCustomName() != null ? getCustomName() : new TranslationTextComponent("container.upper", new Object[0]));
	}

	@Override
	protected ITextComponent getDefaultName() {
		return new TranslationTextComponent("container.upper", new Object[0]);
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

//	@Override
//	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
//		return oldState.getBlock() != newState.getBlock();
//	}

	@Override
	public void tick() {
		if (world != null && !world.isRemote) {
			--transferCooldown;
			tickedGameTime = world.getGameTime();
			if (!isOnTransferCooldown()) {
				setTransferCooldown(0);
				updateUpper(() -> {
					return pullItems(this);
				});
			}

		}
	}

	private boolean updateUpper(Supplier<Boolean> supplier) {
		if (world != null && !world.isRemote) {
			if (!isOnTransferCooldown() && getBlockState().get(UpperBlock.ENABLED)) {
				boolean flag = false;
				if (!isInventoryEmpty())
					flag = transferItemsOut();

				if (!isFull())
					flag |= supplier.get();

				if (flag) {
					setTransferCooldown(8);
					markDirty();
					return true;
				}
			}
			return false;
		} else
			return false;
	}

	private boolean isInventoryEmpty() {
		for (ItemStack itemstack : inventory)
			if (!itemstack.isEmpty())
				return false;
		return true;
	}

	@Override
	public boolean isEmpty() {
		return isInventoryEmpty();
	}

	private boolean isFull() {
		for (ItemStack itemstack : inventory)
			if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize())
				return false;
		return true;
	}

	private boolean transferItemsOut() {
		if (InventoryCodeHooksTweaked.insertHook(this))
			return true;
		IInventory iinventory = getInventoryForUpperTransfer();
		if (iinventory == null)
			return false;
		else {
			Direction direction = ((Direction) getBlockState().get(UpperBlock.FACING)).getOpposite();
			if (isInventoryFull(iinventory, direction))
				return false;
			else {
				for (int i = 0; i < getSizeInventory(); ++i) {
					if (!getStackInSlot(i).isEmpty()) {
						ItemStack itemstack = getStackInSlot(i).copy();
						ItemStack itemstack1 = putStackInInventoryAllSlots(this, iinventory, decrStackSize(i, 1), direction);
						if (itemstack1.isEmpty()) {
							iinventory.markDirty();
							return true;
						}
						setInventorySlotContents(i, itemstack);
					}
				}
				return false;
			}
		}
	}

	private boolean isInventoryFull(IInventory inventoryIn, Direction side) {
		if (inventoryIn instanceof ISidedInventory) {
			ISidedInventory isidedinventory = (ISidedInventory) inventoryIn;
			int[] aint = isidedinventory.getSlotsForFace(side);
			for (int k : aint) {
				ItemStack itemstack1 = isidedinventory.getStackInSlot(k);
				if (itemstack1.isEmpty() || itemstack1.getCount() != itemstack1.getMaxStackSize())
					return false;
			}
		} else {
			int i = inventoryIn.getSizeInventory();
			for (int j = 0; j < i; ++j) {
				ItemStack itemstack = inventoryIn.getStackInSlot(j);
				if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize())
					return false;
			}
		}
		return true;
	}

	private static boolean isInventoryEmpty(IInventory inventoryIn, Direction side) {
		if (inventoryIn instanceof ISidedInventory) {
			ISidedInventory isidedinventory = (ISidedInventory) inventoryIn;
			int[] aint = isidedinventory.getSlotsForFace(side);
			for (int i : aint)
				if (!isidedinventory.getStackInSlot(i).isEmpty())
					return false;
		} else {
			int j = inventoryIn.getSizeInventory();
			for (int k = 0; k < j; ++k)
				if (!inventoryIn.getStackInSlot(k).isEmpty())
					return false;
		}
		return true;
	}
	
	public static boolean pullItems(IHopper upper) {
		Boolean ret = InventoryCodeHooksTweaked.extractHook(upper);
		if (ret != null)
			return ret;
		IInventory iinventory = getSourceInventory(upper);
		if (iinventory != null) {
			Direction direction = Direction.UP;
			if (isInventoryEmpty(iinventory, direction))
				return false;
			if (iinventory instanceof ISidedInventory) {
				ISidedInventory isidedinventory = (ISidedInventory) iinventory;
				int[] aint = isidedinventory.getSlotsForFace(direction);
				for (int i : aint)
					if (pullItemFromSlot(upper, iinventory, i, direction))
						return true;
			} else {
				int j = iinventory.getSizeInventory();
				for (int k = 0; k < j; ++k)
					if (pullItemFromSlot(upper, iinventory, k, direction))
						return true;
			}
		} else {
			for (ItemEntity entityitem : getCaptureItems(upper))
				if (captureItem(upper, entityitem))
					return true;
		}
		return false;
	}

	private static boolean pullItemFromSlot(IHopper upper, IInventory inventoryIn, int index, Direction direction) {
		ItemStack itemstack = inventoryIn.getStackInSlot(index);
		if (!itemstack.isEmpty() && canExtractItemFromSlot(inventoryIn, itemstack, index, direction)) {
			ItemStack itemstack1 = itemstack.copy();
			ItemStack itemstack2 = putStackInInventoryAllSlots(inventoryIn, upper, inventoryIn.decrStackSize(index, 1), (Direction) null);
			if (itemstack2.isEmpty()) {
				inventoryIn.markDirty();
				return true;
			}
			inventoryIn.setInventorySlotContents(index, itemstack1);
		}
		return false;
	}

	public static boolean captureItem(IInventory destination, ItemEntity entity) {
		boolean flag = false;
		ItemStack itemstack = entity.getItem().copy();
		ItemStack itemstack1 = putStackInInventoryAllSlots((IInventory) null, destination, itemstack, (Direction) null);
		if (itemstack1.isEmpty()) {
			flag = true;
			entity.remove();
		} else
			entity.setItem(itemstack1);
		return flag;
	}

	@Override
	protected IItemHandler createUnSidedHandler() {
		return new UpperItemHandler(this);
	}
	
	//   @Override
	//   protected net.minecraftforge.items.IItemHandler createUnSidedHandler() {
	//      return new net.minecraftforge.items.VanillaHopperItemHandler(this);
	//   }

	public static ItemStack putStackInInventoryAllSlots(@Nullable IInventory source, IInventory destination, ItemStack stack, @Nullable Direction direction) {
		if (destination instanceof ISidedInventory && direction != null) {
			ISidedInventory isidedinventory = (ISidedInventory) destination;
			int[] aint = isidedinventory.getSlotsForFace(direction);
			for (int k = 0; k < aint.length && !stack.isEmpty(); ++k)
				stack = insertStack(source, destination, stack, aint[k], direction);
		} else {
			int i = destination.getSizeInventory();
			for (int j = 0; j < i && !stack.isEmpty(); ++j)
				stack = insertStack(source, destination, stack, j, direction);
		}
		return stack;
	}

	private static boolean canInsertItemInSlot(IInventory inventoryIn, ItemStack stack, int index, @Nullable Direction side) {
		if (!inventoryIn.isItemValidForSlot(index, stack))
			return false;
		else
			return !(inventoryIn instanceof ISidedInventory) || ((ISidedInventory) inventoryIn).canInsertItem(index, stack, side);
	}

	private static boolean canExtractItemFromSlot(IInventory inventoryIn, ItemStack stack, int index, Direction side) {
		return !(inventoryIn instanceof ISidedInventory) || ((ISidedInventory) inventoryIn).canExtractItem(index, stack, side);
	}

	private static ItemStack insertStack(@Nullable IInventory source, IInventory destination, ItemStack stack, int index, @Nullable Direction direction) {
		ItemStack itemstack = destination.getStackInSlot(index);
		if (canInsertItemInSlot(destination, stack, index, direction)) {
			boolean flag = false;
			boolean flag1 = destination.isEmpty();
			if (itemstack.isEmpty()) {
				destination.setInventorySlotContents(index, stack);
				stack = ItemStack.EMPTY;
				flag = true;
			} else if (canCombine(itemstack, stack)) {
				int i = stack.getMaxStackSize() - itemstack.getCount();
				int j = Math.min(stack.getCount(), i);
				stack.shrink(j);
				itemstack.grow(j);
				flag = j > 0;
			}
			if (flag) {
				if (flag1 && destination instanceof UpperTileEntity) {
					UpperTileEntity tileentityupper1 = (UpperTileEntity) destination;
					if (!tileentityupper1.mayTransfer()) {
						int k = 0;
						if (source instanceof UpperTileEntity) {
							UpperTileEntity tileentityupper = (UpperTileEntity) source;
							if (tileentityupper1.tickedGameTime >= tileentityupper.tickedGameTime)
								k = 1;
						}
						tileentityupper1.setTransferCooldown(8 - k);
					}
				}
				destination.markDirty();
			}
		}
		return stack;
	}

	@Nullable
	private IInventory getInventoryForUpperTransfer() {
		Direction direction = getBlockState().get(UpperBlock.FACING);
		return getInventoryAtPosition(getWorld(), pos.offset(direction));
	}

	public static IInventory getSourceInventory(IHopper upper) {
		return getInventoryAtPosition(upper.getWorld(), upper.getXPos(), upper.getYPos() - 1.0D, upper.getZPos());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<ItemEntity> getCaptureItems(IHopper upper) {
		return (List) upper.getCollectionArea().toBoundingBoxList().stream().<ItemEntity>flatMap((something) -> {
					return upper.getWorld().getEntitiesWithinAABB(ItemEntity.class, something.offset(upper.getXPos() - 0.5D, upper.getYPos() - 0.5D, upper.getZPos() - 0.5D), EntityPredicates.IS_ALIVE).stream();
				}).collect(Collectors.toList());
	}

	@Nullable
	public static IInventory getInventoryAtPosition(World world, BlockPos pos) {
		return getInventoryAtPosition(world, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D);
	}

	@Nullable
	public static IInventory getInventoryAtPosition(World world, double x, double y, double z) {
		IInventory iinventory = null;
		BlockPos blockpos = new BlockPos(x, y, z);
		BlockState state = world.getBlockState(blockpos);
		Block block = state.getBlock();
		if (block instanceof ISidedInventoryProvider) {
			iinventory = ((ISidedInventoryProvider)block).createInventory(state, world, blockpos);
		} else if (state.hasTileEntity()) {
			TileEntity tileentity = world.getTileEntity(blockpos);
			if (tileentity instanceof IInventory) {
				iinventory = (IInventory) tileentity;
				if (iinventory instanceof ChestTileEntity && block instanceof ChestBlock) {
					iinventory = ChestBlock.getInventory(state, world, blockpos, true);
				}
			}
		}
		if (iinventory == null) {
			List<Entity> list = world.getEntitiesInAABBexcluding((Entity) null, new AxisAlignedBB(x - 0.5D, y - 0.5D, z - 0.5D, x + 0.5D, y + 0.5D, z + 0.5D), EntityPredicates.HAS_INVENTORY);

			if (!list.isEmpty())
				iinventory = (IInventory) list.get(world.rand.nextInt(list.size()));
		}
		return iinventory;
	}

	private static boolean canCombine(ItemStack stack1, ItemStack stack2) {
		if (stack1.getItem() != stack2.getItem()) {
			return false;
		} else if (stack1.getDamage() != stack2.getDamage()) {
			return false;
		} else if (stack1.getCount() > stack1.getMaxStackSize()) {
			return false;
		} else {
			return ItemStack.areItemStackTagsEqual(stack1, stack2);
		}
	}

	@Override
	public double getXPos() {
		return (double) pos.getX() + 0.5D;
	}

	@Override
	public double getYPos() {
		return (double) pos.getY() + 0.5D;
	}

	@Override
	public double getZPos() {
		return (double) pos.getZ() + 0.5D;
	}

	public void setTransferCooldown(int ticks) {
		transferCooldown = ticks;
	}

	private boolean isOnTransferCooldown() {
		return transferCooldown > 0;
	}

	public boolean mayTransfer() {
		return transferCooldown > 8;
	}

	@Override
	protected Container createMenu(int id, PlayerInventory player) {
		return new HopperContainer(id, player, this);
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return inventory;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> itemsIn) {
		inventory = itemsIn;
	}

	public void onEntityCollision(Entity entity) {
		if (entity instanceof ItemEntity) {
			BlockPos blockpos = getPos();
			if (VoxelShapes.compare(
					VoxelShapes.create(entity.getBoundingBox().offset((double) (-blockpos.getX()), (double) (-blockpos.getY()), (double) (-blockpos.getZ()))),
					getCollectionArea(), IBooleanFunction.AND)) {
				updateUpper(() -> {
					return captureItem(this, (ItemEntity) entity);
				});
			}
		}

	}

	public long getLastUpdateTime() {
		return tickedGameTime;
	}
}