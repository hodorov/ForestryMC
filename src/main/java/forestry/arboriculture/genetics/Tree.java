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
package forestry.arboriculture.genetics;

import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import com.mojang.authlib.GameProfile;

import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;

import forestry.api.arboriculture.EnumGrowthConditions;
import forestry.api.arboriculture.IAlleleLeafEffect;
import forestry.api.arboriculture.IAlleleTreeSpecies;
import forestry.api.arboriculture.IFruitProvider;
import forestry.api.arboriculture.IGrowthProvider;
import forestry.api.arboriculture.ITree;
import forestry.api.arboriculture.ITreeGenome;
import forestry.api.arboriculture.TreeChromosome;
import forestry.api.arboriculture.TreeManager;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleBoolean;
import forestry.api.genetics.IChromosome;
import forestry.api.genetics.IEffectData;
import forestry.api.genetics.IFruitFamily;
import forestry.arboriculture.genetics.alleles.AlleleFruit;
import forestry.core.genetics.Individual;
import forestry.core.utils.BlockUtil;
import forestry.core.utils.StringUtil;

public class Tree extends Individual<TreeChromosome> implements ITree, IPlantable {

	@Nonnull
	private final ITreeGenome genome;
	@Nullable
	private ITreeGenome mate;

	private final EnumSet<EnumPlantType> plantTypes;
	private EnumPlantType plantType;

	public Tree(@Nonnull ITreeGenome genome) {
		this.genome = genome;
		plantTypes = genome.getPlantTypes();
		plantTypes.add(genome.getPrimary().getPlantType());
	}

	public Tree(@Nonnull NBTTagCompound nbttagcompound) {
		super(nbttagcompound);

		if (nbttagcompound.hasKey("Genome")) {
			this.genome = new TreeGenome(nbttagcompound.getCompoundTag("Genome"));
		} else {
			this.genome = TreeDefinition.Oak.getGenome();
		}

		this.plantTypes = genome.getPlantTypes();
		this.plantTypes.add(genome.getPrimary().getPlantType());

		if (nbttagcompound.hasKey("Mate")) {
			mate = new TreeGenome(nbttagcompound.getCompoundTag("Mate"));
		}
	}

	@Override
	public void writeToNBT(@Nonnull NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);

		NBTTagCompound nbtGenome = new NBTTagCompound();
		genome.writeToNBT(nbtGenome);
		nbttagcompound.setTag("Genome", nbtGenome);

