package com.pablo67340.guishop.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.pablo67340.guishop.GUIShop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.UUID;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * A library for the Bukkit API to create player skulls from uuid.
 * <p>
 * Does not use any NMS code, and should work across all versions.
 *
 * @author Dean B on 12/28/2016.
 * @author Bryce W, 4/8/2021.
 */
public class SkullCreator {

    // some reflection stuff to be used when setting a skull's profile
    private static Method metaSetProfileMethod;
    private static Field metaProfileField;

    /**
     * Modifies a skull to use the skin based on the given base64 string.
     *
     * @param item The ItemStack to put the base64 onto. Must be a player skull.
     * @param base64 The base64 string containing the texture.
     * @return The head with a custom texture.
     */
    public static ItemStack itemFromBase64(ItemStack item, String base64, String UUID) {
        notNull(item, "item");
        notNull(base64, "base64");

        if (!(item.getItemMeta() instanceof SkullMeta)) {
            return null;
        }
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        mutateItemMeta(meta, base64, UUID);
        item.setItemMeta(meta);

        return item;
    }

    private static void notNull(Object o, String name) {
        if (o == null) {
            throw new NullPointerException(name + " should not be null!");
        }
    }

    private static GameProfile makeProfile(String uid, String b64) {
        // random uuid based on the b64 string
        GameProfile profile = new GameProfile(UUID.fromString(uid), "aaaaa");
        profile.getProperties().put("textures", new Property("textures", b64));
        return profile;
    }

    public static void mutateItemMeta(SkullMeta meta, String b64, String UUID) {
        try {
            if (metaSetProfileMethod == null) {
                metaSetProfileMethod = meta.getClass().getDeclaredMethod("setProfile", GameProfile.class);
                metaSetProfileMethod.setAccessible(true);
            }
            metaSetProfileMethod.invoke(meta, makeProfile(UUID, b64));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            // if in an older API where there is no setProfile method,
            // we set the profile field directly.
            try {
                if (metaProfileField == null) {
                    metaProfileField = meta.getClass().getDeclaredField("profile");
                    metaProfileField.setAccessible(true);
                }
                metaProfileField.set(meta, makeProfile(UUID, b64));

            } catch (NoSuchFieldException | IllegalAccessException ex2) {
                GUIShop.debugLog("Error making profile when loading player head: " + ex2.getMessage());
            }
        }
    }

    public static String getBase64FromUUID(String uuid) {
        String base64 = GUIShop.getINSTANCE().getCachedHeads().get(uuid);
        if (base64 != null) {
            GUIShop.debugLog("Loaded player: " + uuid + " from cache.");
            return base64;
        } else {
            try {
                GUIShop.debugLog("PlayerHead: " + uuid + " not cached. Grabbing skin and caching.");
                URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("User-Agent", "Mozilla/5.0");
                int responseCode = con.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder response;
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                        String inputLine;
                        response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                    }

                    JSONParser parser = new JSONParser();
                    JSONObject obj = (JSONObject) parser.parse(response.toString());
                    JSONArray properties = (JSONArray) obj.get("properties");
                    JSONObject default_props = (JSONObject) properties.get(0);
                    String skin_base64 = (String) default_props.get("value");

                    GUIShop.getINSTANCE().getCacheConfig().set("player-heads." + uuid, skin_base64);

                    Thread thread = new Thread(() -> {
                        try {
                            GUIShop.getINSTANCE().getCacheConfig().save(GUIShop.getINSTANCE().getCachef());
                        } catch (IOException ex) {
                            GUIShop.debugLog("Error saving player-heads to cache: " + ex.getMessage());
                        }
                    });
                    thread.start();

                    return skin_base64;
                }
            } catch (IOException | ParseException ex) {
                GUIShop.debugLog("Error grabbing skin base64 from Mojang: " + ex.getMessage());
            }
        }
        return "";
    }

}
