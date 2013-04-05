package org.mctourney.autoreferee;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.data.DataException;

import com.sk89q.worldedit.schematic.SchematicFormat;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import com.google.common.collect.Sets;

public class MapModule
{
	public enum ResourceType
	{
		/**
		 * Materials with which to make tools and armor from.
		 */
		ORE(new Material[]
		{
			Material.IRON_ORE,
			Material.IRON_BLOCK,
			Material.DIAMOND_ORE,
			Material.DIAMOND_BLOCK,
		}),

		/**
		 * Materials with which to make tools from.
		 */
		WOOD(new Material[]
		{
			Material.WOOD,
			Material.LOG,
		}),

		/**
		 * Any materials that can be used to bridge quickly.
		 */
		BUILDING(new Material[]
		{
			Material.DIRT,
			Material.WOOD,
			Material.LOG,
			Material.STONE,
			Material.COBBLESTONE,
		}),

		/**
		 * Food to restore hunger.
		 */
		FOOD(new Material[]
		{
			Material.WHEAT,
			Material.BREAD,
			Material.COOKIE,
			Material.POTATO_ITEM,
			Material.BAKED_POTATO,
			Material.CARROT_ITEM,
			Material.APPLE,
			Material.RAW_CHICKEN,
			Material.COOKED_CHICKEN,
			Material.RAW_BEEF,
			Material.COOKED_BEEF,
			Material.PORK,
			Material.GRILLED_PORK,
			Material.RAW_FISH,
			Material.COOKED_FISH,
			Material.MELON,
			Material.MELON_BLOCK,
			Material.PUMPKIN_PIE,
			Material.CAKE,
			Material.MUSHROOM_SOUP,
		}),

		/**
		 * Materials that may be used to construct a cannon.
		 */
		CANNON(new Material[]
		{
			Material.TNT,
		}),

		/**
		 * Materials that produce light, to help with darkness.
		 */
		LIGHT(new Material[]
		{
			Material.TORCH,
			Material.GLOWSTONE,
		}),

		/**
		 * Water sources, which can help with lava or climbing.
		 */
		WATER(new Material[]
		{
			Material.WATER_BUCKET
		});

		Material materials[];
		ResourceType(Material[] materials)
		{
			this.materials = materials;
		}
	}

	public enum HazardType
	{
		/**
		 * Lava, fire, blazes, and fire zombies can all set the player on fire.
		 */
		FIRE(ResourceType.WATER, PotionEffectType.FIRE_RESISTANCE),

		/**
		 * Darkness restricts the players vision and often increase the danger
		 * of other hazards within the affected area.
		 */
		DARKNESS(ResourceType.LIGHT, PotionEffectType.NIGHT_VISION),

		/**
		 * Areas that are filled with water may result in a player drowning.
		 */
		WATER(null, PotionEffectType.WATER_BREATHING),

		/**
		 * Open spaces with a clear vantage point from somewhere higher on the
		 * opposite lane may invite arrow fire. Shared regions involve melee.
		 */
		PVP(null, PotionEffectType.DAMAGE_RESISTANCE),

		/**
		 * Areas forcing the player to dig can slow or stop a team.
		 */
		DIGGING(null, null),

		/**
		 * Areas with open gaps require bridging to cross, which is often an
		 * excellent place for advanced PvP pressure. Building materials may
		 * be required to progress.
		 */
		BRIDGING(ResourceType.BUILDING, null),

		/**
		 * Other.
		 */
		GENERIC(null, null);

		// solutions to the hazards
		ResourceType resource;
		PotionEffectType effect;

		HazardType(ResourceType resource, PotionEffectType effect)
		{
			this.resource = resource;
			this.effect = effect;
		}
	}

	private Set<ResourceType> resources = Sets.newHashSet();

	private Set<HazardType> hazards = Sets.newHashSet();

	private CuboidClipboard clipboardData = null;

	private MapModule(File module, File metadata)
		throws IOException, DataException
	{
		clipboardData = SchematicFormat.MCEDIT.load(module);
		this.loadMetadata(metadata);
	}

	public static MapModule fromModuleFolder(File folder)
	{
		File module = new File(folder, "module.schematic");
		File metadata = new File(folder, "module.xml");

		if (module.canRead() && !module.canRead())
			try { return new MapModule(module, metadata); }
			catch (IOException e) { e.printStackTrace(); }
			catch (DataException e) { e.printStackTrace(); }

		return null;
	}

	public void loadMetadata(File metadata)
	{

	}
}
