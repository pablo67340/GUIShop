package com.pablo67340.guishop.autosellstuff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.listenable.Sell;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SellWand implements CommandExecutor, TabCompleter, Listener {

    private final Main main;

    public SellWand(Main plugin) {
        this.main = plugin;

        Objects.requireNonNull(plugin.getCommand("sellwand")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("sellwand")).setTabCompleter(this);

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("sellwand")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
                if (args.length > 1) {
                    if (sender.hasPermission("sellwand.command")) {
                        Player p = Bukkit.getPlayer(args[1]);

                        ItemStack sellWand = new ItemStack(Material.BLAZE_ROD, 1);
                        ItemMeta im = sellWand.getItemMeta();

                        if (im != null) {
                            im.setCustomModelData(330);
                            List<String> lore = new ArrayList<>();
                            lore.add(ChatColor.DARK_AQUA + "Sell wand - sells stuff in chests");
                            im.setLore(lore);
                            sellWand.setItemMeta(im);
                        } else {
                            main.getLogger().warning("[Sell Wand] item was null!");
                            return false;
                        }

                        if (p != null) {
                            p.getInventory().addItem(sellWand);
                        } else {
                            main.getLogger().warning("[Sell Wand] player was null!");
                            return false;
                        }
                        main.getLogger().info("[Sell Wand] Gave " + p.getName()
                                + " a Sell Wand!");
                    } else {
                        sender.sendMessage("You do not have permission to give yourself a " +
                                "Sell Wand!");
                    }
                }
            }
        }
        return false;
    }


    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("sellwand")) {
            if (args.length == 1) return Collections.singletonList("give");
            return sender.getServer().getOnlinePlayers().stream().map(CommandSender::getName)
                    .collect(Collectors.toList());
        } else return null;
    }

    public static void log(String o) {
        if (SellChest.econLogWriter != null) {
            try {
                SellChest.econLogWriter.write(o);
                SellChest.econLogWriter.newLine();
                SellChest.econLogWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSellWandClick(PlayerInteractEvent e) {
        if (e.useInteractedBlock() == Event.Result.DENY) return; // respect claims

        Player player = e.getPlayer();
        ItemStack item = e.getItem();

        if (item != null && item.getType() == Material.BLAZE_ROD && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();

            // if it's a blaze rod and we be clicking on a chest
            if (meta != null && meta.getCustomModelData() == 330) {
                Block block = e.getClickedBlock();
                if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
                    // try to sell the stuff in the chest
                    if (!(block.getState() instanceof Container)) return;
                    Inventory inv = ((Container) block.getState()).getInventory();

                    List<ItemStack> stuffToPrint = new ArrayList<>();
                    for (ItemStack i : inv.getContents()) {
                        if (i != null) stuffToPrint.add(i);
                    }

                    log("[" + SellChest.format.format(new Date()) + "] [" + player.getName() +
                            "] [SellWand] selling " + stuffToPrint + "!");
                    List<ItemStack> unsellable = Sell.sellItems(player, inv.getContents());
                    for (ItemStack i : unsellable) {
                        inv.addItem(i);
                    }
                    e.setCancelled(true);
                }
            }
        }
    }
}