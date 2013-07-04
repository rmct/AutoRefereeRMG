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
import com.sk89q.worldedit.CuboidClipboard.FlipDirection;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EditSessionFactory;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitUtil;

import com.google.common.collect.Lists;

public class RandomMatch extends AutoRefMatch
{
	private static final int DEFAULT_VOID_WIDTH = 16;

	private class WorldGenerationTask extends BukkitRunnable
	{
		private Random random;

		private Iterator<MapModule> iter;

		private EditSession edit;

		private int z = 0;
		private int maxModuleWidth = 0;

		public WorldGenerationTask(Random random, List<MapModule> modules)
		{
			this.random = random;
			this.iter = modules.iterator();

			for (MapModule module : modules)
			{
				if (module.getWidth() > maxModuleWidth)
					maxModuleWidth = module.getWidth();
			}

			EditSessionFactory factory = WorldEdit.getInstance().getEditSessionFactory();
			this.edit = factory.getEditSession(BukkitUtil.getLocalWorld(getWorld()), -1);
			this.edit.setFastMode(true);
		}

		@Override
		public void run()
		{

			if (iter.hasNext()) copyNextModule();
			else completeGeneration();
		}

		public void copyNextModule()
		{
			MapModule module = iter.next();
			AutoRefereeRMG.log(RandomMatch.this + " using " + module, Level.INFO);

			try
			{
				Vector pos = new Vector(0, MapModule.MIN_Y, z);
				CuboidClipboard clipboard = module.getClipboard();
				clipboard.setOffset(clipboard.getOrigin());

				Vector laneL = pos.setX( voidWidth / 2 + 1);
				Vector laneR = pos.setX(-voidWidth / 2 - maxModuleWidth);

				clipboard.place(this.edit, laneR, true);
				clipboard.flip(FlipDirection.WEST_EAST);
				clipboard.place(this.edit, laneL, true);

				this.z += clipboard.getLength();
			}
			catch (MaxChangedBlocksException e) { e.printStackTrace(); }
		}

		public void completeGeneration()
		{
			// TODO fix up AutoReferee configuration
			this.cancel();
		}
	}

	private Random random = null;

	private WorldGenerationTask worldGenerationTask = null;

	private int voidWidth = DEFAULT_VOID_WIDTH;

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
		for (int i = 0, msz = modules.size(); i < nummodules; ++i)
			Collections.swap(modules, i, i + random.nextInt(msz - i));

		// return the random match provided by the iterator of modules
		return RandomMatch.generate(world, random, modules.subList(0, nummodules));
	}

	public static RandomMatch generate(World world, Random random, List<MapModule> modules)
	{
		RandomMatch match = new RandomMatch(world, random, true);
		match.worldGenerationTask = match.new WorldGenerationTask(random, modules);
		match.worldGenerationTask.runTaskTimer(AutoRefereeRMG.getInstance(), 0L, 20L);
		return match;
	}
}
