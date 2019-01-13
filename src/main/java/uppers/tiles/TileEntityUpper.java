package uppers.tiles;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerHopper;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.IHopper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.walkers.ItemStackDataLists;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import uppers.blocks.BlockUpper;

public class TileEntityUpper extends TileEntityLockableLoot implements IHopper, ITickable {
	private NonNullList<ItemStack> inventory = NonNullList.<ItemStack>withSize(5, ItemStack.EMPTY);
	private int transferCooldown = -1;
	private long tickedGameTime;

	public static void registerFixesUpper(DataFixer fixer) {
		fixer.registerWalker(FixTypes.BLOCK_ENTITY, new ItemStackDataLists(TileEntityUpper.class, new String[] { "Items" }));
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		this.inventory = NonNullList.<ItemStack>withSize(this.getSizeInventory(), ItemStack.EMPTY);

		if (!this.checkLootAndRead(compound)) {
			ItemStackHelper.loadAllItems(compound, this.inventory);
		}

		if (compound.hasKey("CustomName", 8)) {
			this.customName = compound.getString("CustomName");
		}

		this.transferCooldown = compound.getInteger("TransferCooldown");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);

		if (!this.checkLootAndWrite(compound)) {
			ItemStackHelper.saveAllItems(compound, this.inventory);
		}

		compound.setInteger("TransferCooldown", this.transferCooldown);

		if (this.hasCustomName()) {
			compound.setString("CustomName", this.customName);
		}

