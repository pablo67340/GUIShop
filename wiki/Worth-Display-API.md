# Worth Display API

The Worth Display API allows other plugins to integrate with GUIShop's worth display system. This is useful for:

- Providing persistent per-player worth settings
- Checking item worth programmatically
- Controlling worth display for specific players

## Checking Worth Display Status

```java
import com.pablo67340.guishop.api.GUIShopAPI;

// Check if the worth display system is enabled and running
boolean isEnabled = GUIShopAPI.isWorthDisplayEnabled();
```

## Getting Item Worth

```java
import java.math.BigDecimal;

// Get the sell value of a single item
BigDecimal singleWorth = GUIShopAPI.getItemWorth(itemStack);
// Returns null if item is not sellable

// Get the sell value of the entire stack
BigDecimal stackWorth = GUIShopAPI.getStackWorth(itemStack);
// Returns null if item is not sellable
```

## Per-Player Worth Toggle

### Session-Based Toggle

Toggle worth display for a player (resets on server restart):

```java
// Toggle worth display (returns new state: true = enabled, false = disabled)
boolean nowEnabled = GUIShopAPI.toggleWorthForPlayer(player);

// Enable worth display
GUIShopAPI.enableWorthForPlayer(player);

// Disable worth display
GUIShopAPI.disableWorthForPlayer(player);

// Check if worth is enabled for a player
boolean isEnabled = GUIShopAPI.isWorthEnabledForPlayer(player);
```

### Persistent Settings via External Hook

For persistent per-player settings, set an external check predicate:

```java
import java.util.function.Predicate;

// Set up a hook that GUIShop will call to check if worth should be disabled
GUIShopAPI.setExternalWorthCheck(player -> {
    // Return TRUE to DISABLE worth display for this player
    // Return FALSE to allow worth display (defer to other checks)
    return myPlugin.hasWorthDisabled(player.getUniqueId());
});

// Remove the hook
GUIShopAPI.removeExternalWorthCheck();
```

## Complete Integration Example

Here's a complete example of a plugin that provides persistent worth toggle settings:

```java
import com.pablo67340.guishop.api.GUIShopAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WorthSettingsPlugin extends JavaPlugin {
    
    // Players who have worth display disabled (persist this to config/database)
    private Set<UUID> disabledPlayers = new HashSet<>();
    
    @Override
    public void onEnable() {
        // Load disabled players from config
        loadDisabledPlayers();
        
        // Register hook with GUIShop
        if (getServer().getPluginManager().isPluginEnabled("GUIShop")) {
            registerGUIShopHook();
        }
    }
    
    private void registerGUIShopHook() {
        // This predicate is called by GUIShop to check if worth should be hidden
        GUIShopAPI.setExternalWorthCheck(player -> {
            return disabledPlayers.contains(player.getUniqueId());
        });
        
        getLogger().info("Registered worth display hook with GUIShop");
    }
    
    @Override
    public void onDisable() {
        // Clean up the hook
        if (getServer().getPluginManager().isPluginEnabled("GUIShop")) {
            GUIShopAPI.removeExternalWorthCheck();
        }
        
        // Save settings
        saveDisabledPlayers();
    }
    
    /**
     * Toggle worth display for a player (persistent).
     * @return true if worth is now enabled, false if disabled
     */
    public boolean toggleWorth(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (disabledPlayers.contains(uuid)) {
            disabledPlayers.remove(uuid);
            saveDisabledPlayers();
            return true; // Now enabled
        } else {
            disabledPlayers.add(uuid);
            saveDisabledPlayers();
            return false; // Now disabled
        }
    }
    
    /**
     * Check if worth is enabled for a player.
     */
    public boolean isWorthEnabled(Player player) {
        return !disabledPlayers.contains(player.getUniqueId());
    }
    
    private void loadDisabledPlayers() {
        // Load from config.yml
        for (String uuidStr : getConfig().getStringList("disabled-worth-players")) {
            try {
                disabledPlayers.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {}
        }
    }
    
    private void saveDisabledPlayers() {
        // Save to config.yml
        getConfig().set("disabled-worth-players", 
            disabledPlayers.stream()
                .map(UUID::toString)
                .toList());
        saveConfig();
    }
}
```

## Priority Order

When checking if worth should be displayed for a player, GUIShop checks in this order:

1. **Session toggle** (from `/gs toggleworth` command)
2. **External hook** (from `setExternalWorthCheck`)
3. **Default** (enabled)

If any check returns "disabled", worth will not be shown.

## Thread Safety

The worth display check is called from the packet handling thread. Ensure your external check predicate is thread-safe:

```java
// Good: Use thread-safe collections
private final Set<UUID> disabledPlayers = ConcurrentHashMap.newKeySet();

// Or synchronize access
private final Set<UUID> disabledPlayers = new HashSet<>();

GUIShopAPI.setExternalWorthCheck(player -> {
    synchronized (disabledPlayers) {
        return disabledPlayers.contains(player.getUniqueId());
    }
});
```

## Best Practices

1. **Always check if GUIShop is enabled** before calling API methods
2. **Use try-catch** around API calls in case GUIShop is not available
3. **Remove your hook** in `onDisable()` to prevent memory leaks
4. **Make your predicate fast** since it's called on every packet
5. **Cache results** if your check involves database queries
