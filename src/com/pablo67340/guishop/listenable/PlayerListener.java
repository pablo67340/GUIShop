package com.pablo67340.guishop.listenable;

import com.github.stefvanschie.inventoryframework.shade.nbtapi.NBTCompound;
import com.github.stefvanschie.inventoryframework.shade.nbtapi.NBTItem;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.Item;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class PlayerListener implements Listener {

    /**
     * An instance of a {@link PlayerListener} that will be used to handle this
     * specific object reference from other classes, even though methods here
     * will be static.
     */
    public static final PlayerListener INSTANCE = new PlayerListener();

    private final String[] commandsEntryList = {
            "reload",
            "parsemob",
            "edit",
            "buy-price",
            "sell-price",
            "shop-name",
            "buy-name",
            "name",
            "enchant",
            "add-shop-lore",
            "edit-shop-lore",
            "delete-shop-lore",
            "add-buy-lore",
            "edit-buy-lore",
            "delete-buy-lore",
            "add-lore",
            "edit-lore",
            "delete-lore",
            "type",
            "add-command",
            "edit-command",
            "delete-command",
            "mob-type",
            "target-shop",
            "nbt",
            "printnbt",
            "list-shops",
            "list-commands",
            "potion-info",
            "disable-quantity",
            "skull-uuid",
            "value"};

    public Menu openMenu(Player player) {
        Menu menu = new Menu(player);
        menu.open(player);
        return menu;
    }

    /**
     * Print the usage of the plugin to the player.
     *
     * @param sender The player the help text will be sent to
     */
    public void printUsage(CommandSender sender) {
        GUIShop.sendMessagePrefix(sender, String.join("\n", GUIShop.getINSTANCE().getMessagesConfig().getStringList("messages.list"))
                .replace("%list%", Arrays.stream(commandsEntryList).map(entry -> GUIShop.getINSTANCE().messageSystem.translate("messages." + entry + ".entry"))
                        .collect(Collectors.joining("\n"))));
    }

    // When the player clicks a sign
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Block block = e.getClickedBlock();

        // If the block exists
        if (block != null) {
            // If the block state is a Sign
            if (block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();
                String line1 = ChatColor.translateAlternateColorCodes('&', sign.getLine(0));
                // Check if the sign is a GUIShop sign
                if (line1.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&',
                        Config.getTitlesConfig().getSignTitle()))) {
                    // If the player has Permission to use sign
                    if (player.hasPermission("guishop.use") && player.hasPermission("guishop.sign.use")
                            || player.isOp()) {
                        e.setCancelled(true);
                        Menu menu = new Menu(player);
                        menu.open(player);
                    } else {
                        e.setCancelled(true);
                        GUIShop.sendPrefix(player, "no-permission");
                    }
                }
            }
        }
    }

    /**
     * Custom MobSpawner placement method.
     *
     * @param event The event type we're listening to
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (Item.isSpawnerItem(item)) {
            NBTCompound cmp = new NBTItem(item);
            if (cmp.hasKey("GUIShopSpawner")) {
                BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                scheduler.scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> {
                    String mobId = cmp.getString("GUIShopSpawner");
                    Block block = event.getBlockPlaced();
                    CreatureSpawner cs = (CreatureSpawner) block.getState();

                    GUIShop.debugLog("Applying mob type " + mobId);

                    /*
                     * Although valueOf is almost always safe here because
                     * we used EntityType.name() when setting the NBT tag,
                     * it's possible the user might change server versions,
                     * in which case the EntityType enum may have changed.
                     */
                    try {
                        cs.setSpawnedType(EntityType.valueOf(mobId));
                        cs.update();
                    } catch (IllegalArgumentException veryRareException) {
                        GUIShop.log("Detected outdated mob spawner ID: " + mobId + " placed by " + event.getPlayer());
                    }
                }, 1L);
            }
        }
    }
}
