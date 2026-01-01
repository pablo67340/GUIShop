# Internal Economy System

GUIShop includes a built-in economy system that can replace external economy plugins like EssentialsX Economy. When enabled, it registers with Vault, allowing all Vault-compatible plugins to use it.

## Enabling the Internal Economy

1. Open `plugins/GUIShop/economy.yml`
2. Set `enabled: true`
3. Restart your server (or reload GUIShop)

```yaml
# economy.yml
enabled: true
```

When enabled, you'll see this message in console:
```
[GUIShop] Internal economy enabled and registered with Vault.
[GUIShop] Currency: $ (Dollar)
[GUIShop] Starting balance: $1,000.00
```

## Configuration (economy.yml)

### Currency Settings

```yaml
currency:
  # Name of the currency (singular)
  name: "Dollar"
  
  # Name of the currency (plural)
  name-plural: "Dollars"
  
  # Currency symbol
  symbol: "$"
  
  # Whether the symbol comes before the amount
  # true = $100, false = 100$
  symbol-prefix: true
```

### Number Formatting

```yaml
formatting:
  # Number of decimal places to display
  decimal-places: 2
  
  # Whether to use thousands separators (e.g., 1,000,000)
  use-thousands-separator: true
  
  # Character to use as thousands separator
  thousands-separator: ","
  
  # Character to use as decimal separator
  decimal-separator: "."
  
  # Whether to abbreviate large numbers (e.g., 1.5M, 100K)
  abbreviate-large-numbers: true
  
  # Format for displaying balance
  # Available: %symbol%, %amount%, %currency%, %currency_plural%
  balance-format: "%symbol%%amount%"
```

### Balance Settings

```yaml
balance:
  # Starting balance for new players
  starting-balance: 1000.0
  
  # Whether players can have negative balances
  allow-negative: false
  
  # Minimum balance allowed (only if allow-negative is true)
  minimum-balance: -10000.0
  
  # Maximum balance allowed
  maximum-balance: 1000000000000.0
```

### Command Settings

```yaml
commands:
  # Enable /bal and /balance commands
  balance-enabled: true
  
  # Enable /pay command
  pay-enabled: true
  
  # Minimum amount that can be sent with /pay
  pay-minimum: 1.0
  
  # Maximum amount that can be sent with /pay (0 = unlimited)
  pay-maximum: 0
  
  # Allow paying offline players
  pay-offline-players: false
```

### Messages

All economy messages are fully customizable in `economy.yml`:

```yaml
messages:
  balance-self: "&7Your balance: &a%balance%"
  balance-other: "&7%player%'s balance: &a%balance%"
  pay-sent: "&aYou sent %amount% to %player%."
  pay-received: "&aYou received %amount% from %player%."
  pay-self-error: "&cYou cannot pay yourself!"
  pay-insufficient: "&cInsufficient funds. You have %balance%."
  pay-minimum-error: "&cMinimum payment amount is %minimum%."
  pay-maximum-error: "&cMaximum payment amount is %maximum%."
  pay-player-not-found: "&cPlayer '%player%' not found."
  pay-invalid-amount: "&cInvalid amount: %amount%"
  economy-disabled: "&cEconomy system is not enabled."
```

## Player Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/bal`, `/balance`, `/money` | Check your own balance | `guishop.economy.balance` |
| `/bal <player>` | Check another player's balance | `guishop.economy.balance.others` |
| `/pay <player> <amount>` | Send money to another player | `guishop.economy.pay` |

## Admin Commands

All admin commands can be run from console or in-game.

| Command | Description |
|---------|-------------|
| `/gs eco give <player> <amount>` | Give money to a player |
| `/gs eco take <player> <amount>` | Take money from a player |
| `/gs eco set <player> <amount>` | Set a player's balance |
| `/gs eco balance <player>` | Check a player's balance |
| `/gs eco reset <player>` | Reset to starting balance |

### Amount Abbreviations

Commands support abbreviated amounts:
- `1k` = 1,000
- `1.5k` = 1,500
- `100k` = 100,000
- `1m` or `1M` = 1,000,000
- `1.5m` = 1,500,000
- `1b` or `1B` = 1,000,000,000
- `1t` or `1T` = 1,000,000,000,000

**Examples:**
```
/gs eco give Steve 1.5k     # Gives $1,500
/gs eco give Steve 100000   # Gives $100,000
/gs eco set Steve 1M        # Sets balance to $1,000,000
/gs eco take Steve 500      # Takes $500
```

## Database Storage

Balances are stored in SQLite:
```
plugins/GUIShop/Data/player_balances.db
```

The database stores:
- Player UUID
- Username (updated on login)
- Current balance
- Last seen timestamp

## Negative Balance Support

When `allow-negative: true`:
- Players can go into debt up to `minimum-balance`
- Useful for loan systems or penalty mechanics
- Transactions that would exceed the minimum are blocked

When `allow-negative: false` (default):
- Players cannot go below $0
- Withdrawals that would result in negative are blocked

## Maximum Balance

The `maximum-balance` setting prevents players from exceeding a certain amount:
- Default is 1 trillion (1,000,000,000,000)
- Deposits that would exceed this are capped
- Useful for preventing economy overflow

## Vault Integration

Once enabled, any Vault-compatible plugin can use the GUIShop economy:
- Other shop plugins
- Auction plugins
- Job plugins
- Crate plugins
- And more

GUIShop registers with `ServicePriority.High`, so it will be preferred over lower-priority economy plugins.

## Example Configurations

### Basic Server Economy
```yaml
enabled: true
currency:
  name: "Coin"
  name-plural: "Coins"
  symbol: "⛃"
  symbol-prefix: true
formatting:
  decimal-places: 0
  abbreviate-large-numbers: true
balance:
  starting-balance: 500
  allow-negative: false
  maximum-balance: 100000000
```

### Realistic Economy with Debt
```yaml
enabled: true
currency:
  name: "Dollar"
  name-plural: "Dollars"
  symbol: "$"
  symbol-prefix: true
formatting:
  decimal-places: 2
  use-thousands-separator: true
balance:
  starting-balance: 1000
  allow-negative: true
  minimum-balance: -5000
  maximum-balance: 1000000000
```

### European Format
```yaml
enabled: true
currency:
  name: "Euro"
  name-plural: "Euros"
  symbol: "€"
  symbol-prefix: false
formatting:
  decimal-places: 2
  thousands-separator: "."
  decimal-separator: ","
  balance-format: "%amount% %symbol%"
```

## Migrating from Another Economy

If you're switching from EssentialsX or another economy plugin:

1. Use a plugin like [Economy Shop GUI](https://www.spigotmc.org/resources/economyshopgui.69927/) to export balances, or manually set balances
2. Enable GUIShop's internal economy
3. Remove the old economy plugin
4. Use `/gs eco set <player> <amount>` to restore balances

**Note:** There is no automatic migration tool. Back up your data before switching.
