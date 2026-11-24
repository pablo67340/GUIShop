package com.pablo67340.guishop.neoforge.definition;

import com.pablo67340.guishop.definition.PagedDefinition;

/**
 * A NeoForge shop: its pages of items, plus the display title from the config.
 * All page handling lives in {@link PagedDefinition}.
 */
public class NeoShop extends PagedDefinition<NeoItem, NeoPage> {

    private String name;
    private String title;

    /**
     * The shop's own name, as used by /shop and by menu target-shop entries.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The title shown at the top of the shop, still in '&' colour code form.
     */
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
