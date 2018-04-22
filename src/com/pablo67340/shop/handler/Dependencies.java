package com.pablo67340.shop.handler;

import org.bukkit.Bukkit;

import com.songoda.epicspawners.EpicSpawners;

import de.dustplanet.util.SilkUtil;

public final class Dependencies {

	/**
	 * True/False if the server is running the input dependency
	 * 
	 * @param Plugin
	 *            Name
	 */
	public static Boolean hasDependency(String name) {
		return Bukkit.getServer().getPluginManager().isPluginEnabled(name);
	}

	/**
	 * Gets the API instance using plugin specific methods.
	 * 
	 * @return {@link Object} Object that needs casting to original form.
	 */
	public static Object getDependencyInstance(String name) {
		if (name.equalsIgnoreCase("SilkSpawners")) {
			return SilkUtil.hookIntoSilkSpanwers();
		} else if (name.equalsIgnoreCase("EpicSpawners")) {
			return EpicSpawners.pl().getApi();
		} else {
			return null;
		}
	}

}
