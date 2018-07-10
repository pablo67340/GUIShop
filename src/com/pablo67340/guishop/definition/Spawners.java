package com.pablo67340.guishop.definition;

import java.util.HashMap;
import java.util.Map;



public class Spawners {
	/**
	 * {@link Map} of aliases for MobSpawners.
	 * 
	 */
	private static final Map<String, Integer> ALIASNAMES = new HashMap<>();

	/**
	 * Statically fill mob spawner map. Need to keep updated for latest Minecraft
	 * versions.
	 * 
	 */
	static {
		
		ALIASNAMES.put("CREEPER", 50);
		ALIASNAMES.put("SKELETON", 51);
		ALIASNAMES.put("SPIDER", 52);
		ALIASNAMES.put("GIANT", 53);
		ALIASNAMES.put("ZOMBIE", 54);
		ALIASNAMES.put("SLIME", 55);
		ALIASNAMES.put("GHAST", 56);
		ALIASNAMES.put("PIG_ZOMBIE", 57);
		ALIASNAMES.put("ENDERMAN", 58);
		ALIASNAMES.put("CAVE_SPIDER", 59);
		ALIASNAMES.put("SILVERFISH", 60);
		ALIASNAMES.put("BLAZE", 61);
		ALIASNAMES.put("MAGMA_CUBE", 62);
		ALIASNAMES.put("ENDER_DRAGON", 63);
		ALIASNAMES.put("WITHER", 64);
		ALIASNAMES.put("WITCH", 66);
		ALIASNAMES.put("ENDERMITE", 67);
		ALIASNAMES.put("GUARDIAN", 68);
		ALIASNAMES.put("SHULKER", 69);
		ALIASNAMES.put("RABBIT", 101);
		ALIASNAMES.put("BAT", 65);
		ALIASNAMES.put("PIG", 90);
		ALIASNAMES.put("SHEEP", 91);
		ALIASNAMES.put("COW", 92);
		ALIASNAMES.put("CHICKEN", 93);
		ALIASNAMES.put("SQUID", 94);
		ALIASNAMES.put("WOLF", 95);
		ALIASNAMES.put("MUSHROOM_COW", 96);
		ALIASNAMES.put("SNOWMAN", 97);
		ALIASNAMES.put("OCELOT", 98);
		ALIASNAMES.put("IRON_GOLEM", 99);
		ALIASNAMES.put("HORSE", 100);
		ALIASNAMES.put("RABBIT", 101);
		ALIASNAMES.put("VILLAGER", 120);
		ALIASNAMES.put("ZOMBIE_HORSE", 29);
		ALIASNAMES.put("SKELETON_HORSE", 28);
		ALIASNAMES.put("DONKEY", 31);
		ALIASNAMES.put("MULE", 32);
		ALIASNAMES.put("POLAR_BEAR", 102);
		ALIASNAMES.put("LLAMA", 103);
		ALIASNAMES.put("PARROT", 105);

	}

	/**
	 * Get the name of the mob based on ItemID
	 * 
	 * @return MobName
	 * 
	 */
	public static String getMobName(Integer id) {
		for (Map.Entry<String, Integer> entry : ALIASNAMES.entrySet()) {
			if (entry.getValue() == id) {
				return entry.getKey();
			}
		}
		return "";
	}

	/**
	 * Get the name of the mob based on ItemID
	 * 
	 * @return MobName
	 * 
	 */
	public static Integer getMobID(String name) {
		return ALIASNAMES.get(name);
	}
}