		return compound;
	}

	@Override
	public int getSizeInventory() {
		return this.inventory.size();
	}

	@Override
	public ItemStack decrStackSize(int index, int count) {
		this.fillWithLoot((EntityPlayer) null);
		ItemStack itemstack = ItemStackHelper.getAndSplit(this.getItems(), index, count);
		return itemstack;
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		this.fillWithLoot((EntityPlayer) null);
		this.getItems().set(index, stack);

		if (stack.getCount() > this.getInventoryStackLimit()) {
			stack.setCount(this.getInventoryStackLimit());
		}
	}

	@Override
	public String getName() {
		return this.hasCustomName() ? this.customName : "container.upper";
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		return oldState.getBlock() != newState.getBlock();
	}

	@Override
	public void update() {
		if (this.world != null && !this.world.isRemote) {
			--this.transferCooldown;
			this.tickedGameTime = this.world.getTotalWorldTime();

			if (!this.isOnTransferCooldown()) {
				this.setTransferCooldown(0);
				this.updateUpper();
			}
		}
	}

	protected boolean updateUpper() {
		if (this.world != null && !this.world.isRemote) {
			if (!this.isOnTransferCooldown() && BlockUpper.isEnabled(this.getBlockMetadata())) {
				boolean flag = false;

				if (!this.isInventoryEmpty()) {
					flag = this.transferItemsOut();
				}

				if (!this.isFull()) {
					flag = pullItems(this) || flag;
				}

				if (flag) {
					this.setTransferCooldown(8);
					this.markDirty();
					return true;
				}
			}

			return false;
		} else {
			return false;
		}
	}

	private boolean isInventoryEmpty() {
		for (ItemStack itemstack : this.inventory) {
			if (!itemstack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean isEmpty() {
		return this.isInventoryEmpty();
	}

	private boolean isFull() {
		for (ItemStack itemstack : this.inventory) {
			if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize()) {
				return false;
			}
		}

		return true;
	}

	private boolean transferItemsOut() {
		if (InventoryCodeHooksTweaked.insertHook(this)) {
			return true;
		}
		IInventory iinventory = this.getInventoryForUpperTransfer();

		if (iinventory == null) {
			return false;
		} else {
			EnumFacing enumfacing = BlockUpper.getFacing(this.getBlockMetadata()).getOpposite();

			if (this.isInventoryFull(iinventory, enumfacing)) {
				return false;
			} else {
				for (int i = 0; i < this.getSizeInventory(); ++i) {
					if (!this.getStackInSlot(i).isEmpty()) {
						ItemStack itemstack = this.getStackInSlot(i).copy();
						ItemStack itemstack1 = putStackInInventoryAllSlots(this, iinventory, this.decrStackSize(i, 1), enumfacing);

						if (itemstack1.isEmpty()) {
							iinventory.markDirty();
							return true;
						}

						this.setInventorySlotContents(i, itemstack);
					}
				}

				return false;
			}
		}
	}

	private boolean isInventoryFull(IInventory inventoryIn, EnumFacing side) {
		if (inventoryIn instanceof ISidedInventory) {
			ISidedInventory isidedinventory = (ISidedInventory) inventoryIn;
			int[] aint = isidedinventory.getSlotsForFace(side);

			for (int k : aint) {
				ItemStack itemstack1 = isidedinventory.getStackInSlot(k);

				if (itemstack1.isEmpty() || itemstack1.getCount() != itemstack1.getMaxStackSize()) {
					return false;
				}
			}
		} else {
			int i = inventoryIn.getSizeInventory();

			for (int j = 0; j < i; ++j) {
				ItemStack itemstack = inventoryIn.getStackInSlot(j);

				if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize()) {
					return false;
				}
			}
		}

		return true;
	}

	private static boolean isInventoryEmpty(IInventory inventoryIn, EnumFacing side) {
		if (inventoryIn instanceof ISidedInventory) {
			ISidedInventory isidedinventory = (ISidedInventory) inventoryIn;
			int[] aint = isidedinventory.getSlotsForFace(side);

			for (int i : aint) {
				if (!isidedinventory.getStackInSlot(i).isEmpty()) {
					return false;
				}
			}
		} else {
			int j = inventoryIn.getSizeInventory();

			for (int k = 0; k < j; ++k) {
				if (!inventoryIn.getStackInSlot(k).isEmpty()) {
					return false;
				}
			}
		}

		return true;
	}

	public static boolean pullItems(IHopper upper) {
		Boolean ret = InventoryCodeHooksTweaked.extractHook(upper);
		if (ret != null)
			return ret;
		IInventory iinventory = getSourceInventory(upper);

		if (iinventory != null) {
			EnumFacing enumfacing = EnumFacing.UP;

			if (isInventoryEmpty(iinventory, enumfacing)) {
				return false;
			}

			if (iinventory instanceof ISidedInventory) {
				ISidedInventory isidedinventory = (ISidedInventory) iinventory;
				int[] aint = isidedinventory.getSlotsForFace(enumfacing);

				for (int i : aint) {
					if (pullItemFromSlot(upper, iinventory, i, enumfacing)) {
						return true;
					}
				}
			} else {
				int j = iinventory.getSizeInventory();

				for (int k = 0; k < j; ++k) {
					if (pullItemFromSlot(upper, iinventory, k, enumfacing)) {
						return true;
					}
				}
			}
		} else {
			for (EntityItem entityitem : getCaptureItems(upper.getWorld(), upper.getXPos(), upper.getYPos(), upper.getZPos())) {
				if (putDropInInventoryAllSlots((IInventory) null, upper, entityitem)) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean pullItemFromSlot(IHopper upper, IInventory inventoryIn, int index, EnumFacing direction) {
		ItemStack itemstack = inventoryIn.getStackInSlot(index);

		if (!itemstack.isEmpty() && canExtractItemFromSlot(inventoryIn, itemstack, index, direction)) {
			ItemStack itemstack1 = itemstack.copy();
			ItemStack itemstack2 = putStackInInventoryAllSlots(inventoryIn, upper, inventoryIn.decrStackSize(index, 1), (EnumFacing) null);

			if (itemstack2.isEmpty()) {
				inventoryIn.markDirty();
				return true;
			}

			inventoryIn.setInventorySlotContents(index, itemstack1);
		}

		return false;
	}

	public static boolean putDropInInventoryAllSlots(IInventory source, IInventory destination, EntityItem entity) {
		boolean flag = false;

		if (entity == null) {
			return false;
		} else {
			ItemStack itemstack = entity.getItem().copy();
			ItemStack itemstack1 = putStackInInventoryAllSlots(source, destination, itemstack, (EnumFacing) null);

			if (itemstack1.isEmpty()) {
				flag = true;
				entity.setDead();
			} else {
				entity.setItem(itemstack1);
			}

			return flag;
		}
	}

	@Override
	protected IItemHandler createUnSidedHandler() {
		return new UpperItemHandler(this);
	}

	public static ItemStack putStackInInventoryAllSlots(IInventory source, IInventory destination, ItemStack stack, @Nullable EnumFacing direction) {
		if (destination instanceof ISidedInventory && direction != null) {
			ISidedInventory isidedinventory = (ISidedInventory) destination;
			int[] aint = isidedinventory.getSlotsForFace(direction);

			for (int k = 0; k < aint.length && !stack.isEmpty(); ++k) {
				stack = insertStack(source, destination, stack, aint[k], direction);
			}
		} else {
			int i = destination.getSizeInventory();

			for (int j = 0; j < i && !stack.isEmpty(); ++j) {
				stack = insertStack(source, destination, stack, j, direction);
			}
		}

		return stack;
	}

	private static boolean canInsertItemInSlot(IInventory inventoryIn, ItemStack stack, int index, EnumFacing side) {
		if (!inventoryIn.isItemValidForSlot(index, stack)) {
			return false;
		} else {
			return !(inventoryIn instanceof ISidedInventory) || ((ISidedInventory) inventoryIn).canInsertItem(index, stack, side);
		}
	}

	private static boolean canExtractItemFromSlot(IInventory inventoryIn, ItemStack stack, int index, EnumFacing side) {
		return !(inventoryIn instanceof ISidedInventory) || ((ISidedInventory) inventoryIn).canExtractItem(index, stack, side);
	}

	private static ItemStack insertStack(IInventory source, IInventory destination, ItemStack stack, int index, EnumFacing direction) {
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
				if (flag1 && destination instanceof TileEntityUpper) {
					TileEntityUpper tileentityupper1 = (TileEntityUpper) destination;

					if (!tileentityupper1.mayTransfer()) {
						int k = 0;

						if (source != null && source instanceof TileEntityUpper) {
							TileEntityUpper tileentityupper = (TileEntityUpper) source;

							if (tileentityupper1.tickedGameTime >= tileentityupper.tickedGameTime) {
								k = 1;
							}
						}

						tileentityupper1.setTransferCooldown(8 - k);
					}
				}

				destination.markDirty();
			}
		}

		return stack;
	}

	private IInventory getInventoryForUpperTransfer() {
		EnumFacing enumfacing = BlockUpper.getFacing(this.getBlockMetadata());
		return getInventoryAtPosition(this.getWorld(), this.getXPos() + (double) enumfacing.getFrontOffsetX(), this.getYPos() + (double) enumfacing.getFrontOffsetY(), this.getZPos() + (double) enumfacing.getFrontOffsetZ());
	}

	public static IInventory getSourceInventory(IHopper upper) {
		return getInventoryAtPosition(upper.getWorld(), upper.getXPos(), upper.getYPos() - 1.0D, upper.getZPos());
	}

	public static List<EntityItem> getCaptureItems(World worldIn, double x, double y, double z) {
		return worldIn.<EntityItem>getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(x - 0.5D, y - 1.5D, z - 0.5D, x + 0.5D, y, z + 0.5D), EntitySelectors.IS_ALIVE);
	}

	public static IInventory getInventoryAtPosition(World worldIn, double x, double y, double z) {
		IInventory iinventory = null;
		int i = MathHelper.floor(x);
		int j = MathHelper.floor(y);
		int k = MathHelper.floor(z);
		BlockPos blockpos = new BlockPos(i, j, k);
		net.minecraft.block.state.IBlockState state = worldIn.getBlockState(blockpos);
		Block block = state.getBlock();

		if (block.hasTileEntity(state)) {
			TileEntity tileentity = worldIn.getTileEntity(blockpos);

			if (tileentity instanceof IInventory) {
				iinventory = (IInventory) tileentity;

				if (iinventory instanceof TileEntityChest && block instanceof BlockChest) {
					iinventory = ((BlockChest) block).getContainer(worldIn, blockpos, true);
				}
			}
		}

		if (iinventory == null) {
			List<Entity> list = worldIn.getEntitiesInAABBexcluding((Entity) null, new AxisAlignedBB(x - 0.5D, y - 0.5D, z - 0.5D, x + 0.5D, y + 0.5D, z + 0.5D), EntitySelectors.HAS_INVENTORY);

			if (!list.isEmpty()) {
				iinventory = (IInventory) list.get(worldIn.rand.nextInt(list.size()));
			}
		}

		return iinventory;
	}

	private static boolean canCombine(ItemStack stack1, ItemStack stack2) {
		if (stack1.getItem() != stack2.getItem()) {
			return false;
		} else if (stack1.getMetadata() != stack2.getMetadata()) {
			return false;
		} else if (stack1.getCount() > stack1.getMaxStackSize()) {
			return false;
		} else {
			return ItemStack.areItemStackTagsEqual(stack1, stack2);
		}
	}

	@Override
	public double getXPos() {
		return (double) this.pos.getX() + 0.5D;
	}

	@Override
	public double getYPos() {
		return (double) this.pos.getY() + 0.5D;
	}

	@Override
	public double getZPos() {
		return (double) this.pos.getZ() + 0.5D;
	}

	public void setTransferCooldown(int ticks) {
		this.transferCooldown = ticks;
	}

	private boolean isOnTransferCooldown() {
		return this.transferCooldown > 0;
	}

	public boolean mayTransfer() {
		return this.transferCooldown > 8;
	}

	@Override
	public String getGuiID() {
		return "minecraft:hopper";
	}

	@Override
	public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
		this.fillWithLoot(playerIn);
		return new ContainerHopper(playerInventory, this, playerIn);
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.inventory;
	}

	public long getLastUpdateTime() {
		return tickedGameTime;
	} // Forge
}