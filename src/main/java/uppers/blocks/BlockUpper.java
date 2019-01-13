package uppers.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.IHopper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ShapeUtils;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import uppers.tiles.TileEntityUpper;

public class BlockUpper extends BlockContainer {
	   public static final DirectionProperty FACING = BlockStateProperties.FACING;
	   public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
	   private static final VoxelShape INPUT_SHAPE = Block.makeCuboidShape(0.0D, 10.0D, 0.0D, 16.0D, 16.0D, 16.0D);
	   private static final VoxelShape MIDDLE_SHAPE = Block.makeCuboidShape(4.0D, 4.0D, 4.0D, 12.0D, 10.0D, 12.0D);
	   private static final VoxelShape INPUT_MIDDLE_SHAPE = ShapeUtils.or(MIDDLE_SHAPE, INPUT_SHAPE);
	   private static final VoxelShape field_196326_A = ShapeUtils.combineAndSimplify(INPUT_MIDDLE_SHAPE, IHopper.INSIDE_BOWL_SHAPE, IBooleanFunction.ONLY_FIRST);
	   private static final VoxelShape DOWN_SHAPE = ShapeUtils.or(field_196326_A, Block.makeCuboidShape(6.0D, 0.0D, 6.0D, 10.0D, 4.0D, 10.0D));
	   private static final VoxelShape EAST_SHAPE = ShapeUtils.or(field_196326_A, Block.makeCuboidShape(12.0D, 4.0D, 6.0D, 16.0D, 8.0D, 10.0D));
	   private static final VoxelShape NORTH_SHAPE = ShapeUtils.or(field_196326_A, Block.makeCuboidShape(6.0D, 4.0D, 0.0D, 10.0D, 8.0D, 4.0D));
	   private static final VoxelShape SOUTH_SHAPE = ShapeUtils.or(field_196326_A, Block.makeCuboidShape(6.0D, 4.0D, 12.0D, 10.0D, 8.0D, 16.0D));
	   private static final VoxelShape WEST_SHAPE = ShapeUtils.or(field_196326_A, Block.makeCuboidShape(0.0D, 4.0D, 6.0D, 4.0D, 8.0D, 10.0D));
	   private static final VoxelShape DOWN_RAYTRACE_SHAPE = IHopper.INSIDE_BOWL_SHAPE;
	   private static final VoxelShape EAST_RAYTRACE_SHAPE = ShapeUtils.or(IHopper.INSIDE_BOWL_SHAPE, Block.makeCuboidShape(12.0D, 8.0D, 6.0D, 16.0D, 10.0D, 10.0D));
	   private static final VoxelShape NORTH_RAYTRACE_SHAPE = ShapeUtils.or(IHopper.INSIDE_BOWL_SHAPE, Block.makeCuboidShape(6.0D, 8.0D, 0.0D, 10.0D, 10.0D, 4.0D));
	   private static final VoxelShape SOUTH_RAYTRACE_SHAPE = ShapeUtils.or(IHopper.INSIDE_BOWL_SHAPE, Block.makeCuboidShape(6.0D, 8.0D, 12.0D, 10.0D, 10.0D, 16.0D));
	   private static final VoxelShape WEST_RAYTRACE_SHAPE = ShapeUtils.or(IHopper.INSIDE_BOWL_SHAPE, Block.makeCuboidShape(0.0D, 8.0D, 6.0D, 4.0D, 10.0D, 10.0D));

	public BlockUpper(Builder builder) {
		super(builder);
		setDefaultState((IBlockState)((IBlockState)(this.stateContainer.getBaseState()).with(FACING, EnumFacing.DOWN)).with(ENABLED, Boolean.valueOf(true)));
	}

	@Override
	public VoxelShape getShape(IBlockState state, IBlockReader worldIn, BlockPos pos) {
		switch (state.get(FACING)) {
		case DOWN:
			return DOWN_SHAPE;
		case NORTH:
			return NORTH_SHAPE;
		case SOUTH:
			return SOUTH_SHAPE;
		case WEST:
			return WEST_SHAPE;
		case EAST:
			return EAST_SHAPE;
		default:
			return field_196326_A;
		}
	}

