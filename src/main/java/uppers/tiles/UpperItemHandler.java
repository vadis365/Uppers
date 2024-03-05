package uppers.tiles;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public class UpperItemHandler extends InvWrapper {
	private final UpperBlockEntity upper;

	public UpperItemHandler(UpperBlockEntity upper) {
		super(upper);
		this.upper = upper;
	}

	@Override
	@Nonnull
	public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
		if (simulate) {
			return super.insertItem(slot, stack, simulate);
		} else {
			boolean wasEmpty = getInv().isEmpty();
			int originalStackSize = stack.getCount();
			stack = super.insertItem(slot, stack, simulate);
			if (wasEmpty && originalStackSize > stack.getCount())
				if (!upper.isOnCustomCooldown())
					upper.setCooldown(8);
			return stack;
		}
	}
}