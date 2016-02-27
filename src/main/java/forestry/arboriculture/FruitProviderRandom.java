/*******************************************************************************
 * Copyright (c) 2011-2014 SirSengir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Various Contributors including, but not limited to:
 * SirSengir (original work), CovertJaguar, Player, Binnie, MysteriousAges
 ******************************************************************************/
package forestry.arboriculture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import forestry.api.arboriculture.ITreeGenome;
import forestry.api.arboriculture.ITreeModifier;
import forestry.api.arboriculture.ITreekeepingMode;
import forestry.api.arboriculture.TreeManager;
import forestry.api.genetics.IFruitFamily;

/**
 * Simple fruit provider which drops from any leaf block according to yield and either marks all leave blocks as fruit leaves or none.
 */
public class FruitProviderRandom extends FruitProviderNone {

	private final Map<ItemStack, Float> products = new HashMap<>();
	private int colour = 0xffffff;

	public FruitProviderRandom(String key, IFruitFamily family, ItemStack product, float modifier) {
		super(key, family);
		products.put(product, modifier);
	}

	public FruitProviderRandom setColour(int colour) {
		this.colour = colour;
		return this;
	}

	@Override
	public int getColour(ITreeGenome genome, IBlockAccess world, BlockPos pos, int ripeningTime) {
		return colour;
	}

	@Override
	public ItemStack[] getFruits(ITreeGenome genome, World world, BlockPos pos, int ripeningTime) {
		ArrayList<ItemStack> product = new ArrayList<>();

		ITreekeepingMode mode = TreeManager.treeRoot.getMode(world);
		ITreeModifier modifier = mode.getTreeModifier();
		float modeYieldMod = modifier.getYieldModifier(genome, 1f);

		for (Map.Entry<ItemStack, Float> entry : products.entrySet()) {
			if (world.rand.nextFloat() <= genome.getYield() * modeYieldMod * entry.getValue()) {
				product.add(entry.getKey().copy());
			}
		}

		return product.toArray(new ItemStack[product.size()]);
	}

	@Override
	public ItemStack[] getProducts() {
		Set<ItemStack> products = this.products.keySet();
		return products.toArray(new ItemStack[products.size()]);
	}

	@Override
	public ItemStack[] getSpecialty() {
		return new ItemStack[0];
	}

	@Override
	public boolean markAsFruitLeaf(ITreeGenome genome, World world, BlockPos pos) {
		return true;
	}

}
