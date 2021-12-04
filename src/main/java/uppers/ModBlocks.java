package uppers;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import uppers.blocks.UpperBlock;
import uppers.tiles.UpperBlockEntity;

public class ModBlocks {
	public static Block UPPER;
	public static BlockItem UPPER_ITEM;
	public static BlockEntityType<UpperBlockEntity> UPPER_TILE;

	public static void init() {
		UPPER = new UpperBlock(BlockBehaviour.Properties.of(Material.METAL, MaterialColor.STONE).requiresCorrectToolForDrops().strength(3.0F, 4.8F).sound(SoundType.METAL).noOcclusion());
		UPPER_ITEM = new BlockItem(UPPER, new Item.Properties().tab(Uppers.TAB)) {
			@Override
			@OnlyIn(Dist.CLIENT)
			   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
				tooltip.add(new TranslatableComponent("tooltip.upper_1"));
				tooltip.add(new TranslatableComponent("tooltip.upper_2"));
			}
		};
		UPPER_TILE = BlockEntityType.Builder.of(UpperBlockEntity::new, UPPER).build(null);
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
        public static void registerTileEntities(RegistryEvent.Register<BlockEntityType<?> > event) {
            IForgeRegistry<BlockEntityType<?>> registry = event.getRegistry();
            registry.register(UPPER_TILE);
        }
	}
}
