package uppers;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

public class ModRecipes {

	public static IRecipe UPPER, UPPER_ALT, HOPPER_ALT;

	public static void init() {
		UPPER = new ShapedOreRecipe(getResource("recipe_upper"), new ItemStack(ModBlocks.UPPER_ITEM), " I ", "ICI", "I I", 'I', "ingotIron", 'C', new ItemStack(Blocks.CHEST));
		UPPER.setRegistryName(getResource("upper"));

		UPPER_ALT = new ShapelessOreRecipe(getResource("recipe_upper_alt"), new ItemStack(ModBlocks.UPPER_ITEM), new ItemStack(Item.getItemFromBlock(Blocks.HOPPER)));
		UPPER_ALT.setRegistryName(getResource("upper_alt"));
		
		HOPPER_ALT = new ShapelessOreRecipe(getResource("recipe_hopper_alt"), new ItemStack(Item.getItemFromBlock(Blocks.HOPPER)), new ItemStack(ModBlocks.UPPER_ITEM));
		HOPPER_ALT.setRegistryName(getResource("hopper_alt"));
	}

	private static ResourceLocation getResource(String inName) {
		return new ResourceLocation("uppers", inName);
	}

	@Mod.EventBusSubscriber(modid = "uppers")
	public static class RegistrationHandlerRecipes {
		@SubscribeEvent
		public static void registerRecipes(final RegistryEvent.Register<IRecipe> event) {
			init();
			final IForgeRegistry<IRecipe> registry = event.getRegistry();
			registry.registerAll(UPPER, UPPER_ALT, HOPPER_ALT);
		}
	}

}