package uppers;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import uppers.blocks.UpperBlock;
import uppers.tiles.UpperBlockEntity;
import uppers.tiles.UpperItemHandler;

@Mod(Reference.MOD_ID)
public class Uppers {
	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Reference.MOD_ID);
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Reference.MOD_ID);
	private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Reference.MOD_ID);
	private static final DeferredRegister<CreativeModeTab> TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Reference.MOD_ID);
	public static final DeferredBlock<Block> UPPER = BLOCKS.register(Reference.UPPER, () -> new UpperBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3.0F, 4.8F).sound(SoundType.METAL).noOcclusion()));

	public static final DeferredItem<BlockItem> UPPER_ITEM = ITEMS.register(Reference.UPPER, () -> new BlockItem(UPPER.get(), new Item.Properties()) {
		@Override
		@OnlyIn(Dist.CLIENT)
		   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
			tooltip.add(Component.translatable("tooltip.upper_1"));
			tooltip.add(Component.translatable("tooltip.upper_2"));
			}
		}
	);

	public static final Supplier<BlockEntityType<UpperBlockEntity>> UPPER_TILE = TILES.register(Reference.UPPER, () -> BlockEntityType.Builder.of(UpperBlockEntity::new, UPPER.get()).build(null));

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> UPPERS_TAB = TAB.register(Reference.MOD_ID, () -> CreativeModeTab.builder().
			title(Component.translatable("itemGroup.uppers")).
			icon(UPPER_ITEM.get()::getDefaultInstance).displayItems((params, output) -> {
				output.accept(UPPER_ITEM.get());
			})
			.build());

	public Uppers(IEventBus modBus) {
		IEventBus neoBus = NeoForge.EVENT_BUS;
		BLOCKS.register(modBus);
		ITEMS.register(modBus);
		TILES.register(modBus);
		TAB.register(modBus);
		modBus.addListener(this::registerCaps);
	}

	private void registerCaps(final RegisterCapabilitiesEvent event) {
	       event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, UPPER_TILE.get(), (upper, side) -> {
	            return new UpperItemHandler(upper);
	        });
	}
}
