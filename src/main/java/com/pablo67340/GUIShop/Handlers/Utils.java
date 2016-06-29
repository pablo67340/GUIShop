package com.pablo67340.GUIShop.Handlers;

import com.pablo67340.GUIShop.Main.Main;
import org.bukkit.ChatColor;

public class Utils {
	protected Main plugin;
	protected boolean verbose;
	protected String prefix;
	protected String menuname;
	protected String command;
	protected String signtitle;
	protected String sellcommand;
	protected String selltitle;
	protected boolean signonly;

	public Utils(Main main) {
		plugin = main;
		verbose = false;
		prefix = "";
		menuname = "";
		command = "";
		signtitle = "";
		sellcommand = "";
		selltitle = "";
		signonly = false;
	}

	public Boolean getVerbose() {
		return verbose;
	}

	public void setVerbose(boolean input) {
		verbose = input;
	}

	public void setPrefix(String input) {
		prefix = ChatColor.translateAlternateColorCodes('&', input);
	}

	public String getPrefix() {
		return prefix;
	}

	public void setMenuName(String input) {
		menuname = ChatColor.translateAlternateColorCodes('&', input);
	}

	public String getMenuName() {
		return menuname;
	}

	public void setSellTitle(String input) {
		selltitle = input;
	}

	public String getSellTitle() {
		return selltitle;
	}

	public void setCommand(String input) {
		command = input;
	}

	public String getCommand() {
		return command;
	}

	public void setSellCommand(String input) {
		sellcommand = input;
	}

	public String getSellCommand() {
		return sellcommand;
	}

	public void setSignTitle(String input) {
		signtitle = ChatColor.translateAlternateColorCodes('&', input);
	}

	public String getSignTitle() {
		return signtitle;
	}

	public void setSignOnly(boolean input) {
		signonly = input;
	}

	public Boolean getSignOnly() {
		return signonly;
	}

	public boolean isInteger(String s, int radix) {
		if (s.isEmpty()) {
			return false;
		}
		for (int i = 0; i < s.length(); ++i) {
			if (!(i == 0 && s.charAt(i) == '-' ? s.length() == 1 : Character.digit(s.charAt(i), radix) < 0)) continue;
			return false;
		}
		return true;
	}
}

