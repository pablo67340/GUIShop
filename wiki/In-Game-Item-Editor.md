# In-Game Item Editor

GUIShop features a powerful, fully GUI-based item editor that eliminates the need for manual config editing. Configure every aspect of shop items through intuitive click-based menus.

## Entering Creator Mode

Use one of these commands to enter creator mode:

| Command | Description |
|---------|-------------|
| `/gs edit` | Edit the main menu |
| `/gs edit menu` | Edit the main menu |
| `/gs edit <shop>` | Edit a specific shop |
| `/gs edit <shop> <page>` | Edit a specific page |

**Permission required:** `guishop.creator`

## How It Works

### Creator Mode Controls

| Action | Result |
|--------|--------|
| **Left-click** | Pick up / place items (works between shop and your inventory) |
| **Right-click** | Open Item Editor for the clicked item |
| **Shift+click** | Open Item Editor for the clicked item |

### Moving Items Around

Once in creator mode:
- **Left-click** items to pick them up and move them around
- Drop items into empty slots to place them
- Drag items from your player inventory into the shop to add new items
- Remove items by dragging them back to your inventory
- Navigation buttons (forward, back, return to menu) still function normally
- All changes are saved automatically when you close the inventory

### Opening the Item Editor

- **Right-click** any item to open its Item Editor GUI
- **Shift+click** also opens the Item Editor
- The editor shows all configurable properties for that item

### Saving Changes

- Changes are saved automatically when you close the shop/menu
- After editing an item in the Item Editor, click "Save and Return" to apply changes
- No need to run `/gs reload` - changes take effect immediately

## Item Editor Features

When you open the Item Editor GUI, you'll see icons for each configurable property:

### Basic Properties

| Setting | Description |
|---------|-------------|
| **Buy Price** | Price players pay to purchase (supports 1k, 1.5M, 100B formats) |
| **Sell Price** | Price players receive when selling |
| **Item Type** | SHOP (buyable), COMMAND (runs commands), or DUMMY (decoration) |
| **Quantity** | Stack size given on purchase |

### Display Properties

| Setting | Description |
|---------|-------------|
| **Shop Name** | Display name shown in the shop GUI |
| **Buy Name** | Name on the item after purchase |
| **Shop Lore** | Lore lines shown in shop |
| **Buy Lore** | Lore lines on purchased item |

### Advanced Properties

| Setting | Description |
|---------|-------------|
| **Enchantments** | Opens visual enchantment picker |
| **Potion Info** | Configure potion type, splash/lingering, extended/upgraded |
| **Firework Info** | Set flight duration, explosion effects, colors |
| **Mob Type** | For spawners - select entity type |
| **Commands** | Commands to run on purchase (COMMAND type only) |
| **Permission** | Required permission to buy |
| **Target Shop** | For menu items - which shop to open |

## Nested Editors

Some properties open additional GUIs for easier configuration:

### Enchantment Editor
- Browse all available enchantments
- Click to add/remove enchantments
- Set enchantment levels through chat input

### Potion Editor
- Select potion type from visual list
- Toggle splash/lingering
- Toggle extended/upgraded duration

### Firework Editor
- Set flight duration
- Configure multiple explosions
- Choose shapes, colors, and effects

## Chat Input

When modifying values like prices or names:
1. The GUI closes temporarily
2. Type your value in chat
3. Supports special formats:
   - Prices: `100`, `1.5k`, `2.5M`, `1B`, `1,000,000`
   - Names: Full color code support with `&`
   - Type `cancel` to cancel input

## Tips

### Quick Item Setup
1. Place a new item from your inventory into the shop
2. Right-click it to open the editor
3. Set buy/sell prices
4. The item type automatically changes from DUMMY to SHOP when prices are set

### Decoration Items
- Items without prices are automatically saved as DUMMY type
- DUMMY items have their name set to a single space by default
- Perfect for glass panes and other decoration

### Clearing Values
- Right-click on a setting in the Item Editor to clear/reset it
- Useful for removing prices, permissions, or other optional values

## Example Workflow

1. Run `/gs edit Blocks` to edit the Blocks shop
2. Drag a Diamond from your inventory into slot 10
3. Right-click the Diamond to open the editor
4. Click "Buy Price" and type `500` in chat
5. Click "Sell Price" and type `250` in chat
6. Click "Shop Name" and type `&bShiny Diamond` in chat
7. Click "Save and Return"
8. Close the inventory to save all changes

The diamond is now configured and purchasable!
