package uppers;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import uppers.blocks.UpperBlock;
import uppers.tiles.UpperTileEntity;

public class ModBlocks {
	public static Block UPPER;
	public static BlockItem UPPER_ITEM;
	public static TileEntityType<UpperTileEntity> UPPER_TILE;

	public static void init() {
		UPPER = new UpperBlock(Block.Properties.create(Material.IRON, MaterialColor.STONE).hardnessAndResistance(3.0F, 4.8F).sound(SoundType.METAL));
		UPPER_ITEM = new BlockItem(UPPER, new Item.Properties().group(ItemGroup.REDSTONE)) {
			@Override
			@OnlyIn(Dist.CLIENT)
			   public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
				tooltip.add(new TranslationTextComponent("tooltip.upper_1"));
				tooltip.add(new TranslationTextComponent("tooltip.upper_2"));
			}
		};
		UPPER_TILE = TileEntityType.Builder.create(UpperTileEntity::new, UPPER).build(null);
		UPPER.setRegistryName(Reference.MOD_ID, Reference.UPPER);
		UPPER_ITEM.setRegistryName(UPPER.getRegistryName());
		UPPER_TILE.setRegistryName(Reference.MOD_ID, Reference.UPPER);
	}

	@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class RegistrationHandlerBlocks {
		@SubscribeEvent
		public static void registerBlocks(final RegistryEvent.Register<Block> event) {
			init();
			final IForgeRegistry<Block> registry = event.getRegistry();
			registry.register(UPPER);
		}

		@SubscribeEvent
		public static void registerItemBlocks(final RegistryEvent.Register<Item> event) {
			final IForgeRegistry<Item> registry = event.getRegistry();
			registry.register(UPPER_ITEM);
		}

        @SubscribeEvent
        public static void registerTileEntities(RegistryEvent.Register<TileEntityType<?> > event) {
            IForgeRegistry<TileEntityType<?>> registry = event.getRegistry();
            registry.register(UPPER_TILE);
        }
	}
}
