package com.pablo67340.guishop.economy;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.economy.DynamicPricingSettings.ItemPriceOverride;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads {@code dynamicpricing.yml} into a {@link DynamicPricingSettings}.
 * <p>
 * The pricing maths lives in core and knows nothing about YAML; this is the
 * Bukkit half that fills in the numbers.
 */
public final class DynamicPricingConfigLoader {

    private DynamicPricingConfigLoader() {
    }

    public static DynamicPricingSettings load(GUIShop plugin) {
        DynamicPricingSettings settings = new DynamicPricingSettings();

        File configFile = new File(plugin.getDataFolder(), "dynamicpricing.yml");

        // Create default config if it doesn't exist
        if (!configFile.exists()) {
            plugin.saveResource("dynamicpricing.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Load global defaults
        settings.setPriceChangePerTransaction(config.getDouble("price-change-per-item", 0.01));
        settings.setMaxPriceMultiplier(config.getDouble("max-price-multiplier", 2.0));
        settings.setMinPriceMultiplier(config.getDouble("min-price-multiplier", 0.5));
        settings.setNormalizationRate(config.getDouble("normalization-rate", 0.001));
        settings.setNormalizationInterval(config.getInt("normalization-interval", 300));

        // Load per-item overrides
        settings.getItemOverrides().clear();
        int totalLinks = 0;
        ConfigurationSection overridesSection = config.getConfigurationSection("item-overrides");
        if (overridesSection != null) {
            for (String itemKey : overridesSection.getKeys(false)) {
                ConfigurationSection itemSection = overridesSection.getConfigurationSection(itemKey);
                if (itemSection != null) {
                    // Use global defaults if not specified for this item
                    double itemPriceChange = itemSection.getDouble("price-change-per-item",
                            settings.getPriceChangePerTransaction());
                    double itemMaxMult = itemSection.getDouble("max-price-multiplier",
                            settings.getMaxPriceMultiplier());
                    double itemMinMult = itemSection.getDouble("min-price-multiplier",
                            settings.getMinPriceMultiplier());

                    // Load linked items (affects section)
                    Map<String, Double> affects = new HashMap<>();
                    ConfigurationSection affectsSection = itemSection.getConfigurationSection("affects");
                    if (affectsSection != null) {
                        for (String affectedItem : affectsSection.getKeys(false)) {
                            double multiplier = affectsSection.getDouble(affectedItem, 1.0);
                            affects.put(affectedItem.toUpperCase(), multiplier);
                            totalLinks++;
                            plugin.getLogUtil().debugLog("  -> " + itemKey.toUpperCase() + " affects "
                                    + affectedItem.toUpperCase() + " with multiplier " + multiplier);
                        }
                    }

                    // Store with uppercase key for consistent lookup
                    settings.getItemOverrides().put(itemKey.toUpperCase(),
                            new ItemPriceOverride(itemPriceChange, itemMaxMult, itemMinMult, affects));
                    plugin.getLogUtil().debugLog("Loaded item override for " + itemKey.toUpperCase()
                            + ": priceChange=" + itemPriceChange + ", maxMult=" + itemMaxMult
                            + ", minMult=" + itemMinMult + ", affects=" + affects.size() + " items");
                }
            }
            plugin.getLogUtil().log("Loaded " + settings.getItemOverrides().size()
                    + " item-specific dynamic pricing overrides"
                    + (totalLinks > 0 ? " with " + totalLinks + " linked price relationships" : ""));
        }

        return settings;
    }
}
