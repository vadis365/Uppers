package uppers.proxy;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.registry.GameRegistry;
import uppers.tiles.TileEntityUpper;

public class CommonProxy {

	public void registerTileEntities() {
		registerTileEntity(TileEntityUpper.class, "upper");
	}

	private void registerTileEntity(Class<? extends TileEntity> cls, String baseName) {
		GameRegistry.registerTileEntity(cls, "tile.uppers." + baseName);
	}
}
