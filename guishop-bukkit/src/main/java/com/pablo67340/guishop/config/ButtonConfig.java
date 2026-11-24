package com.pablo67340.guishop.config;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.util.ItemBuilder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;

public class ButtonConfig {

    @Getter
    @Setter
    public Item backwardButton, forwardButton, pageIndicatorButton;

    @Getter
    @Setter
    public int backwardSlot = 48, forwardSlot = 52;

    private FileConfiguration config;

    public void createButtons() {
        config = GUIShop.getINSTANCE().getConfigManager().getMainConfig();
        createBackwardButton();
        createForwardButton();
        createPageIndicatorButton();
    }

    protected void createBackwardButton() {
        if (config.getConfigurationSection("buttons.backward") == null) {
            backwardButton = new ItemBuilder().setNames("&cPrevious page").setLores(Collections.singletonList("&fGo to the previous page")).setMaterial("ARROW").setItemType(ItemType.DUMMY).build();
            return;
        }

        backwardButton = Item.deserialize(config.getConfigurationSection("buttons.backward").getValues(true), -1, null);
        backwardButton.setItemType(ItemType.DUMMY);

        backwardSlot = config.getInt("buttons.backward.slot");
    }

    protected void createForwardButton() {
        if (config.getConfigurationSection("buttons.forward") == null) {
            backwardButton = new ItemBuilder().setNames("&cNext page").setLores(Collections.singletonList("&fGo to the next page")).setMaterial("ARROW").setItemType(ItemType.DUMMY).build();
            return;
        }

        forwardButton = Item.deserialize(config.getConfigurationSection("buttons.forward").getValues(true), -1, null);
        forwardButton.setItemType(ItemType.DUMMY);

        forwardSlot = config.getInt("buttons.forward.slot");
    }

    protected void createPageIndicatorButton() {
        if (config.getConfigurationSection("buttons.page-indicator") == null) {
            pageIndicatorButton = new ItemBuilder().setNames("&fPage %page% of %maxpage%").setMaterial("NETHER_STAR").setItemType(ItemType.DUMMY).build();
            return;
        }

        pageIndicatorButton = Item.deserialize(config.getConfigurationSection("buttons.page-indicator").getValues(true), -1, null);
        pageIndicatorButton.setItemType(ItemType.DUMMY);
    }
}
