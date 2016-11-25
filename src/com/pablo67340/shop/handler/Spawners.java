package com.pablo67340.shop.handler;

import java.util.HashMap;
import java.util.Map;

public class Spawners {
	private static final Map<Integer, String> ALIASNAMES = new HashMap<Integer, String>();

	static {
		ALIASNAMES.put(50, "Creeper");
		ALIASNAMES.put(51, "Skeleton");
		ALIASNAMES.put(52, "Spider");
		ALIASNAMES.put(53, "Giant Zombie");
		ALIASNAMES.put(54, "Zombie");
		ALIASNAMES.put(55, "Slime");
		ALIASNAMES.put(56, "Ghast");
		ALIASNAMES.put(57, "Zombie Pigman");
		ALIASNAMES.put(58, "Enderman");
		ALIASNAMES.put(59, "Cave Spider");
		ALIASNAMES.put(60, "Silverfish");
		ALIASNAMES.put(61, "Blaze");
		ALIASNAMES.put(62, "Magma Cube");
		ALIASNAMES.put(63, "Ender Dragon");
		ALIASNAMES.put(64, "Wither");
		ALIASNAMES.put(66, "Witch");
		ALIASNAMES.put(67, "Endermite");
		ALIASNAMES.put(68, "Guardian");
		ALIASNAMES.put(69, "Shulker");
		ALIASNAMES.put(101, "Killer Rabbit");
		ALIASNAMES.put(65, "Bat");
		ALIASNAMES.put(90, "Pig");
		ALIASNAMES.put(91, "Sheep");
		ALIASNAMES.put(92, "Cow");
		ALIASNAMES.put(93, "Chicken");
		ALIASNAMES.put(94, "Squid");
		ALIASNAMES.put(95, "Wolf");
		ALIASNAMES.put(96, "Mooshroom");
		ALIASNAMES.put(97, "Snow Golem");
		ALIASNAMES.put(98, "Ocelot");
		ALIASNAMES.put(99, "Iron Golem");
		ALIASNAMES.put(100, "Horse");
		ALIASNAMES.put(101, "Rabbit");
		ALIASNAMES.put(120, "Villager");
	}

	public static String getMobName(Integer name) {
		String mobName = null;
		mobName = ALIASNAMES.get(name);
		return mobName;
	}
}
