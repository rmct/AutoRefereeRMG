package org.mctourney.autoreferee;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.mctourney.autoreferee.regions.CuboidRegion;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.CuboidClipboard.FlipDirection;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EditSessionFactory;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.schematic.SchematicFormat;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class RandomMatch extends AutoRefMatch
{
	private static final int DEFAULT_VOID_WIDTH = 16;

	private class WorldGenerationTask extends BukkitRunnable
	{
		private Random random;

		private Iterator<MapModule> iter;
		private int m = 0, modulecount;

		private EditSession edit;
		private boolean needstart = true;

		private int z = 0;

		public WorldGenerationTask(Random random, List<MapModule> modules)
		{
			this.random = random;
			this.modulecount = modules.size();
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
			if (!needstart)
			{
				if (iter.hasNext()) copyNextModule();
				else completeGeneration();
			}
			else copyStartPlatform();

			int c = 100 * m / modulecount;
			if (waitingPlayers != null) for (Player p : waitingPlayers)
				p.sendMessage(String.format("%s: %d%% loaded", getMapName(), c));
		}

		public void copyNextModule()
		{
			MapModule module = iter.next(); ++m;
			AutoRefereeRMG.log(RandomMatch.this + " using " + module, Level.INFO);
			AutoRefTeam teamL = getTeam("Left"), teamR = getTeam("Right");

			World world = getWorld();
			try
			{
				Vector pos = new Vector(0, MapModule.MIN_Y, z);
				CuboidClipboard clipboard = module.getClipboard();
				clipboard.setOffset(clipboard.getOrigin());

				// TODO center the lanes around x=16 and x=-16, respectively, for symmetry with start platform
				Vector laneL = pos.setX( voidWidth / 2 + (maxModuleWidth - module.getWidth()) / 2);
				Vector laneR = pos.setX(-voidWidth / 2 - (maxModuleWidth + module.getWidth()) / 2);
				Vector v = clipboard.getSize();

				clipboard.place(this.edit, laneR, false);
				clipboard.flip(FlipDirection.WEST_EAST);
				clipboard.place(this.edit, laneL, false);

				teamL.addRegion(new CuboidRegion(
					BukkitUtil.toLocation(world, laneL),
					BukkitUtil.toLocation(world, laneL.add(v).subtract(1,1,1))
				));

				teamR.addRegion(new CuboidRegion(
					BukkitUtil.toLocation(world, laneR),
					BukkitUtil.toLocation(world, laneR.add(v).subtract(1,1,1))
				));

				for (int x = 0; x < v.getBlockX(); ++x)
				for (int z = 0; z < v.getBlockZ(); ++z)
				{
					world.setBiome(laneL.getBlockX() + x, laneL.getBlockZ() + z, Biome.PLAINS);
					world.setBiome(laneR.getBlockX() + x, laneR.getBlockZ() + z, Biome.PLAINS);
				}

				this.z += clipboard.getLength();
			}
			catch (MaxChangedBlocksException e) { e.printStackTrace(); }
		}

		public void completeGeneration()
		{
			World w = getWorld();

			// bring players who are waiting, remove waiting list
			for (Player p : waitingPlayers) p.teleport(getWorldSpawn());
			waitingPlayers = null;

			this.cancel();
		}

		public void copyStartPlatform()
		{
			needstart = false;
			World world = getWorld();
			File tmp_schm = null;

			try
			{
				tmp_schm = File.createTempFile("start", null, FileUtils.getTempDirectory());
				InputStream start_strm = AutoRefereeRMG.getInstance().getResource("worldbase/start.schematic");
				FileUtils.copyInputStreamToFile(start_strm, tmp_schm);

				Vector origin = new Vector(0, 64, 0);
				CuboidClipboard clipboard = SchematicFormat.MCEDIT.load(tmp_schm);
				clipboard.paste(this.edit, origin, false, true);

				Vector min = origin.add(clipboard.getOffset());
				Vector v = clipboard.getSize();

				for (int x = 0; x < v.getBlockX(); ++x)
				for (int z = 0; z < v.getBlockZ(); ++z)
					world.setBiome(min.getBlockX() + x, min.getBlockZ() + z, Biome.PLAINS);
			}
			catch (DataException e) { e.printStackTrace(); }
			catch (MaxChangedBlocksException e) { e.printStackTrace(); }
			catch (IOException e) { e.printStackTrace(); }
			finally
			{
				if (tmp_schm != null) FileUtils.deleteQuietly(tmp_schm);
				loadBaseWorldConfiguration();
			}
		}
	}

	private Random random = null;

	private WorldGenerationTask worldGenerationTask = null;

	private int voidWidth = DEFAULT_VOID_WIDTH;
	private int maxModuleWidth = 0;

	private Set<Player> waitingPlayers = Sets.newHashSet();

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
		match.worldGenerationTask.runTaskTimer(AutoRefereeRMG.getInstance(), 5L, 5L);

		// add all appropriate authors
		match.mapAuthors = Sets.newHashSet();
		for (MapModule module : modules)
			match.mapAuthors.addAll(module.getAuthors());

		match.mapName = "AutoRefereeRMG";
		return match;
	}

	private void loadBaseWorldConfiguration()
	{
		InputStream basexml = AutoRefereeRMG.getInstance().getResource("worldbase/autoreferee.xml");
		loadWorldConfiguration(basexml);
	}

	@Override
	protected void loadWorldConfiguration()
	{
	}

	@Override
	public void joinMatch(Player player)
	{
		if (waitingPlayers == null) super.joinMatch(player);
		else waitingPlayers.add(player);
	}
}
