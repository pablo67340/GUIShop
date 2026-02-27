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

## Item-Specific Overrides

You can configure different price volatility settings for specific items in `dynamicpricing.yml`:

```yaml
item-overrides:
  DIAMOND:
    price-change-per-item: 0.005  # More stable (0.5% per item)
    max-price-multiplier: 1.5     # Max 150% of base
    min-price-multiplier: 0.75    # Min 75% of base
  
  DIRT:
    price-change-per-item: 0.02   # More volatile (2% per item)
    max-price-multiplier: 3.0     # Max 300% of base
    min-price-multiplier: 0.25    # Min 25% of base
```

| Override Key | Description |
|--------------|-------------|
| `price-change-per-item` | Price change per item for this specific item |
| `max-price-multiplier` | Maximum price multiplier for this item |
| `min-price-multiplier` | Minimum price multiplier for this item |

## Linked Pricing (affects)

One of the most powerful features is **linked pricing**, where buying or selling one item can affect the prices of related items. This creates realistic market relationships based on crafting recipes or resource chains.

### How It Works

When you buy or sell an item with linked pricing configured, the stock levels of related items are also adjusted based on the configured multiplier:

- **Multiplier of 9.0**: Buying 1 DIAMOND_BLOCK affects DIAMOND prices as if 9 diamonds were bought
- **Multiplier of 0.5**: The effect is halved
- **Multiplier of 1.0**: 1:1 effect

### Configuration Example

```yaml
item-overrides:
  # Diamond blocks affect diamond prices (crafting relationship)
  DIAMOND_BLOCK:
    price-change-per-item: 0.05
    affects:
      DIAMOND: 9.0           # 1 block = 9 diamonds in crafting
      DIAMOND_ORE: 1.0       # Also affects ore prices
  
  # Iron blocks affect iron-related items
  IRON_BLOCK:
    affects:
      IRON_INGOT: 9.0        # 1 block = 9 ingots
      RAW_IRON: 9.0          # Also affects raw iron
  
  # Food crafting relationships
  BREAD:
    affects:
      WHEAT: 3.0             # 1 bread = 3 wheat in crafting
  
  # Complex recipes
  CAKE:
    affects:
      WHEAT: 3.0
      SUGAR: 2.0
      EGG: 1.0
      MILK_BUCKET: 3.0
```

### Example Scenario: Linked Diamond Economy

With the configuration above:

1. Player buys 10 DIAMOND_BLOCK
2. DIAMOND_BLOCK stock decreases by 10 (price increases)
3. DIAMOND stock also decreases by 90 (10 × 9.0 multiplier)
4. DIAMOND_ORE stock decreases by 10 (10 × 1.0 multiplier)
5. All three items become more expensive!

This simulates real economic effects where buying processed materials also affects raw material prices.

### Use Cases

| Relationship Type | Example |
|-------------------|---------|
| **Crafting (compressed)** | DIAMOND_BLOCK → DIAMOND (×9) |
| **Smelting** | IRON_INGOT → RAW_IRON (×1) |
| **Brewing ingredients** | POTION → BLAZE_POWDER, NETHER_WART |
| **Food recipes** | CAKE → WHEAT, SUGAR, EGG |
| **Tool materials** | DIAMOND_PICKAXE → DIAMOND (×3), STICK (×2) |

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
