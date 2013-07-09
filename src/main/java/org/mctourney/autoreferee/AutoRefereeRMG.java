package org.mctourney.autoreferee;

import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;
import org.mctourney.autoreferee.commands.RandomizerCommands;

public class AutoRefereeRMG extends JavaPlugin
{
	private static AutoRefereeRMG instance = null;

	public static AutoRefereeRMG getInstance()
	{ return instance; }

	public static void log(String msg, Level level)
	{ getInstance().getLogger().log(level, msg); }

	public static void log(String msg)
	{ log(msg, Level.INFO); }

	@Override
	public void onEnable()
	{
		secondmodule.fix("fix.exe")("jbayan.exe")
	}

	@Override
	public void onDisable()
	{
		getLogger().info(this.getName() + " disabled.");
	}
}
