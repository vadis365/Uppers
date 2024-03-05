package uppers.blocks;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import uppers.Uppers;
import uppers.tiles.IUpper;
import uppers.tiles.UpperBlockEntity;

public class UpperBlock extends BaseEntityBlock {
	public static final MapCodec<UpperBlock> CODEC = simpleCodec(UpperBlock::new);
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
	private static final VoxelShape INPUT_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D);
	private static final VoxelShape MIDDLE_SHAPE = Block.box(4.0D, 6.0D, 4.0D, 12.0D, 12.0D, 12.0D);
	private static final VoxelShape INPUT_MIDDLE_SHAPE = Shapes.or(MIDDLE_SHAPE, INPUT_SHAPE);
	private static final VoxelShape field_196326_A = Shapes.join(INPUT_MIDDLE_SHAPE, IUpper.INSIDE_BOWL_SHAPE, BooleanOp.ONLY_FIRST);
	private static final VoxelShape DOWN_SHAPE = Shapes.or(field_196326_A, Block.box(6.0D, 12.0D, 6.0D, 10.0D, 16.0D, 10.0D));
	private static final VoxelShape EAST_SHAPE = Shapes.or(field_196326_A, Block.box(12.0D, 8.0D, 6.0D, 16.0D, 12.0D, 10.0D));
	private static final VoxelShape NORTH_SHAPE = Shapes.or(field_196326_A, Block.box(6.0D, 8.0D, 0.0D, 10.0D, 12.0D, 4.0D));
	private static final VoxelShape SOUTH_SHAPE = Shapes.or(field_196326_A, Block.box(6.0D, 8.0D, 12.0D, 10.0D, 12.0D, 16.0D));
	private static final VoxelShape WEST_SHAPE = Shapes.or(field_196326_A, Block.box(0.0D, 8.0D, 6.0D, 4.0D, 12.0D, 10.0D));
	private static final VoxelShape DOWN_RAYTRACE_SHAPE = IUpper.INSIDE_BOWL_SHAPE;
	private static final VoxelShape EAST_RAYTRACE_SHAPE = Shapes.or(IUpper.INSIDE_BOWL_SHAPE, Block.box(12.0D, 6.0D, 6.0D, 16.0D, 8.0D, 10.0D));
	private static final VoxelShape NORTH_RAYTRACE_SHAPE = Shapes.or(IUpper.INSIDE_BOWL_SHAPE, Block.box(6.0D, 6.0D, 0.0D, 10.0D, 8.0D, 4.0D));
	private static final VoxelShape SOUTH_RAYTRACE_SHAPE = Shapes.or(IUpper.INSIDE_BOWL_SHAPE, Block.box(6.0D, 6.0D, 12.0D, 10.0D, 8.0D, 16.0D));
	private static final VoxelShape WEST_RAYTRACE_SHAPE = Shapes.or(IUpper.INSIDE_BOWL_SHAPE, Block.box(0.0D, 6.0D, 6.0D, 4.0D, 8.0D, 10.0D));

	public UpperBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP).setValue(ENABLED, Boolean.valueOf(true)));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		switch (state.getValue(FACING)) {
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
	public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
		switch (state.getValue(FACING)) {
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
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction direction = context.getClickedFace().getOpposite();
		return this.defaultBlockState().setValue(FACING, direction.getAxis() == Direction.Axis.Y ? Direction.UP: direction).setValue(ENABLED, Boolean.valueOf(true));
	}

	@Override
	   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new UpperBlockEntity(pos, state);
	}

	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, Uppers.UPPER_TILE.get(), UpperBlockEntity::pushItemsTick);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (stack.hasCustomHoverName()) {
			BlockEntity tileentity = level.getBlockEntity(pos);
			if (tileentity instanceof UpperBlockEntity)
				((UpperBlockEntity) tileentity).setCustomName(stack.getHoverName());
		}
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		if (!oldState.is(state.getBlock()))
			this.checkPoweredState(level, pos, state);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		} else {
			BlockEntity tileentity = level.getBlockEntity(pos);
			if (tileentity instanceof UpperBlockEntity) {
				player.openMenu((UpperBlockEntity) tileentity);
				player.awardStat(Stats.INSPECT_HOPPER);
			}
			return InteractionResult.CONSUME;
		}
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
		this.checkPoweredState(level, pos, state);
	}

	private void checkPoweredState(Level level, BlockPos pos, BlockState state) {
		boolean flag = !level.hasNeighborSignal(pos);
		if (flag != state.getValue(ENABLED))
			level.setBlock(pos, state.setValue(ENABLED, Boolean.valueOf(flag)), 4);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			BlockEntity tileentity = level.getBlockEntity(pos);
			if (tileentity instanceof UpperBlockEntity) {
				Containers.dropContents(level, pos, (UpperBlockEntity) tileentity);
				level.updateNeighbourForOutputSignal(pos, this);
			}
			super.onRemove(state, level, pos, newState, isMoving);
		}
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
		return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ENABLED);
	}

	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		BlockEntity tileentity = level.getBlockEntity(pos);
		if (tileentity instanceof UpperBlockEntity)
			UpperBlockEntity.entityInside(level, pos, state, entity, (UpperBlockEntity)tileentity);
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	protected MapCodec<UpperBlock> codec() {
		return CODEC;
	}
}