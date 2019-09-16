package com.pablo67340.guishop.definition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.pablo67340.guishop.main.Main;

public class ItemCommand {

	private Set<String> commands = new HashSet<>();

	private Map<String, Expires> expires = new HashMap<>();

	private UUID uuid;

	public ItemCommand(List<String> commands, UUID uuid, Boolean isNew, String startDate) {
		this.uuid = uuid;
		for (String cmd : commands) {
			String command;
			String unit = cmd.substring(cmd.length() - 1);
			Integer duration;
			if (cmd.contains("::")) {
				String[] parts = cmd.split("::");
				command = parts[0];
				duration = Integer.parseInt(parts[1].replace(unit, ""));
				if (isNew) {
					Main.getInstance().addCommand(uuid, command, duration.toString() + unit, startDate.toString());
				}
			} else {
				command = cmd;
				duration = 0;
			}
			this.commands.add(command);
			Expires expire = new Expires(duration, unit, startDate);
			expires.put(command, expire);
		}

	}

	public Set<String> getCommands() {
		return commands;
	}

	public Set<String> getValidCommands() {
		Set<String> validCommands = new HashSet<>();

		for (String cmd : commands) {
			Expires expiration = getExpiration(cmd);

			if (!expiration.isExpired()) {
				validCommands.add(cmd);
			}
		}

		return validCommands;
	}

	public UUID getUUID() {
		return uuid;
	}

	public Map<String, Expires> getExpires() {
		return expires;
	}

	public Expires getExpiration(String command) {
		return expires.get(command);
	}

	public Boolean addCommands(List<String> commands, String startDate) {
		for (String cmd : commands) {
			String command;
			String unit = cmd.substring(cmd.length() - 1);
			Integer duration;
			if (cmd.contains("::")) {
				String[] parts = cmd.split("::");
				command = parts[0];
				duration = Integer.parseInt(parts[1].replace(unit, ""));
			} else {
				command = cmd;
				duration = 0;
			}
			if (!isAlreadyPurchased(command)) {
				Main.getInstance().addCommand(uuid, command, duration.toString() + unit, startDate.toString());
				commands.add(command);
				Expires expire = new Expires(duration, unit, startDate);
				expires.put(command, expire);
				return true;
			} else {
				return false;
			}

		}
		return false;
	}

	public void removeCommand(String command) {
		commands.remove(command);
		Main.getInstance().removeCommand(uuid, command);
	}

	public Boolean isAlreadyPurchased(String command) {
		return expires.containsKey(command);
	}

}
