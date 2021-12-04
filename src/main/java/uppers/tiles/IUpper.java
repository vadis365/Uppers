package uppers.tiles;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface IUpper extends Hopper {
	VoxelShape INSIDE_BOWL_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 5.0D, 14.0D);
	VoxelShape BLOCK_BELOW_SHAPE = Block.box(0.0D, -16.0D, 0.0D, 16.0D, 0.0D, 16.0D);
	VoxelShape COLLECTION_AREA_SHAPE = Shapes.or(INSIDE_BOWL_SHAPE, BLOCK_BELOW_SHAPE);

	default VoxelShape getSuckShape() {
		return COLLECTION_AREA_SHAPE;
	}

	double getLevelX();

	double getLevelY();

	double getLevelZ();
}