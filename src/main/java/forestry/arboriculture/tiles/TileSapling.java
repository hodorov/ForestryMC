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
package forestry.arboriculture.tiles;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import forestry.api.arboriculture.IArboristTracker;
import forestry.api.arboriculture.ITree;
import forestry.api.arboriculture.ITreeModifier;
import forestry.api.arboriculture.ITreekeepingMode;
import forestry.api.arboriculture.TreeManager;
import forestry.arboriculture.worldgen.WorldGenArboriculture;
import forestry.core.worldgen.WorldGenBase;

public class TileSapling extends TileTreeContainer {

	private int timesTicked = 0;

	/* SAVING & LOADING */
	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);

		timesTicked = nbttagcompound.getInteger("TT");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);

		nbttagcompound.setInteger("TT", timesTicked);
	}

	@Override
	public void onBlockTick() {

		timesTicked++;
		tryGrow(false);
	}

	private static int getRequiredMaturity(World world, ITree tree) {
		ITreekeepingMode treekeepingMode = TreeManager.treeRoot.getMode(world);
		ITreeModifier treeModifier = treekeepingMode.getTreeModifier();
		float maturationModifier = treeModifier.getMaturationModifier(tree.getGenome(), 1f);
		return Math.round(tree.getRequiredMaturity() * maturationModifier);
	}

	public boolean canAcceptBoneMeal() {
		ITree tree = getTree();

		if (tree == null) {
			return false;
		}

		int maturity = getRequiredMaturity(worldObj, tree);
		if (timesTicked < maturity) {
			return true;
		}

		WorldGenerator generator = tree.getTreeGenerator(worldObj, getPos(), true);
		if (generator instanceof WorldGenArboriculture) {
			WorldGenArboriculture arboricultureGenerator = (WorldGenArboriculture) generator;
			arboricultureGenerator.preGenerate(worldObj, getPos());
			return arboricultureGenerator.canGrow(worldObj, getPos());
		} else {
			return true;
		}
	}

	public void tryGrow(boolean bonemealed) {

		ITree tree = getTree();

		if (tree == null) {
			return;
		}

		int maturity = getRequiredMaturity(worldObj, tree);
		if (timesTicked < maturity) {
			if (bonemealed) {
				timesTicked = maturity;
			}
			return;
		}

		WorldGenerator generator = tree.getTreeGenerator(worldObj, getPos(), bonemealed);
		final boolean generated;
		if (generator instanceof WorldGenBase) {
			generated = ((WorldGenBase) generator).generate(worldObj, getPos(), bonemealed);
		} else {
			generated = generator.generate(worldObj, worldObj.rand, getPos());
		}

		if (generated) {
			IArboristTracker breedingTracker = TreeManager.treeRoot.getBreedingTracker(worldObj, getOwner());
			breedingTracker.registerBirth(tree);
		}
	}

}
