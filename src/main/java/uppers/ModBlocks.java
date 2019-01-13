package uppers;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;
import uppers.blocks.BlockUpper;

public class ModBlocks {

	public static Block UPPER;
	public static ItemBlock UPPER_ITEM;

	public static void init() {
		UPPER = new BlockUpper();
		UPPER_ITEM = new ItemBlock(UPPER) {
			@Override
			@SideOnly(Side.CLIENT)
			public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flag) {
				list.add(TextFormatting.YELLOW + new TextComponentTranslation("tooltip.upper_1").getFormattedText());
				list.add(TextFormatting.YELLOW + new TextComponentTranslation("tooltip.upper_2").getFormattedText());
			}
		};
		UPPER.setRegistryName("uppers", "upper").setUnlocalizedName("uppers.upper");
		UPPER_ITEM.setRegistryName(UPPER.getRegistryName()).setUnlocalizedName("uppers.upper");
	}

	@Mod.EventBusSubscriber(modid = "uppers")
	public static class RegistrationHandlerBlocks {
		@SubscribeEvent
		public static void registerBlocks(final RegistryEvent.Register<Block> event) {
			init();
			final Block[] blocks = { UPPER };
			final IForgeRegistry<Block> registry = event.getRegistry();
			for (final Block block : blocks) {
				registry.register(block);
			}
		}

		@SubscribeEvent
		public static void registerItemBlocks(final RegistryEvent.Register<Item> event) {
			final ItemBlock[] items = { UPPER_ITEM };
			final IForgeRegistry<Item> registry = event.getRegistry();
			for (final ItemBlock item : items) {
				registry.register(item);
			}
		}

		@SideOnly(Side.CLIENT)
		@SubscribeEvent
		public static void registerModels(ModelRegistryEvent event) {
			ModelLoader.setCustomModelResourceLocation(UPPER_ITEM, 0, new ModelResourceLocation(UPPER_ITEM.getRegistryName().toString(), "inventory"));
			ModelLoader.setCustomStateMapper((UPPER), (new StateMap.Builder()).ignore(new IProperty[] { BlockUpper.ENABLED }).build());
		}
	}

}
