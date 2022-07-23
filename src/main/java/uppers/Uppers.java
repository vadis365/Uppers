package uppers;

import java.util.List;

import javax.annotation.Nullable;

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
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import uppers.blocks.UpperBlock;
import uppers.tiles.UpperBlockEntity;

@Mod(Reference.MOD_ID)
public class Uppers {
	private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Reference.MOD_ID);
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Reference.MOD_ID);
	private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Reference.MOD_ID);

	public static final RegistryObject<Block> UPPER = BLOCKS.register(Reference.UPPER, () -> new UpperBlock(BlockBehaviour.Properties.of(Material.METAL, MaterialColor.STONE).requiresCorrectToolForDrops().strength(3.0F, 4.8F).sound(SoundType.METAL).noOcclusion()));
	
	public static final RegistryObject<BlockItem> UPPER_ITEM = ITEMS.register(Reference.UPPER, () -> 
	new BlockItem(UPPER.get(), new Item.Properties().tab(Uppers.TAB)) {
		@Override
		@OnlyIn(Dist.CLIENT)
		   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
			tooltip.add(Component.translatable("tooltip.upper_1"));
			tooltip.add(Component.translatable("tooltip.upper_2"));
			}
		}
	);

	public static final RegistryObject<BlockEntityType<UpperBlockEntity>> UPPER_TILE = TILES.register(Reference.UPPER, () -> BlockEntityType.Builder.of(UpperBlockEntity::new, UPPER.get()).build(null));
	
	public Uppers() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		BLOCKS.register(modEventBus);
		ITEMS.register(modEventBus);
		TILES.register(modEventBus);
		MinecraftForge.EVENT_BUS.register(this);
	}

	public static CreativeModeTab TAB = new CreativeModeTab(Reference.MOD_ID) {
		@Override
		public ItemStack makeIcon() {
			return new ItemStack(UPPER_ITEM.get());
		}
	};

}
