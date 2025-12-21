# Worth Display Configuration

The Worth Display system shows item sell values directly in item lore. This is a client-side only feature that uses packet manipulation, so your actual items are never modified.

## Requirements

- **PacketEvents** plugin must be installed

If PacketEvents is not present, the worth display feature will be automatically disabled.

## Configuration File

All worth display settings are in `worth.yml`:

```yaml
# Enable or disable the worth display feature
enabled: true

# The format for the worth line added to item lore
# Placeholders:
#   %worth% - The total sell value of the stack
#   %worth_single% - The sell value of a single item
#   %amount% - The stack size
#   %currency_prefix% - Currency prefix from messages.yml
#   %currency_suffix% - Currency suffix from messages.yml
format: "&7Worth: &a%worth%"

# Where to add the worth line in the lore
# Options: TOP, BOTTOM
position: BOTTOM

# Add a blank line before the worth line for visual separation
add-blank-line: true

# Only show worth if the item has a sell price > 0
# If false, items without a sell price will show "Not sellable"
only-show-sellable: true

# Text to show for items that cannot be sold
not-sellable-format: "&7Worth: &cNot sellable"

# Ignore items that already have lore containing these strings
ignore-lore-containing:
  - "Worth:"
  - "Sell Value:"

# Blacklist inventory titles where worth should NOT be displayed
# GUIShop's own inventories are automatically excluded
blacklisted-inventories:
  - "Auction House"
  - "Crate Preview"

# Blacklist item display names
# Items with names containing these strings will NOT show worth
blacklisted-item-names:
  - "Crate Key"
  - "Vote Token"

# If true, worth will ONLY show in the player's own inventory
player-inventory-only: false

# If true, items in armor slots will NOT show worth
hide-armor-slots: true

# Debug mode for troubleshooting
debug: false
```

## Format Placeholders

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%worth%` | Total stack value | `$640.00` |
| `%worth_single%` | Single item value | `$10.00` |
| `%amount%` | Stack size | `64` |
| `%currency_prefix%` | Currency prefix | `$` |
| `%currency_suffix%` | Currency suffix | `` |

### Example Formats

**Simple:**
```yaml
format: "&7Worth: &a%worth%"
# Result: Worth: $640.00
```

**Detailed:**
```yaml
format: "&7Worth: &a%worth% &7(&a%worth_single%&7 each)"
# Result: Worth: $640.00 ($10.00 each)
```

**With amount:**
```yaml
format: "&7%amount%x = &a%worth%"
# Result: 64x = $640.00
```

## Blacklisting

### Inventory Blacklist

Prevent worth from showing in specific GUIs:

```yaml
blacklisted-inventories:
  - "Auction House"
  - "Crate Preview"
  - "Player Shop"
```

Uses partial matching (case-insensitive).

### Item Name Blacklist

Prevent worth from showing on specific items:

```yaml
blacklisted-item-names:
  - "Crate Key"
  - "Vote Token"
  - "Special"
```

Uses partial matching (case-insensitive).

## Armor Slot Hiding

When `hide-armor-slots: true`, items in equipped armor slots will not show worth. When the armor is moved to the regular inventory, worth will appear.

This is useful to keep your equipped armor's lore clean.

## Per-Player Toggle

Players can toggle worth display for themselves using:
```
/gs toggleworth
```

This is session-only and resets on server restart.

For persistent per-player settings, use the [Worth Display API](Worth-Display-API).

## Debugging

Enable debug mode to see worth calculations in the console:

```yaml
debug: true
```

This will log:
- SET_SLOT packets processed
- WINDOW_ITEMS packets processed
- Item worth calculations
- Blacklist matches

## How It Works

The worth display system works by:

1. Intercepting outgoing inventory packets (SET_SLOT, WINDOW_ITEMS)
2. Stripping any existing worth lore from items
3. Calculating the current worth based on sell price and stack size
4. Adding fresh worth lore to the packet
5. Sending the modified packet to the client

**Important:** This is entirely client-side. The actual items on the server are never modified, ensuring:
- Items remain stackable
- No NBT conflicts
- No duplication issues
- No data persistence problems
