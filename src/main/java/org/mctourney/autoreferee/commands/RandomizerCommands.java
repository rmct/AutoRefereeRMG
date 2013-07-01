package org.mctourney.autoreferee.commands;

import java.io.IOException;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import org.apache.commons.cli.CommandLine;

import org.mctourney.autoreferee.AutoRefMatch;
import org.mctourney.autoreferee.AutoReferee;
import org.mctourney.autoreferee.MapModule;
import org.mctourney.autoreferee.RandomMatch;
import org.mctourney.autoreferee.util.NullChunkGenerator;
import org.mctourney.autoreferee.util.commands.AutoRefCommand;
import org.mctourney.autoreferee.util.commands.AutoRefPermission;
import org.mctourney.autoreferee.util.commands.CommandHandler;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldedit.data.DataException;

public class RandomizerCommands implements CommandHandler
{
	private static final float WORLDEDIT_BASE_ROTATION = 90.0f;

	AutoReferee plugin;

	public RandomizerCommands(Plugin plugin)
	{
		this.plugin = (AutoReferee) plugin;
	}

	@AutoRefCommand(name={"autoref", "loadrandom"}, argmax=0, options="s+",
		description="Generate a new random map with a given seed.")
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

	@AutoRefCommand(name={"autoref", "module", "save"}, argmin=1, argmax=1, options="f",
		description="Save a ARM to the local module directory.")
	@AutoRefPermission(console=false, nodes={"autoreferee.configure"})

	public boolean moduleSave(CommandSender sender, AutoRefMatch match, String[] args, CommandLine options)
		throws IOException, DataException
	{
		Player player = (Player) sender;
		WorldEditPlugin worldedit = AutoReferee.getWorldEdit();

		Selection selection = worldedit.getSelection(player);
		Vector vmin = selection.getNativeMinimumPoint();
		Vector vmax = selection.getNativeMaximumPoint();

		vmin.setY(MapModule.MIN_Y);
		vmax.setY(MapModule.MAX_Y);

		CuboidClipboard clipboard = new CuboidClipboard(vmax.subtract(vmin).add(1,1,1), vmin, new Vector(0,0,0));
		clipboard.copy(worldedit.createEditSession(player));

		float pfacing = Math.round(player.getLocation().getYaw() / 90.0f) * 90.0f;
		clipboard.rotate2D((int)(WORLDEDIT_BASE_ROTATION + MapModule.FORWARD_YAW - pfacing));

		int w = clipboard.getWidth();
		if (w > 20 && !options.hasOption('f'))
		{
			sender.sendMessage("" + ChatColor.RED + "You are attempting to save this module with a width of " + w);
			sender.sendMessage("" + ChatColor.RED + "If this is intentional, please add the -f flag to continue.");
			return true;
		}

		MapModule module = new MapModule(args[0], clipboard);
		module.save();

		sender.sendMessage("" + ChatColor.GREEN + module + " saved!");
		return true;
	}
}
