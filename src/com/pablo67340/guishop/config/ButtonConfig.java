package com.pablo67340.guishop.config;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ItemType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;

public class ButtonConfig {
    @Getter
    @Setter
    public Item backwardButton, forwardButton, backButton;

    private final FileConfiguration config = GUIShop.getINSTANCE().getMainConfig();

    public void createButtons() {
        createBackButton();
        createBackwardButton();
        createForwardButton();
    }

    protected void createBackwardButton() {
        backwardButton = Item.deserialize(config.getConfigurationSection("buttons.backward").getValues(true), -1, null);
        backwardButton.setItemType(ItemType.DUMMY);
    }

    protected void createForwardButton() {
        forwardButton = Item.deserialize(config.getConfigurationSection("buttons.forward").getValues(true), -1, null);
        forwardButton.setItemType(ItemType.DUMMY);
    }

    protected void createBackButton() {
        backButton = Item.deserialize(config.getConfigurationSection("buttons.back").getValues(true), -1, null);
        backButton.setItemType(ItemType.DUMMY);
    }
}
