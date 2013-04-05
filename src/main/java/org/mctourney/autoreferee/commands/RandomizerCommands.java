package org.mctourney.autoreferee.commands;

import org.bukkit.Bukkit;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import org.apache.commons.cli.CommandLine;

import org.mctourney.autoreferee.AutoRefMatch;
import org.mctourney.autoreferee.AutoReferee;
import org.mctourney.autoreferee.RandomMatch;
import org.mctourney.autoreferee.util.NullChunkGenerator;
import org.mctourney.autoreferee.util.commands.AutoRefCommand;
import org.mctourney.autoreferee.util.commands.AutoRefPermission;
import org.mctourney.autoreferee.util.commands.CommandHandler;

import java.util.Date;

public class RandomizerCommands implements CommandHandler
{
	AutoReferee plugin;

	public RandomizerCommands(Plugin plugin)
	{
		this.plugin = (AutoReferee) plugin;
	}

	@AutoRefCommand(name={"autoref", "loadrandom"}, argmax=0, options="s+",
		description="An example command to add.")
	@AutoRefPermission(console=true, nodes={"autoreferee.admin"})

	public boolean generateRandom(CommandSender sender, AutoRefMatch match, String[] args, CommandLine options)
	{
		String worldname = AutoReferee.WORLD_PREFIX + Long.toHexString(new Date().getTime());
		WorldCreator creator = WorldCreator.name(worldname).generator(new NullChunkGenerator());

		long seed = System.currentTimeMillis();
		if (options.hasOption('s'))
		{
			// parse out the provided seed (numbers are converted verbatim)
			seed = options.getOptionValue('s').hashCode();
			try { seed = Long.parseLong(options.getOptionValue('s')); }
			catch (NumberFormatException e) {  }
		}

		// add the randomized map object
		plugin.addMatch(new RandomMatch(Bukkit.createWorld(creator), seed, true));
		return true;
	}
}
