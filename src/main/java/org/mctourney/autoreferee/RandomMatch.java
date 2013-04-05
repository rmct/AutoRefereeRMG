package org.mctourney.autoreferee;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class RandomMatch extends AutoRefMatch
{
	private Random random = null;

	private WorldGenerationTask worldGenerationTask = null;

	public class WorldGenerationTask extends BukkitRunnable
	{
		@Override
		public void run()
		{

		}
	}

	public RandomMatch(World world, long seed, boolean tmp)
	{
		super(world, tmp);
		this.random = new Random(seed);

		worldGenerationTask = new WorldGenerationTask();
		worldGenerationTask.runTask(AutoRefereeRMG.getInstance());
	}
}
