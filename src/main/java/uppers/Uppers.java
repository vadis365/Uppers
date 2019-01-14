package uppers;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.javafmlmod.FMLModLoadingContext;

@Mod(Reference.MOD_ID)
public class Uppers {
	public Uppers() {
        FMLModLoadingContext.get().getModEventBus().addListener(this::preInit);
        FMLModLoadingContext.get().getModEventBus().addListener(this::init);
		MinecraftForge.EVENT_BUS.register(this);
	}
	 public static ItemGroup TAB = new ItemGroup(Reference.MOD_ID) {
		 @Override
		 public ItemStack createIcon() {
			 return new ItemStack(ModBlocks.UPPER_ITEM);
		 }
	 };

	public void preInit(FMLPreInitializationEvent event) {
		System.out.println("*************UPPERS PRE-INIT!!!!!**********");
	}

	public void init(FMLInitializationEvent event) {
		System.out.println("*************UPPERS INIT!!!!!**********");
	}
}
