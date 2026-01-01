# Statistics & PlaceholderAPI

GUIShop includes a built-in statistics system that tracks player shop transactions. Statistics are stored in an SQLite database and are accessible via the Java API or PlaceholderAPI.

## Overview

The statistics system tracks:
- **Total money spent** - Money spent buying items from shops
- **Total money earned** - Money earned from selling items
- **Total items bought** - Quantity of items purchased
- **Total items sold** - Quantity of items sold
- **Top 3 bought items** - Most frequently purchased items per player
- **Top 3 sold items** - Most frequently sold items per player

## Database Location

Statistics are stored in:
```
plugins/GUIShop/Data/player_statistics.db
```

## PlaceholderAPI Placeholders

All placeholders use the `guishop` prefix.

### Money Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%guishop_total_spent%` | Total money spent (with commas) | `1,234,567.89` |
| `%guishop_total_spent_formatted%` | Total spent (abbreviated) | `1.23M` |
| `%guishop_total_spent_raw%` | Total spent (raw number) | `1234567.89` |
| `%guishop_total_earned%` | Total money earned (with commas) | `987,654.32` |
| `%guishop_total_earned_formatted%` | Total earned (abbreviated) | `987.65K` |
| `%guishop_total_earned_raw%` | Total earned (raw number) | `987654.32` |

### Item Count Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%guishop_items_bought%` | Total items bought (with commas) | `15,432` |
| `%guishop_items_bought_formatted%` | Items bought (abbreviated) | `15.4K` |
| `%guishop_items_bought_raw%` | Items bought (raw number) | `15432` |
| `%guishop_items_sold%` | Total items sold (with commas) | `8,765` |
| `%guishop_items_sold_formatted%` | Items sold (abbreviated) | `8.77K` |
| `%guishop_items_sold_raw%` | Items sold (raw number) | `8765` |

### Top Bought Items

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%guishop_top_bought_1%` | #1 most bought item | `Diamond Sword: 150` |
| `%guishop_top_bought_2%` | #2 most bought item | `Golden Apple: 89` |
| `%guishop_top_bought_3%` | #3 most bought item | `Iron Block: 64` |
| `%guishop_top_bought_1_item%` | #1 item name only | `Diamond Sword` |
| `%guishop_top_bought_1_qty%` | #1 quantity only | `150` |
| `%guishop_top_bought_1_qty_formatted%` | #1 quantity (abbreviated) | `150` |

### Top Sold Items

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%guishop_top_sold_1%` | #1 most sold item | `Cobblestone: 10,000` |
| `%guishop_top_sold_2%` | #2 most sold item | `Iron Ore: 5,432` |
| `%guishop_top_sold_3%` | #3 most sold item | `Diamond: 234` |
| `%guishop_top_sold_1_item%` | #1 item name only | `Cobblestone` |
| `%guishop_top_sold_1_qty%` | #1 quantity only | `10,000` |
| `%guishop_top_sold_1_qty_formatted%` | #1 quantity (abbreviated) | `10K` |

## Example Usage

### Scoreboard
```yaml
lines:
  - "&7Money Spent: &a$%guishop_total_spent_formatted%"
  - "&7Money Earned: &a$%guishop_total_earned_formatted%"
  - "&7Items Bought: &e%guishop_items_bought%"
  - "&7Items Sold: &e%guishop_items_sold%"
```

### Hologram
```yaml
lines:
  - "&6Your Shop Stats"
  - "&7Spent: &a$%guishop_total_spent_formatted%"
  - "&7Earned: &a$%guishop_total_earned_formatted%"
  - "&7Top Item: &b%guishop_top_sold_1%"
```

### Chat Format
```
&7[&aShop&7] You've spent &a$%guishop_total_spent_formatted% &7in shops!
```

## Java API

Access statistics programmatically through the `GUIShopAPI` class:

```java
import com.pablo67340.guishop.api.GUIShopAPI;
import com.pablo67340.guishop.statistics.PlayerStats;
import org.bukkit.entity.Player;
import java.math.BigDecimal;

// Check if statistics are enabled
if (GUIShopAPI.isStatisticsEnabled()) {
    
    // Get player stats
    PlayerStats stats = GUIShopAPI.getPlayerStats(player);
    
    // Get individual values
    BigDecimal totalSpent = GUIShopAPI.getTotalSpent(player);
    BigDecimal totalEarned = GUIShopAPI.getTotalEarned(player);
    int itemsBought = GUIShopAPI.getItemsBought(player);
    int itemsSold = GUIShopAPI.getItemsSold(player);
    
    // Get formatted strings
    String spentFormatted = GUIShopAPI.getTotalSpentFormatted(player, true);  // "1.5M"
    String spentCommas = GUIShopAPI.getTotalSpentFormatted(player, false);    // "1,500,000"
    
    // Get top items
    String topSold = stats.getTopItemMaterial(1, false);  // "COBBLESTONE"
    Integer topSoldQty = stats.getTopItemQuantity(1, false);  // 10000
}
```

### Leaderboards

```java
// Get top 10 spenders
List<Map.Entry<UUID, BigDecimal>> topSpenders = GUIShopAPI.getTopSpenders(10);

for (Map.Entry<UUID, BigDecimal> entry : topSpenders) {
    UUID uuid = entry.getKey();
    BigDecimal amount = entry.getValue();
    // Display on leaderboard...
}

// Get top 10 earners
List<Map.Entry<UUID, BigDecimal>> topEarners = GUIShopAPI.getTopEarners(10);
```

### Reset Statistics

```java
// Reset a player's statistics
GUIShopAPI.resetPlayerStats(player);
```

## Notes

- Statistics are saved asynchronously to prevent lag
- Player stats are cached while online for fast access
- The database is automatically created on first run
- Statistics persist across server restarts
- Offline player stats can be queried using their UUID
