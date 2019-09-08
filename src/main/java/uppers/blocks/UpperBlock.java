package uppers.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import uppers.tiles.IUpper;
import uppers.tiles.UpperTileEntity;

public class UpperBlock extends ContainerBlock {
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
	private static final VoxelShape INPUT_SHAPE = Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D);
	private static final VoxelShape MIDDLE_SHAPE = Block.makeCuboidShape(4.0D, 6.0D, 4.0D, 12.0D, 12.0D, 12.0D);
	private static final VoxelShape INPUT_MIDDLE_SHAPE = VoxelShapes.or(MIDDLE_SHAPE, INPUT_SHAPE);
	private static final VoxelShape field_196326_A = VoxelShapes.combineAndSimplify(INPUT_MIDDLE_SHAPE, IUpper.INSIDE_BOWL_SHAPE, IBooleanFunction.ONLY_FIRST);
	private static final VoxelShape DOWN_SHAPE = VoxelShapes.or(field_196326_A, Block.makeCuboidShape(6.0D, 12.0D, 6.0D, 10.0D, 16.0D, 10.0D));
	private static final VoxelShape EAST_SHAPE = VoxelShapes.or(field_196326_A, Block.makeCuboidShape(12.0D, 8.0D, 6.0D, 16.0D, 12.0D, 10.0D));
	private static final VoxelShape NORTH_SHAPE = VoxelShapes.or(field_196326_A, Block.makeCuboidShape(6.0D, 8.0D, 0.0D, 10.0D, 12.0D, 4.0D));
	private static final VoxelShape SOUTH_SHAPE = VoxelShapes.or(field_196326_A, Block.makeCuboidShape(6.0D, 8.0D, 12.0D, 10.0D, 12.0D, 16.0D));
	private static final VoxelShape WEST_SHAPE = VoxelShapes.or(field_196326_A, Block.makeCuboidShape(0.0D, 8.0D, 6.0D, 4.0D, 12.0D, 10.0D));
	private static final VoxelShape DOWN_RAYTRACE_SHAPE = IUpper.INSIDE_BOWL_SHAPE;
	private static final VoxelShape EAST_RAYTRACE_SHAPE = VoxelShapes.or(IUpper.INSIDE_BOWL_SHAPE, Block.makeCuboidShape(12.0D, 6.0D, 6.0D, 16.0D, 8.0D, 10.0D));
	private static final VoxelShape NORTH_RAYTRACE_SHAPE = VoxelShapes.or(IUpper.INSIDE_BOWL_SHAPE, Block.makeCuboidShape(6.0D, 6.0D, 0.0D, 10.0D, 8.0D, 4.0D));
	private static final VoxelShape SOUTH_RAYTRACE_SHAPE = VoxelShapes.or(IUpper.INSIDE_BOWL_SHAPE, Block.makeCuboidShape(6.0D, 6.0D, 12.0D, 10.0D, 8.0D, 16.0D));
	private static final VoxelShape WEST_RAYTRACE_SHAPE = VoxelShapes.or(IUpper.INSIDE_BOWL_SHAPE, Block.makeCuboidShape(0.0D, 6.0D, 6.0D, 4.0D, 8.0D, 10.0D));

	public UpperBlock(Block.Properties properties) {
		super(properties);
		setDefaultState(this.stateContainer.getBaseState().with(FACING, Direction.UP).with(ENABLED, Boolean.valueOf(true)));
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		switch (state.get(FACING)) {
		case UP:
			return DOWN_SHAPE;
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
	public VoxelShape getRaytraceShape(BlockState state, IBlockReader worldIn, BlockPos pos) {
		switch (state.get(FACING)) {
		case UP:
			return DOWN_RAYTRACE_SHAPE;
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
			return IUpper.INSIDE_BOWL_SHAPE;
		}
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		Direction direction = context.getFace().getOpposite();
		return this.getDefaultState().with(FACING, direction.getAxis() == Direction.Axis.Y ? Direction.UP: direction).with(ENABLED, Boolean.valueOf(true));
	}

	@Override
	public TileEntity createNewTileEntity(IBlockReader world) {
		return new UpperTileEntity();
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (stack.hasDisplayName()) {
			TileEntity tileentity = world.getTileEntity(pos);
			if (tileentity instanceof UpperTileEntity)
				((UpperTileEntity) tileentity).setCustomName(stack.getDisplayName());
		}
	}

	@Override
	public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
		if (oldState.getBlock() != state.getBlock())
			this.updateState(worldIn, pos, state);
	}

	@Override
	public boolean onBlockActivated(BlockState state, World world, BlockPos pos,  PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
		if (world.isRemote) {
			return true;
		} else {
			TileEntity tileentity = world.getTileEntity(pos);
			if (tileentity instanceof UpperTileEntity) {
				player.openContainer((UpperTileEntity) tileentity);
				player.addStat(Stats.INSPECT_HOPPER);
			}
			return true;
		}
	}

	@Override
	public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
		this.updateState(world, pos, state);
	}

	private void updateState(World world, BlockPos pos, BlockState state) {
		boolean flag = !world.isBlockPowered(pos);
		if (flag != state.get(ENABLED))
			world.setBlockState(pos, state.with(ENABLED, Boolean.valueOf(flag)), 4);
	}

	@Override
	public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			TileEntity tileentity = world.getTileEntity(pos);
			if (tileentity instanceof UpperTileEntity) {
				InventoryHelper.dropInventoryItems(world, pos, (UpperTileEntity) tileentity);
				world.updateComparatorOutputLevel(pos, this);
			}
			super.onReplaced(state, world, pos, newState, isMoving);
		}
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public boolean hasComparatorInputOverride(BlockState state) {
		return true;
	}

	@Override
	public int getComparatorInputOverride(BlockState blockState, World worldIn, BlockPos pos) {
		return Container.calcRedstone(worldIn.getTileEntity(pos));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.CUTOUT_MIPPED;
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		return state.with(FACING, rot.rotate(state.get(FACING)));
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.toRotation(state.get(FACING)));
	}

	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
		builder.add(FACING, ENABLED);
	}

	@Override
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		TileEntity tileentity = world.getTileEntity(pos);
		if (tileentity instanceof UpperTileEntity)
			((UpperTileEntity) tileentity).onEntityCollision(entity);
	}

	@Override
	public boolean allowsMovement(BlockState state, IBlockReader world, BlockPos pos, PathType type) {
		return false;
	}
}