package com.pablo67340.guishop.neoforge.config;

import com.pablo67340.guishop.neoforge.util.TextUtil;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything GUIShop says to a player.
 * <p>
 * The plugin's {@code messages.yml} has a few hundred keys, but most are usage
 * and help text for the chat commands that did the editing on Bukkit. The
 * editor is a screen here, so only the messages that are actually sent survive,
 * each one wired into the code that sends it. A key nothing reads would just be
 * a setting that looks editable and does nothing.
 * <p>
 * Kept in {@code settings.json} rather than a file of its own so there is a
 * single config to load, write and reload.
 */
public final class Messages {

    private static final Map<String, String> messages = new LinkedHashMap<>(defaults());

    private Messages() {
    }

    /**
     * The wording as shipped, taken from the plugin so a server moving across
     * reads the same thing it always did.
     */
    static Map<String, String> defaults() {
        Map<String, String> values = new LinkedHashMap<>();

        values.put("prefix", "&f[&cGUIShop&f] ");
        values.put("no-permission", "&cNo permission!");
        values.put("only-player", "&4You can only run this command as a player!");
        values.put("disabled-world", "&cYou cannot use the shop from this world!");
        values.put("shop-not-found", "&fShop &c%shop% &fwas not found.");

        values.put("purchase", "&fA purchase was made, and &c%taken% &fwas taken from your account.");
        values.put("not-enough-money", "&fYou need &c%needed% &fto purchase this!");
        values.put("cant-buy", "&cSorry, but you are not able to buy that item.");
        values.put("too-high-quantity", "&fYou can't buy more than &c%max% &fitems of that type!");
        values.put("partial-purchase",
                "&eOnly %amount% items fit in your inventory. The rest were dropped at your feet.");

        values.put("sell", "&fYour items were sold, and &a%money% &fwas added to your account.");
        values.put("cant-sell", "&cSorry, but you are not able to sell that item.");
        values.put("not-enough-items", "&fYou do not have &c%amount% &fsuch items to sell.");

        values.put("command-error",
                "&cThere was an error executing this command. Please contact an administrator.");
        values.put("something-wrong", "&cSomething went wrong, contact an admin.");
        values.put("item-gone", "&cThat item is no longer there. Reopen the shop.");

        // Said by the editor, which the plugin had no equivalent for.
        values.put("no-edit-permission", "&cYou do not have permission to edit shops.");
        values.put("saved", "&aSaved to %file%.");
        values.put("nothing-changed", "&7Nothing changed.");
        values.put("error-saving", "&cCould not save: %error%");

        return values;
    }

    /**
     * The keys in the order the settings screen should list them, so adding a
     * message in one place is enough.
     */
    public static List<String> keys() {
        return List.copyOf(defaults().keySet());
    }

    public static String raw(String key) {
        return messages.getOrDefault(key, "");
    }

    /**
     * A message with its placeholders filled and its colour codes applied.
     *
     * @param replacements placeholder name then value, repeatedly. Names are
     *                     given without the surrounding percent signs
     */
    public static Component get(String key, Object... replacements) {
        return TextUtil.toComponent(plain(key, replacements));
    }

    /**
     * The same, prefixed, for anything sent to chat. The in-screen status lines
     * leave the prefix off: the player can already see which screen is talking
     * to them.
     */
    public static Component chat(String key, Object... replacements) {
        return TextUtil.toComponent(raw("prefix") + plain(key, replacements));
    }

    private static String plain(String key, Object... replacements) {
        String text = raw(key);

        // Stepped in pairs; an odd trailing argument is a caller bug rather
        // than something to guess at, so it is ignored.
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            text = text.replace("%" + replacements[index] + "%",
                    String.valueOf(replacements[index + 1]));
        }
        return text;
    }

    // ----------------------------------------------------------- persistence

    static void reset() {
        messages.clear();
        messages.putAll(defaults());
    }

    /**
     * Overrides only the keys the file defines, so a partial block keeps the
     * shipped wording for everything it leaves out.
     */
    static void applyDocument(Map<String, Object> document) {
        if (document == null) {
            return;
        }
        document.forEach((key, value) -> {
            if (value != null) {
                messages.put(key, String.valueOf(value));
            }
        });
    }

    static Map<String, String> all() {
        return new LinkedHashMap<>(messages);
    }

    /**
     * @return whether the key is one this build knows how to send
     */
    static boolean set(String key, String value) {
        if (!messages.containsKey(key)) {
            return false;
        }
        messages.put(key, value);
        return true;
    }
}
