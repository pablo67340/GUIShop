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

## Dynamic Pricing API

GUIShop includes a built-in supply/demand system. You can query and control it programmatically.

### Accessing the Dynamic Pricing Manager

```java
import com.pablo67340.guishop.economy.DynamicPricingManager;

DynamicPricingManager dpManager = DynamicPricingManager.getInstance();

// Check if dynamic pricing is active
if (dpManager != null && dpManager.isInitialized()) {
    // Dynamic pricing is active
}
```

### Querying Market Data

```java
// Get current stock level for an item
// Positive = oversupply, Negative = undersupply, 0 = equilibrium
int stockLevel = dpManager.getStockLevel("DIAMOND");

// Get current price multipliers (1.0 = base price)
double buyMultiplier = dpManager.getBuyMultiplier("DIAMOND");   // e.g., 1.5 = 150%
double sellMultiplier = dpManager.getSellMultiplier("DIAMOND"); // e.g., 0.8 = 80%
```

### Admin Operations

```java
// Reset a single item to base price
dpManager.resetItem("DIAMOND");

// Reset all items to base prices
dpManager.resetAll();
```

### Custom Price Provider

You can replace GUIShop's built-in dynamic pricing with your own implementation:

```java
import com.pablo67340.guishop.api.DynamicPriceProvider;
import java.math.BigDecimal;

public class MyPriceProvider implements DynamicPriceProvider {
    
    @Override
    public BigDecimal calculateBuyPrice(String item, int quantity, 
            BigDecimal staticBuyPrice, BigDecimal staticSellPrice) {
        // Your custom buy price calculation
        // Return the total price for the given quantity
        return staticBuyPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    @Override
    public BigDecimal calculateSellPrice(String item, int quantity,
            BigDecimal staticBuyPrice, BigDecimal staticSellPrice) {
        // Your custom sell price calculation
        return staticSellPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    @Override
    public void buyItem(String item, int quantity) {
        // Called when items are purchased from the shop
        // Update your internal economy state
    }
    
    @Override
    public void sellItem(String item, int quantity) {
        // Called when items are sold to the shop
        // Update your internal economy state
    }
}
```

Register your provider with Bukkit's service manager:

```java
import org.bukkit.plugin.ServicePriority;

@Override
public void onEnable() {
    getServer().getServicesManager().register(
        DynamicPriceProvider.class,
        new MyPriceProvider(),
        this,
        ServicePriority.High
    );
}
```

When GUIShop starts, it will detect your provider and use it instead of the built-in system.

See [Dynamic Pricing](Dynamic-Pricing) for full documentation.

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