		if (mate != null) {
			NBTTagCompound nbtMate = new NBTTagCompound();
			mate.writeToNBT(nbtMate);
			nbttagcompound.setTag("Mate", nbtMate);
		}
	}

	/* INTERACTION */
	@Override
	public void mate(ITree other) {
		mate = new TreeGenome(other.getGenome().getChromosomes());
	}

	/* EFFECTS */
	@Override
	public IEffectData[] doEffect(IEffectData[] storedData, World world, BlockPos pos) {
		IAlleleLeafEffect effect = (IAlleleLeafEffect) getGenome().getActiveAllele(TreeChromosome.EFFECT);

		if (effect == null) {
			return null;
		}

		storedData[0] = doEffect(effect, storedData[0], world, pos);

		// Return here if the primary can already not be combined
		if (!effect.isCombinable()) {
			return storedData;
		}

		IAlleleLeafEffect secondary = (IAlleleLeafEffect) getGenome().getInactiveAllele(TreeChromosome.EFFECT);
		if (!secondary.isCombinable()) {
			return storedData;
		}

		storedData[1] = doEffect(secondary, storedData[1], world, pos);

		return storedData;
	}

	private IEffectData doEffect(IAlleleLeafEffect effect, IEffectData storedData, World world, BlockPos pos) {
		storedData = effect.validateStorage(storedData);
		return effect.doEffect(getGenome(), storedData, world, pos);
	}

	@Override
	public IEffectData[] doFX(IEffectData[] storedData, World world, BlockPos pos) {
		return null;
	}

	/* GROWTH */
	@Override
	public WorldGenerator getTreeGenerator(World world, BlockPos pos, boolean wasBonemealed) {
		return genome.getPrimary().getGenerator().getWorldGenerator(this);
	}

	@Override
	public boolean canStay(IBlockAccess world, BlockPos pos) {
		Block block = BlockUtil.getBlock(world, pos.add(0, -1, 0));
		if (block == null) {
			return false;
		}

		for (EnumPlantType type : getPlantTypes()) {
			this.plantType = type;
			if (block.canSustainPlant(world, pos.add(0, -1, 0), EnumFacing.UP, this)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public EnumPlantType getPlantType(IBlockAccess world, BlockPos pos) {
		return plantType;
	}

	@Override
	public IBlockState getPlant(IBlockAccess world, BlockPos pos) {
		return world.getBlockState(pos);
	}

	@Override
	public boolean canGrow(World world, BlockPos pos, int expectedGirth, int expectedHeight) {
		IGrowthProvider growthProvider = genome.getGrowthProvider();
		return growthProvider.canGrow(genome, world, pos, expectedGirth, expectedHeight);
	}

	@Override
	public int getRequiredMaturity() {
		return genome.getMaturationTime();
	}

	@Override
	public EnumGrowthConditions getGrowthCondition(World world, BlockPos pos) {
		return genome.getGrowthProvider().getGrowthConditions(getGenome(), world, pos);
	}

	@Override
	public int getGirth(World world, BlockPos pos) {
		return genome.getGirth();
	}

	@Override
	public int getResilience() {
		int base = (int) (getGenome().getFertility() * getGenome().getSappiness() * 100);
		return (base > 1 ? base : 1) * 10;
	}

	@Override
	public float getHeightModifier() {
		return genome.getHeight();
	}

	@Override
	public void setLeaves(World world, GameProfile owner, BlockPos pos) {
		genome.getPrimary().getGenerator().setLeaves(genome, world, owner, pos, false);
	}

	@Override
	public void setLeavesDecorative(World world, GameProfile owner, BlockPos pos) {
		genome.getPrimary().getGenerator().setLeaves(genome, world, owner, pos, true);
	}

	@Override
	public void setLogBlock(World world, BlockPos pos, EnumFacing facing) {
		genome.getPrimary().getGenerator().setLogBlock(genome, world, pos, facing);
	}

	@Override
	public boolean allowsFruitBlocks() {
		IFruitProvider provider = getGenome().getFruitProvider();
		if (!provider.requiresFruitBlocks()) {
			return false;
		}

		Collection<IFruitFamily> suitable = genome.getPrimary().getSuitableFruit();
		return suitable.contains(provider.getFamily());
	}

	@Override
	public boolean trySpawnFruitBlock(World world, BlockPos pos) {
		IFruitProvider provider = getGenome().getFruitProvider();
		Collection<IFruitFamily> suitable = genome.getPrimary().getSuitableFruit();
		if (!suitable.contains(provider.getFamily())) {
			return false;
		}

		return provider.trySpawnFruitBlock(getGenome(), world, pos);
	}

	/* INFORMATION */
	@Nonnull
	@Override
	public ITreeGenome getGenome() {
		return genome;
	}

	@Override
	public ITree copy() {
		NBTTagCompound nbttagcompound = new NBTTagCompound();
		this.writeToNBT(nbttagcompound);
		return new Tree(nbttagcompound);
	}

	@Nullable
	@Override
	public ITreeGenome getMate() {
		return this.mate;
	}

	@Override
	public boolean isPureBred(TreeChromosome chromosome) {
		return genome.getActiveAllele(chromosome).getUID().equals(genome.getInactiveAllele(chromosome).getUID());
	}

	@Override
	public EnumSet<EnumPlantType> getPlantTypes() {
		return plantTypes;
	}

	@Override
	public void addTooltip(List<String> list) {

		// No info 4 u!
		if (!isAnalyzed) {
			list.add("<" + StringUtil.localize("gui.unknown") + ">");
			return;
		}

		// You analyzed it? Juicy tooltip coming up!
		IAlleleTreeSpecies primary = genome.getPrimary();
		IAlleleTreeSpecies secondary = genome.getSecondary();
		if (!isPureBred(TreeChromosome.SPECIES)) {
			list.add(EnumChatFormatting.BLUE + StringUtil.localize("trees.hybrid").replaceAll("%PRIMARY", primary.getName()).replaceAll("%SECONDARY", secondary.getName()));
		}

		String sappiness = EnumChatFormatting.GOLD + "S: " + genome.getActiveAllele(TreeChromosome.SAPPINESS).getName();
		String maturation = EnumChatFormatting.RED + "M: " + genome.getActiveAllele(TreeChromosome.MATURATION).getName();
		String height = EnumChatFormatting.LIGHT_PURPLE + "H: " + genome.getActiveAllele(TreeChromosome.HEIGHT).getName();
		String girth = EnumChatFormatting.AQUA + "G: " + String.format("%sx%s", genome.getGirth(), genome.getGirth());
		String saplings = EnumChatFormatting.YELLOW + "S: " + genome.getActiveAllele(TreeChromosome.FERTILITY).getName();
		String yield = EnumChatFormatting.WHITE + "Y: " + genome.getActiveAllele(TreeChromosome.YIELD).getName();
		list.add(String.format("%s, %s", saplings, maturation));
		list.add(String.format("%s, %s", height, girth));
		list.add(String.format("%s, %s", yield, sappiness));

		IAlleleBoolean primaryFireproof = (IAlleleBoolean) genome.getActiveAllele(TreeChromosome.FIREPROOF);
		if (primaryFireproof.getValue()) {
			list.add(EnumChatFormatting.RED + StatCollector.translateToLocal("for.gui.fireresist"));
		}

		IAllele fruit = getGenome().getActiveAllele(TreeChromosome.FRUITS);
		if (fruit != AlleleFruit.fruitNone) {
			String strike = "";
			if (!canBearFruit()) {
				strike = EnumChatFormatting.STRIKETHROUGH.toString();
			}
			list.add(strike + EnumChatFormatting.GREEN + "F: " + StringUtil.localize(genome.getFruitProvider().getDescription()));
		}

	}

	/* REPRODUCTION */
	@Override
	public Collection<ITree> getSaplings(World world, GameProfile playerProfile, BlockPos pos, float modifier) {
		List<ITree> prod = new ArrayList<>();

		final float chance = genome.getFertility() * modifier;

		if (world.rand.nextFloat() <= chance) {
			if (this.getMate() == null) {
				prod.add(TreeManager.treeRoot.getTree(world, new TreeGenome(genome.getChromosomes())));
			} else {
				prod.add(createOffspring(world, playerProfile, pos));
			}
		}

		return prod;
	}

	private ITree createOffspring(World world, GameProfile playerProfile, BlockPos pos) {
		if (mate == null) {
			return null;
		}
		ImmutableMap<TreeChromosome, IChromosome> chromosomes = createOffspringChromosomes(world, playerProfile, pos, genome, mate);
		ITreeGenome genome = TreeManager.treeRoot.chromosomesAsGenome(chromosomes);
		return new Tree(genome);
	}

	/* PRODUCTION */
	@Override
	public boolean canBearFruit() {
		return genome.getPrimary().getSuitableFruit().contains(genome.getFruitProvider().getFamily());
	}

	@Override
	public ItemStack[] getProduceList() {
		return genome.getFruitProvider().getProducts();
	}

	@Override
	public ItemStack[] getSpecialtyList() {
		return genome.getFruitProvider().getSpecialty();
	}

	@Override
	public ItemStack[] produceStacks(World world, BlockPos pos, int ripeningTime) {
		return genome.getFruitProvider().getFruits(genome, world, pos, ripeningTime);
	}

}
