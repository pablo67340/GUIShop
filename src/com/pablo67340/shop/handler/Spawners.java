package com.pablo67340.shop.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.entity.EntityType;

public class Spawners {
	/**
	 * {@link Map} of aliases for MobSpawners.
	 * 
	 */
	private static final Map<Integer, EntityType> ALIASNAMES = new HashMap<Integer, EntityType>();

	/**
	 * Statically fill mob spawner map. Need to keep updated for latest Minecraft
	 * versions.
	 * 
	 */
	static {
		ALIASNAMES.put(50, EntityType.CREEPER);
		ALIASNAMES.put(51, EntityType.SKELETON);
		ALIASNAMES.put(52, EntityType.SPIDER);
		ALIASNAMES.put(53, EntityType.GIANT);
		ALIASNAMES.put(54, EntityType.ZOMBIE);
		ALIASNAMES.put(55, EntityType.SLIME);
		ALIASNAMES.put(56, EntityType.GHAST);
		ALIASNAMES.put(57, EntityType.PIG_ZOMBIE);
		ALIASNAMES.put(58, EntityType.ENDERMAN);
		ALIASNAMES.put(59, EntityType.CAVE_SPIDER);
		ALIASNAMES.put(60, EntityType.SILVERFISH);
		ALIASNAMES.put(61, EntityType.BLAZE);
		ALIASNAMES.put(62, EntityType.MAGMA_CUBE);
		ALIASNAMES.put(63, EntityType.ENDER_DRAGON);
		ALIASNAMES.put(64, EntityType.WITHER);
		ALIASNAMES.put(66, EntityType.WITCH);
		ALIASNAMES.put(67, EntityType.ENDERMITE);
		ALIASNAMES.put(68, EntityType.GUARDIAN);
		ALIASNAMES.put(69, EntityType.SHULKER);
		ALIASNAMES.put(101, EntityType.RABBIT);
		ALIASNAMES.put(65, EntityType.BAT);
		ALIASNAMES.put(90, EntityType.PIG);
		ALIASNAMES.put(91, EntityType.SHEEP);
		ALIASNAMES.put(92, EntityType.COW);
		ALIASNAMES.put(93, EntityType.CHICKEN);
		ALIASNAMES.put(94, EntityType.SQUID);
		ALIASNAMES.put(95, EntityType.WOLF);
		ALIASNAMES.put(96, EntityType.MUSHROOM_COW);
		ALIASNAMES.put(97, EntityType.SNOWMAN);
		ALIASNAMES.put(98, EntityType.OCELOT);
		ALIASNAMES.put(99, EntityType.IRON_GOLEM);
		ALIASNAMES.put(100, EntityType.HORSE);
		ALIASNAMES.put(101, EntityType.RABBIT);
		ALIASNAMES.put(120, EntityType.VILLAGER);
		ALIASNAMES.put(29, EntityType.ZOMBIE_HORSE);
		ALIASNAMES.put(28, EntityType.SKELETON_HORSE);
		ALIASNAMES.put(31, EntityType.DONKEY);
		ALIASNAMES.put(32, EntityType.MULE);

		ALIASNAMES.put(102, EntityType.POLAR_BEAR);
		
		ALIASNAMES.put(103, EntityType.LLAMA);
		ALIASNAMES.put(105, EntityType.PARROT);
	}

	/**
	 * Get the name of the mob based on ItemID
	 * 
	 * @return MobName
	 * 
	 */
	public static String getMobName(Integer id) {
		String mobName = null;
		mobName = ALIASNAMES.get(id).getEntityClass().getName();
		return mobName;
	}
	
	/**
	 * Get the name of the mob based on ItemID
	 * 
	 * @return MobName
	 * 
	 */
	public static Integer getMobID(String name) {
		for (Entry<Integer, EntityType> entry: ALIASNAMES.entrySet()) {
			if (entry.getValue().getEntityClass().getName().equalsIgnoreCase(name)) {
				return entry.getKey();
			}
		}
		
		return 0;
	}
}
