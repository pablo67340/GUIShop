package com.pablo67340.guishop.config;

import com.cryptomorin.xseries.XMaterial;
import com.pablo67340.guishop.GUIShop;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuration for the Transaction GUI.
 */
public class TransactionGuiConfig {

    @Getter
    private XMaterial buyMaterial = XMaterial.LIME_STAINED_GLASS_PANE;
    
    @Getter
    private XMaterial sellMaterial = XMaterial.RED_STAINED_GLASS_PANE;
    
    @Getter
    private XMaterial notBuyableMaterial = XMaterial.GRAY_STAINED_GLASS_PANE;
    
    @Getter
    private XMaterial notSellableMaterial = XMaterial.GRAY_STAINED_GLASS_PANE;
    
    @Getter
    private int[] quantities = {1, 32, 64};

    public void load() {
        FileConfiguration config = GUIShop.getINSTANCE().getConfigManager().getMainConfig();
        
        // Load materials
        String buyMatStr = config.getString("transaction-gui.buy-material", "LIME_STAINED_GLASS_PANE");
        String sellMatStr = config.getString("transaction-gui.sell-material", "RED_STAINED_GLASS_PANE");
        String notBuyableMatStr = config.getString("transaction-gui.not-buyable-material", "GRAY_STAINED_GLASS_PANE");
        String notSellableMatStr = config.getString("transaction-gui.not-sellable-material", "GRAY_STAINED_GLASS_PANE");
        
        buyMaterial = XMaterial.matchXMaterial(buyMatStr).orElse(XMaterial.LIME_STAINED_GLASS_PANE);
        sellMaterial = XMaterial.matchXMaterial(sellMatStr).orElse(XMaterial.RED_STAINED_GLASS_PANE);
        notBuyableMaterial = XMaterial.matchXMaterial(notBuyableMatStr).orElse(XMaterial.GRAY_STAINED_GLASS_PANE);
        notSellableMaterial = XMaterial.matchXMaterial(notSellableMatStr).orElse(XMaterial.GRAY_STAINED_GLASS_PANE);
        
        // Load quantities
        int q1 = config.getInt("transaction-gui.quantity-1", 1);
        int q2 = config.getInt("transaction-gui.quantity-2", 32);
        int q3 = config.getInt("transaction-gui.quantity-3", 64);
        quantities = new int[]{q1, q2, q3};
    }
}

