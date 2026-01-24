# Dynamic Pricing

GUIShop includes a built-in dynamic pricing system that simulates supply and demand economics. When enabled, item prices fluctuate based on server-wide trading activity.

## How It Works

- **When items are bought**: Supply decreases → prices increase
- **When items are sold**: Supply increases → prices decrease  
- **Over time**: Prices gradually normalize back to base values (market equilibrium)

This creates a dynamic economy where:
- Popular items become more expensive
- Oversupplied items become cheaper
- The market naturally balances itself over time

## Enabling Dynamic Pricing

### config.yml
```yaml
dynamic-pricing: true
```

### dynamicpricing.yml
All dynamic pricing settings are configured in `dynamicpricing.yml`:

```yaml
# Enable or disable dynamic pricing
enabled: false

# How much the price changes per item (0.01 = 1%)
price-change-per-item: 0.01

# Maximum price multiplier (2.0 = up to 200% of base price)
max-price-multiplier: 2.0

# Minimum price multiplier (0.5 = as low as 50% of base price)
min-price-multiplier: 0.5

# How quickly prices return to normal (0.001 = 0.1% per tick)
normalization-rate: 0.001

# How often normalization runs (in seconds)
normalization-interval: 300
```

## Configuration Options

| Option | Default | Description |
|--------|---------|-------------|
| `price-change-per-item` | 0.01 | Price change per item traded (1% = 0.01) |
| `max-price-multiplier` | 2.0 | Maximum price can reach 200% of base |
| `min-price-multiplier` | 0.5 | Minimum price can drop to 50% of base |
| `normalization-rate` | 0.001 | How fast prices return to normal |
| `normalization-interval` | 300 | Seconds between normalization ticks |

## Admin Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/gs market status` | `guishop.admin` | Show dynamic pricing system status |
| `/gs market info <item>` | `guishop.admin` | Check an item's current market status |
| `/gs market reset <item>` | `guishop.admin` | Reset an item to base price |
| `/gs market resetall` | `guishop.admin` | Reset all items to base prices |

### Market Info Example

```
/gs market info DIAMOND

=== Market Info: DIAMOND ===
Stock Level: -50 (undersupply)
Buy Price Multiplier: 150% (more expensive)
Sell Price Multiplier: 150% (more expensive)
Status: High demand (undersupply)
```

## Per-Item Configuration

When dynamic pricing is enabled globally, all items use dynamic pricing by default. You can exempt specific items from dynamic pricing by setting `dynamic: false`:

```yaml
# In your shop .yml file
'0':
  id: DIAMOND
  buy-price: 100
  sell-price: 50
  # Dynamic pricing is inherited (enabled by default when global is on)

'1':
  id: GOLD_INGOT
  buy-price: 50
  sell-price: 25
  dynamic: false  # This item uses static pricing even with dynamic pricing enabled

```

**Note:** You only need to add `dynamic: false` to items you want to exempt. Items without this key automatically use dynamic pricing when it's globally enabled.

## Data Storage

Dynamic pricing data is stored in `plugins/GUIShop/Data/dynamic_pricing.db` (SQLite database).

This includes:
- Current stock levels for all items
- Total items bought/sold (historical data)
- Last update timestamps

## API Integration

### Using the Built-in System

External plugins can query the dynamic pricing system:

```java
import com.pablo67340.guishop.economy.DynamicPricingManager;

DynamicPricingManager dpManager = DynamicPricingManager.getInstance();

// Get current stock level (-50 = undersupply, +50 = oversupply)
int stock = dpManager.getStockLevel("DIAMOND");

// Get current price multipliers
double buyMult = dpManager.getBuyMultiplier("DIAMOND");   // e.g., 1.5 = 150%
double sellMult = dpManager.getSellMultiplier("DIAMOND"); // e.g., 1.5 = 150%

// Reset an item to base price
dpManager.resetItem("DIAMOND");
```

### Custom Price Provider

You can replace GUIShop's built-in system with your own by implementing `DynamicPriceProvider`:

```java
import com.pablo67340.guishop.api.DynamicPriceProvider;
import java.math.BigDecimal;

public class MyPriceProvider implements DynamicPriceProvider {
    
    @Override
    public BigDecimal calculateBuyPrice(String item, int quantity, 
            BigDecimal staticBuyPrice, BigDecimal staticSellPrice) {
        // Your custom buy price logic
        return staticBuyPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    @Override
    public BigDecimal calculateSellPrice(String item, int quantity,
            BigDecimal staticBuyPrice, BigDecimal staticSellPrice) {
        // Your custom sell price logic
        return staticSellPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    @Override
    public void buyItem(String item, int quantity) {
        // Called when items are purchased
    }
    
    @Override
    public void sellItem(String item, int quantity) {
        // Called when items are sold
    }
}
```

Register your provider with Bukkit's service manager:

```java
getServer().getServicesManager().register(
    DynamicPriceProvider.class,
    new MyPriceProvider(),
    myPlugin,
    ServicePriority.High
);
```

GUIShop will automatically use your provider instead of the built-in system.

## Example Economy Scenarios

### Scenario 1: Diamond Rush
1. Players buy lots of diamonds
2. Stock level drops (undersupply: -100)
3. Diamond buy price increases to 200% of base
4. Diamond sell price also increases (good time to sell!)
5. Eventually, prices normalize back to 100%

### Scenario 2: Cobblestone Overflow
1. Players sell lots of cobblestone
2. Stock level rises (oversupply: +500)
3. Cobblestone prices drop to 50% of base
4. Fewer players sell cobblestone
5. Prices gradually return to normal

### Scenario 3: Balanced Market
1. Equal buying and selling activity
2. Stock level stays near 0
3. Prices remain close to base values
4. Natural market equilibrium
