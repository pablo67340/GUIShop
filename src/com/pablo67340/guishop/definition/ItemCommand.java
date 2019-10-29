package com.pablo67340.guishop.definition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.pablo67340.guishop.Main;

import lombok.Getter;

public class ItemCommand {

    @Getter
    private Set<String> commands = new HashSet<>();

    @Getter
    private Map<String, Expires> expires = new HashMap<>();

    @Getter
    private UUID uuid;

    public ItemCommand(List<String> commands, UUID uuid, Boolean isNew, String startDate) {
        this.uuid = uuid;
        for (String cmd : commands) {
            String command;
            String unit = cmd.substring(cmd.length() - 1);
            int duration;
            if (cmd.contains("::")) {
                String[] parts = cmd.split("::");
                command = parts[0];
                duration = Integer.parseInt(parts[1].replace(unit, ""));
                if (isNew) {
                    Main.getINSTANCE().addCommand(uuid, command, duration + unit, startDate);
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

    public Expires getExpiration(String command) {
        return expires.get(command);
    }

}
