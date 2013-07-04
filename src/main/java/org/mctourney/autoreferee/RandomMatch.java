package org.mctourney.autoreferee;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EditSessionFactory;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitUtil;

import com.google.common.collect.Lists;

public class RandomMatch extends AutoRefMatch
{
	private class WorldGenerationTask extends BukkitRunnable
	{
		private Random random;

		private Iterator<MapModule> iter;

		private EditSession edit;

		private int z = 0;

		public WorldGenerationTask(Random random, Iterator<MapModule> iter)
		{
			this.random = random;
			this.iter = iter;

			EditSessionFactory factory = WorldEdit.getInstance().getEditSessionFactory();
			this.edit = factory.getEditSession(BukkitUtil.getLocalWorld(getWorld()), -1);
			this.edit.setFastMode(true);
		}

		@Override
		public void run()
		{
			MapModule module = iter.next();
			AutoRefereeRMG.log(RandomMatch.this + " using " + module, Level.INFO);

			CuboidClipboard clipboard = module.getClipboard();
			clipboard.setOffset(clipboard.getOrigin());

			try { clipboard.place(this.edit, new Vector(0, MapModule.MIN_Y, z), true); }
			catch (MaxChangedBlocksException e) { e.printStackTrace(); }

			this.z += clipboard.getLength();
			if (!iter.hasNext()) this.cancel();
		}
	}

	private Random random = null;

	private WorldGenerationTask worldGenerationTask = null;

	private RandomMatch(World world, Random random, boolean tmp)
	{
		super(world, tmp);
		this.random = random;

		// TODO fix this value once we have a start platform
		Location loc = new Location(this.getWorld(),0,80,0);
		this.setWorldSpawn(loc);
	}

	public static RandomMatch generate(World world, Random random)
	{
		// normal distribution with a mean of 4 and a std deviation of 0.5
		int nummodules = (int) Math.round(4 + random.nextGaussian() * 0.5);
		return RandomMatch.generate(world, random, nummodules);
	}

	public static RandomMatch generate(World world, Random random, int nummodules)
	{
		// this will be the list of modules we are interested in
		List<MapModule> modules = Lists.newArrayList(MapModule.getInstalledModules());

		// swap elements to get the first N elements of the list
		for (int i = 0; i < nummodules; ++i)
			Collections.swap(modules, i, i + random.nextInt(nummodules - i));

		// return the random match provided by the iterator of modules
		return RandomMatch.generate(world, random, modules.subList(0, nummodules).iterator());
	}

	public static RandomMatch generate(World world, Random random, Iterator<MapModule> iter)
	{
		RandomMatch match = new RandomMatch(world, random, true);
		match.worldGenerationTask = match.new WorldGenerationTask(random, iter);
		match.worldGenerationTask.runTaskTimer(AutoRefereeRMG.getInstance(), 0L, 20L);
		return match;
	}
}
