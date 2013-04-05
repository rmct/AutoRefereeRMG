package org.mctourney.autoreferee;

import org.bukkit.plugin.java.JavaPlugin;
import org.mctourney.autoreferee.commands.RandomizerCommands;

public class AutoRefereeRMG extends JavaPlugin
{
	private static AutoRefereeRMG instance = null;

	public static AutoRefereeRMG getInstance()
	{ return instance; }

	@Override
	public void onEnable()
	{
		// set singleton instance
		AutoRefereeRMG.instance = this;

		// get AutoReferee singleton
		AutoReferee ar = AutoReferee.getInstance();

		// register commands
		ar.getCommandManager().registerCommands(new RandomizerCommands(ar), ar);

		getLogger().info(this.getName() + " enabled.");
	}

	@Override
	public void onDisable()
	{
		getLogger().info(this.getName() + " disabled.");
	}
}
