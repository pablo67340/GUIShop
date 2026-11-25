package com.pablo67340.guishop.neoforge.client;

import com.pablo67340.guishop.neoforge.network.ShopAdminPayload;
import com.pablo67340.guishop.neoforge.network.TradeResultPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Asks for a new shop's name.
 * <p>
 * Reached from the plus button on the menu, which is where someone looking to
 * add a shop will look first. The same name rule the server enforces is checked
 * here too, so a bad name is refused as it is typed rather than after a round
 * trip.
 */
final class NewShopScreen extends ShopChildScreen {

    /** Must agree with ShopAdminService, which has the final say. */
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private final List<String> existing;

    private EditBox name;
    private Button create;

    NewShopScreen(ShopBrowserScreen parent, List<String> existing) {
        super(Component.literal("New shop"), parent);
        this.existing = existing;
    }

    @Override
    protected void init() {
        layoutPanel(280, UiTheme.HEADER_HEIGHT + 64 + UiTheme.FOOTER_HEIGHT);

        String typed = name == null ? "" : name.getValue();

        name = new EditBox(font, contentLeft(), headerBottom() + 22, contentWidth(), 18,
                Component.literal("Shop name"));
        name.setHint(Component.literal("Shop name"));
        name.setMaxLength(32);
        // Filtered rather than validated after the fact, so a space or a slash
        // simply cannot be typed into something that becomes a file name.
        name.setFilter(typing -> typing.isEmpty() || VALID_NAME.matcher(typing).matches());
        name.setValue(typed);
        name.setResponder(value -> refreshCreate());
        addRenderableWidget(name);

        create = Button.builder(Component.literal("Create"), button -> submit())
                .bounds(contentLeft(), footerTop() + 4, 70, 16)
                .build();
        addRenderableWidget(create);

        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(contentLeft() + 74, footerTop() + 4, 70, 16)
                .build());

        setInitialFocus(name);
        refreshCreate();
    }

    private void refreshCreate() {
        create.active = problem() == null;
    }

    /**
     * @return why the typed name cannot be used, or null if it can
     */
    private String problem() {
        String typed = name.getValue().trim();
        if (typed.isEmpty()) {
            return null;
        }
        if (existing.stream().anyMatch(shop -> shop.equalsIgnoreCase(typed))) {
            return "There is already a shop called that.";
        }
        return null;
    }

    private void submit() {
        String typed = name.getValue().trim();
        if (typed.isEmpty() || problem() != null) {
            return;
        }
        PacketDistributor.sendToServer(
                new ShopAdminPayload(ShopAdminPayload.Action.CREATE_SHOP, typed, 0, ""));
    }

    @Override
    protected void onServerResult(TradeResultPayload result) {
        if (result.success()) {
            // The menu behind this has already been rebuilt from the new
            // catalogue, so there is nothing left to do here.
            onClose();
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER && create.active) {
            submit();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderPanel(graphics, Component.literal("New shop"), null);
        renderFooterDivider(graphics);

        graphics.drawString(font, "Name", contentLeft(), headerBottom() + 10,
                UiTheme.TEXT, false);

        String typed = name.getValue().trim();
        String issue = problem();
        Component note = issue != null
                ? Component.literal(issue)
                : Component.literal(typed.isEmpty()
                        ? "Letters, digits, dashes and underscores."
                        : "Creates shops/" + typed + ".json and a menu icon.");

        graphics.drawString(font, note, contentLeft(), headerBottom() + 46,
                issue != null ? UiTheme.WARN : UiTheme.TEXT_MUTED, false);

        renderStatus(graphics);
    }

    @Override
    protected int footerTextLeft() {
        return contentLeft() + 148;
    }
}
