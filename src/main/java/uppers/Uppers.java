package uppers;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import uppers.proxy.CommonProxy;

@Mod(modid = "uppers", name = "uppers", version = "0.0.6")

public class Uppers {

	@Instance("uppers")
	public static Uppers INSTANCE;

	@SidedProxy(clientSide = "uppers.proxy.ClientProxy", serverSide = "uppers.proxy.CommonProxy")
	public static CommonProxy PROXY;

	public static CreativeTabs TAB = new CreativeTabs("uppers") {

		@Override
		public ItemStack getTabIconItem() {
			return new ItemStack(ModBlocks.UPPER_ITEM);
		}
	};

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		PROXY.registerTileEntities();
	}
}
