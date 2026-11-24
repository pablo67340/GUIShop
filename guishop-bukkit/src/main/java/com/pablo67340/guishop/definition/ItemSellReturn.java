package com.pablo67340.guishop.definition;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ItemSellReturn {

    @Getter
    private final List<ItemStack> notSold;

    @Getter
    private final List<ItemStack> sold;

    @Getter
    private final Boolean couldntSell;

    @Getter
    private final Integer coldntSellCount;

    @Getter
    private final BigDecimal moneyGiven;

    public ItemSellReturn(List<ItemStack> notSold, List<ItemStack> sold, boolean couldntSell, int coldntSellCount, BigDecimal moneyGiven) {
        this.notSold = notSold;
        this.sold = sold;
        this.couldntSell = couldntSell;
        this.coldntSellCount = coldntSellCount;
        this.moneyGiven = moneyGiven;
    }

}
