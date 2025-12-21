# API Documentation

GUIShop provides a comprehensive API for developers to integrate with. All API methods are available through the `GUIShopAPI` class.

## Getting Started

### Maven Dependency

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.pablo67340</groupId>
    <artifactId>GUIShop</artifactId>
    <version>VERSION</version>
    <scope>provided</scope>
</dependency>
```

### Soft Depend

Add GUIShop as a soft dependency in your `plugin.yml`:

```yaml
softdepend: [GUIShop]
```

### Checking if GUIShop is Available

```java
if (Bukkit.getPluginManager().isPluginEnabled("GUIShop")) {
    // GUIShop is available
}
```

## Core API Methods

### Checking if Items Can Be Sold/Bought

```java
import com.pablo67340.guishop.api.GUIShopAPI;

// Check if an item can be sold
boolean canSell = GUIShopAPI.canBeSold(itemStack);

// Check if an item can be bought
boolean canBuy = GUIShopAPI.canBeBought(itemStack);
```

### Getting Prices

```java
import java.math.BigDecimal;

// Get buy price for a quantity
BigDecimal buyPrice = GUIShopAPI.getBuyPrice(itemStack, quantity);
// Returns -1 if item cannot be bought

// Get sell price for a quantity
BigDecimal sellPrice = GUIShopAPI.getSellPrice(itemStack, quantity);
// Returns -1 if item cannot be sold

// Get worth of a single item
BigDecimal worth = GUIShopAPI.getItemWorth(itemStack);
// Returns null if not sellable

// Get worth of entire stack
BigDecimal stackWorth = GUIShopAPI.getStackWorth(itemStack);
// Returns null if not sellable
```

### Selling Items Programmatically

```java
import com.pablo67340.guishop.definition.SellType;

// Sell items for a player
GUIShopAPI.sellItems(player, SellType.COMMAND, itemStacks);
```

**SellType options:**
- `SellType.SELL_GUI` - Items from the sell GUI
- `SellType.COMMAND` - Items sold via command
- `SellType.DIRECT` - Direct API sale

**Note:** This method:
- Sums the total sale price
- Gives the money to the player
- Returns unsellable items to the player's inventory
- Does NOT remove items from the player's inventory (caller must handle this)

### Dynamic Pricing Integration

If using dynamic pricing, notify GUIShop of external transactions:

```java
// Indicate items were bought externally
GUIShopAPI.indicateBoughtItems(itemStack, quantity);

// Indicate items were sold externally
GUIShopAPI.indicateSoldItems(itemStack, quantity);
```

This ensures the dynamic pricing system stays in sync.

## Worth Display API

See [Worth Display API](Worth-Display-API) for detailed documentation on:
- Checking if worth display is enabled
- Per-player worth toggling
- External plugin hooks for persistent settings

## Complete Example

```java
import com.pablo67340.guishop.api.GUIShopAPI;
import com.pablo67340.guishop.definition.SellType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;

public class ShopIntegration {

    public void sellPlayerInventory(Player player) {
        // Get all items from player inventory
        ItemStack[] contents = player.getInventory().getContents();
        
        // Filter to only sellable items
        List<ItemStack> sellable = new ArrayList<>();
        for (ItemStack item : contents) {
            if (item != null && GUIShopAPI.canBeSold(item)) {
                sellable.add(item.clone());
            }
        }
        
        if (sellable.isEmpty()) {
            player.sendMessage("No sellable items found!");
            return;
        }
        
        // Calculate total value
        BigDecimal total = BigDecimal.ZERO;
        for (ItemStack item : sellable) {
            BigDecimal worth = GUIShopAPI.getStackWorth(item);
            if (worth != null) {
                total = total.add(worth);
            }
        }
        
        // Sell the items
        GUIShopAPI.sellItems(player, SellType.COMMAND, 
            sellable.toArray(new ItemStack[0]));
        
        // Clear the inventory (since sellItems doesn't remove them)
        for (ItemStack item : sellable) {
            player.getInventory().remove(item);
        }
        
        player.sendMessage("Sold items for $" + total);
    }
    
    public BigDecimal getInventoryWorth(Player player) {
        BigDecimal total = BigDecimal.ZERO;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                BigDecimal worth = GUIShopAPI.getStackWorth(item);
                if (worth != null) {
                    total = total.add(worth);
                }
            }
        }
        
        return total;
    }
}
```

## Events

GUIShop does not currently fire custom events, but you can listen to standard Bukkit inventory events and check if the inventory belongs to GUIShop.

## Accessing Internal Classes

While the `GUIShopAPI` class provides the officially supported API, you can access internal classes if needed. However, internal APIs are subject to change without notice.

```java
import com.pablo67340.guishop.GUIShop;

// Get the main plugin instance
GUIShop plugin = GUIShop.getINSTANCE();

// Access the item table (all configured items)
Map<String, List<Item>> itemTable = plugin.getITEMTABLE();
```

**Warning:** Using internal APIs may break with updates. Prefer using `GUIShopAPI` methods.
