package com.pablo67340.guishop.autosellstuff;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public class ChestLocation {
    @Getter
    public final double x, y, z;

    @Getter
    public final String worldName;

    @Getter
    public final UUID owner;

    @JsonCreator
    public ChestLocation(@JsonProperty("x") double x, @JsonProperty("y") double y,
                         @JsonProperty("z") double z, @JsonProperty("worldName") String worldName,
                         @JsonProperty("owner") String owner) {
        this(x, y, z, worldName, UUID.fromString(owner));
    }

    @JsonIgnore
    public ChestLocation(double x, double y, double z, String worldName, UUID owner) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldName = worldName;
        this.owner = owner;
    }

    @JsonIgnore
    public ChestLocation(Location loc, UUID owner) {
        this(loc.getX(), loc.getY(), loc.getZ(),
                Objects.requireNonNull(loc.getWorld()).getName(), owner);
    }

    @JsonIgnore
    public boolean isAtSameLocation(@NotNull Location that) {
        return Math.abs(this.x - that.getX()) < 0.01 &&
                Math.abs(this.y - that.getY()) < 0.01 &&
                Math.abs(this.z - that.getZ()) < 0.01 &&
                worldName.equalsIgnoreCase(that.getWorld().getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChestLocation that = (ChestLocation) o;
        return Math.abs(this.x - that.x) < 0.01 &&
                Math.abs(this.y - that.y) < 0.01 &&
                Math.abs(this.z - that.z) < 0.01 &&
                worldName.equalsIgnoreCase(that.worldName) &&
                this.owner.equals(that.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, worldName);
    }

    private static ObjectMapper mapper;

    public static ObjectMapper getMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper();
        }
        return mapper;
    }
}