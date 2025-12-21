# Common Issues

This page covers common issues and their solutions.

## Economy Issues

### "No economy system found"

**Cause:** Vault is not detecting your economy plugin.

**Solutions:**
1. Ensure Vault is installed
2. Ensure your economy plugin is installed and enabled
3. Check that your economy plugin supports Vault
4. Check load order in `plugin.yml`

### Players can buy but money is not deducted

**Cause:** Economy plugin integration issue.

**Solutions:**
1. Check economy plugin console for errors
2. Ensure player has an account created
3. Try restarting the server

## Shop Issues

### Items not appearing in shop

**Causes and Solutions:**

1. **Invalid material ID**
   - Check spelling matches Bukkit material names
   - Use `/gs edit` to see valid materials

2. **Missing required properties**
   - Ensure `buy-price` and `sell-price` are set (or `false`)

3. **YAML formatting error**
   - Check indentation (use spaces, not tabs)
   - Ensure proper quoting of strings

4. **hide-non-buyable enabled**
   - Items with `buy-price: false` will be hidden if `hide-non-buyable: true`

### Clicking items does nothing

**Causes and Solutions:**

1. **No permission**
   - Check player has `guishop.use` and relevant item permissions

2. **Item type is DUMMY or BLANK**
   - These types are not purchasable

3. **Item has `buy-price: false`**
   - Cannot buy items with no buy price

## Worth Display Issues

### Worth not showing on items

**Causes and Solutions:**

1. **PacketEvents not installed**
   - Install PacketEvents plugin

2. **Worth display disabled**
   - Check `enabled: true` in `worth.yml`

3. **Player toggled it off**
   - Use `/gs toggleworth` to re-enable

4. **Item is blacklisted**
   - Check `blacklisted-item-names` in `worth.yml`

5. **Inventory is blacklisted**
   - Check `blacklisted-inventories` in `worth.yml`

### Worth showing incorrectly

**Solutions:**
1. Check `format` setting in `worth.yml`
2. Ensure item has a sell price in `shops.yml`
3. Enable debug mode: `debug: true` in `worth.yml`

### Worth duplicating or stacking incorrectly

**Cause:** Usually indicates a conflict with another plugin modifying item lore.

**Solutions:**
1. Check for conflicting plugins
2. Ensure PacketEvents is up to date
3. Add conflicting lore patterns to `ignore-lore-containing`

## Command Issues

### Commands not working

**Causes and Solutions:**

1. **Command conflicts**
   - Another plugin has same command
   - Change `commands-mode` to `INTERCEPT` in config
   - Or rename commands in config

2. **No permission**
   - Check player has required permission

3. **Commands not registered**
   - If using `NONE` mode, configure in Bukkit's `commands.yml`

### Tab completion not working

**Solutions:**
1. Use `commands-mode: 'REGISTER'` in config
2. Restart server after changing command mode

## Configuration Issues

### Changes not applying after reload

**Solutions:**
1. Check for YAML syntax errors
2. Look for console errors
3. Some changes require full restart

### YAML parse errors

**Common causes:**
1. Using tabs instead of spaces
2. Missing quotes around special characters
3. Incorrect indentation
4. Missing colons after keys

**Use a YAML validator** to check your files.

## Performance Issues

### Lag when opening shops

**Solutions:**
1. Reduce number of items per page
2. Disable debug mode
3. Check for slow economy plugin

### Worth display causing lag

**Solutions:**
1. Reduce `blacklisted-inventories` list
2. Disable debug mode in `worth.yml`
3. Check PacketEvents version compatibility

## Debug Mode

Enable debug mode to diagnose issues:

```yaml
# In config.yml
debug-mode: true

# In worth.yml
debug: true
```

This will log detailed information to the console.

## Getting Help

If you cannot resolve your issue:

1. **Check the console** for error messages
2. **Enable debug mode** and reproduce the issue
3. **Collect relevant config files**
4. **Post on Discord** or GitHub with:
   - Server version
   - Plugin version
   - Error messages
   - Steps to reproduce