	@Override
	public VoxelShape getRaytraceShape(IBlockState state, IBlockReader worldIn, BlockPos pos) {
		switch (state.get(FACING)) {
		case DOWN:
			return DOWN_RAYTRACE_SHAPE;
		case NORTH:
			return NORTH_RAYTRACE_SHAPE;
		case SOUTH:
			return SOUTH_RAYTRACE_SHAPE;
		case WEST:
			return WEST_RAYTRACE_SHAPE;
		case EAST:
			return EAST_RAYTRACE_SHAPE;
		default:
			return IHopper.INSIDE_BOWL_SHAPE;
		}
	}

	@Override
	public IBlockState getStateForPlacement(BlockItemUseContext context) {
		EnumFacing enumfacing = context.getFace().getOpposite();
		return (IBlockState) ((IBlockState) this.getDefaultState().with(FACING, enumfacing.getAxis() == EnumFacing.Axis.Y ? EnumFacing.DOWN : EnumFacing.UP)).with(ENABLED, Boolean.valueOf(true));
	}

	@Override
	public TileEntity createNewTileEntity(IBlockReader world) {
		return new TileEntityUpper();
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, placer, stack);
		if (stack.hasDisplayName()) {
			TileEntity tileentity = world.getTileEntity(pos);
			if (tileentity instanceof TileEntityUpper) {
				((TileEntityUpper) tileentity).setCustomName(stack.getDisplayName());
			}
		}
	}

	@Override
	public boolean isTopSolid(IBlockState state) {
		return true;
	}

	@Override
	public void onBlockAdded(IBlockState state, World worldIn, BlockPos pos, IBlockState oldState) {
		if (oldState.getBlock() != state.getBlock()) {
			this.updateState(worldIn, pos, state);
		}
	}

	@Override
	public boolean onBlockActivated(IBlockState state, World world, BlockPos pos,  EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (world.isRemote) {
			return true;
		} else {
			TileEntity tileentity = world.getTileEntity(pos);
			if (tileentity instanceof TileEntityUpper) {
				player.displayGUIChest((TileEntityUpper) tileentity);
				player.addStat(StatList.INSPECT_HOPPER);
			}
			return true;
		}
	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
		this.updateState(world, pos, state);
	}
	
	private void updateState(World world, BlockPos pos, IBlockState state) {
		boolean flag = !world.isBlockPowered(pos);
		if (flag != state.get(ENABLED)) {
			world.setBlockState(pos, (IBlockState) state.with(ENABLED, Boolean.valueOf(flag)), 4);
		}

	}

	@Override
	public void onReplaced(IBlockState state, World world, BlockPos pos, IBlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			TileEntity tileentity = world.getTileEntity(pos);
			if (tileentity instanceof TileEntityUpper) {
				InventoryHelper.dropInventoryItems(world, pos, (TileEntityUpper) tileentity);
				world.updateComparatorOutputLevel(pos, this);
			}
			super.onReplaced(state, world, pos, newState, isMoving);
		}
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean hasComparatorInputOverride(IBlockState state) {
		return true;
	}

	@Override
	public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
		return Container.calcRedstone(worldIn.getTileEntity(pos));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.CUTOUT_MIPPED;
	}

	@Override
	public IBlockState rotate(IBlockState state, Rotation rot) {
		return (IBlockState) state.with(FACING, rot.rotate((EnumFacing) state.get(FACING)));
	}

	@Override
	public IBlockState mirror(IBlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.toRotation((EnumFacing) state.get(FACING)));
	}

	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, IBlockState> builder) {
		builder.add(FACING, ENABLED);
	}

	@Override
	public BlockFaceShape getBlockFaceShape(IBlockReader world, IBlockState state, BlockPos pos, EnumFacing face) {
		return face == EnumFacing.DOWN ? BlockFaceShape.BOWL : BlockFaceShape.UNDEFINED;
	}
	
	@Override
	public void onEntityCollision(IBlockState state, World world, BlockPos pos, Entity entity) {
		TileEntity tileentity = world.getTileEntity(pos);
		if (tileentity instanceof TileEntityUpper) {
			((TileEntityUpper) tileentity).onEntityCollision(entity);
		}

	}

	@Override
	public boolean allowsMovement(IBlockState state, IBlockReader world, BlockPos pos, PathType type) {
		return false;
	}
}