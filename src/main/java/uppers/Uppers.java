package uppers;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLModLoadingContext;

@Mod(Reference.MOD_ID)
public class Uppers {
	public Uppers() {
	      FMLModLoadingContext.get().getModEventBus().addListener(this::setup);
	        // Register the enqueueIMC method for modloading
	        FMLModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
	        // Register the processIMC method for modloading
	        FMLModLoadingContext.get().getModEventBus().addListener(this::processIMC);
	        // Register the doClientStuff method for modloading
	        FMLModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
		MinecraftForge.EVENT_BUS.register(this);
	}
	 public static ItemGroup TAB = new ItemGroup(Reference.MOD_ID) {
		 @Override
		 public ItemStack createIcon() {
			 return new ItemStack(ModBlocks.UPPER_ITEM);
		 }
	 };

	 private void setup(final FMLCommonSetupEvent event) {
		System.out.println("*************UPPERS PRE-INIT!!!!!**********");
	}

    private void doClientStuff(final FMLClientSetupEvent event) {
        // do something that can only be done on the client
		System.out.println("*************UPPERS INIT!!!!!**********");
    }

    private void enqueueIMC(final InterModEnqueueEvent event)
    {
        // some example code to dispatch IMC to another mod
    }

    private void processIMC(final InterModProcessEvent event)
    {
        // some example code to receive and process InterModComms from other mods
    }
}
