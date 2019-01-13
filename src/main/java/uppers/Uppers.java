package uppers;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;

@Mod(Reference.MOD_ID)

public class Uppers {
	 public static ItemGroup TAB = new ItemGroup(Reference.MOD_ID) {
		 @Override
		 public ItemStack createIcon() {
			 return new ItemStack(ModBlocks.UPPER_ITEM);
		 }
	 };
}
