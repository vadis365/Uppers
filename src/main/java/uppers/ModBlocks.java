package uppers;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import uppers.blocks.BlockUpper;
import uppers.tiles.TileEntityUpper;

public class ModBlocks {

	public static Block UPPER;
	public static ItemBlock UPPER_ITEM;
	public static final TileEntityType<TileEntityUpper> UPPER_TILE = TileEntityType.Builder.create(TileEntityUpper::new).build(null);

	public static void init() {
		UPPER = new BlockUpper(Block.Builder.create(Material.IRON, MapColor.STONE).hardnessAndResistance(3.0F, 4.8F));//.sound(SoundType.METAL));
		UPPER_ITEM = new ItemBlock(UPPER, new Item.Builder().group(Uppers.TAB)) {
			@Override
			@OnlyIn(Dist.CLIENT)
			   public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
				tooltip.add(new TextComponentTranslation("tooltip.upper_1"));
				tooltip.add(new TextComponentTranslation("tooltip.upper_2"));
			}
		};
		UPPER.setRegistryName(Reference.MOD_ID, "upper");
		UPPER_ITEM.setRegistryName(UPPER.getRegistryName());
	}

	@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
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
		
        @SubscribeEvent
        public static void registerTileEntities(RegistryEvent.Register<TileEntityType<?> > event) {
            IForgeRegistry<TileEntityType<?>> registry = event.getRegistry();
            registry.register(UPPER_TILE.setRegistryName(Reference.MOD_ID, Reference.UPPER));
        }

        @OnlyIn(Dist.CLIENT)
		@SubscribeEvent
		public static void registerModels(ModelRegistryEvent event) {
		//	ModelLoader.setCustomModelResourceLocation(UPPER_ITEM, 0, new ModelResourceLocation(UPPER_ITEM.getRegistryName().toString(), "inventory"));
		//	ModelLoader.setCustomStateMapper((UPPER), (new StateMap.Builder()).ignore(new IProperty[] { BlockUpper.ENABLED }).build());
		}
	}

}
