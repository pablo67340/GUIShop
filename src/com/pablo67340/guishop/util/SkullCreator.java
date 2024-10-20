package com.pablo67340.guishop.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.pablo67340.guishop.GUIShop;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

/**
 * A library for the Bukkit API to create player skulls from uuid.
 * <p>
 *
 * @author Dean B on 12/28/2016.
 * @author Bryce W, 4/8/2021.
 * @author Bryce W, 10/20/2024
 */
public class SkullCreator {

    /**
     * Modifies a skull to use the skin based on the given base64 string.
     *
     * @param item The ItemStack to put the base64 onto. Must be a player skull.
     * @param base64 The base64 string containing the texture.
     * @return The head with a custom texture.
     */
    public static ItemStack itemFromBase64(ItemStack item, String base64, String uuid) {
        notNull(item, "item");
        notNull(base64, "base64");

        if (!(item.getItemMeta() instanceof SkullMeta)) {
            return null;
        }
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        mutateItemMeta(meta, base64, uuid);
        item.setItemMeta(meta);

        return item;
    }

    private static void notNull(Object o, String name) {
        if (o == null) {
            throw new NullPointerException(name + " should not be null!");
        }
    }

    private static PlayerProfile makeProfile(String uid, String b64) {
        // random uuid based on the b64 string
        JSONParser parser = new JSONParser();
        try {
            String b64Decoded = new String(Base64.getDecoder().decode(b64));
            JSONObject profileObj = (JSONObject)parser.parse(b64Decoded);
            JSONObject textures = (JSONObject)profileObj.get("textures");
            JSONObject skin = (JSONObject)textures.get("SKIN");
            String url = (String)skin.get("url");
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.fromString(uid), "aaaa");
            PlayerTextures playerTextures = profile.getTextures();
            playerTextures.setSkin(new URL(url));
            return profile;
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return null;
    }

    public static void mutateItemMeta(SkullMeta meta, String b64, String UUID) {
        meta.setOwnerProfile(makeProfile(UUID, b64));
    }

    public static String getBase64FromUUID(String uuid) {
        String base64 = GUIShop.getINSTANCE().getConfigManager().getCachedHeads().get(uuid);
        if (base64 != null) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Loaded player: " + uuid + " from cache.");
            return base64;
        } else {
            try {
                GUIShop.getINSTANCE().getLogUtil().debugLog("PlayerHead: " + uuid + " not cached. Grabbing skin and caching.");
                URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("User-Agent", "Mozilla/5.0");
                int responseCode = con.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder response;
                    try ( BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
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

                    GUIShop.getINSTANCE().getConfigManager().getCacheConfig().set("player-heads." + uuid, skin_base64);

                    Thread thread = new Thread(() -> {
                        try {
                            GUIShop.getINSTANCE().getConfigManager().getCacheConfig().save(GUIShop.getINSTANCE().getConfigManager().getCacheFile());
                        } catch (IOException ex) {
                            GUIShop.getINSTANCE().getLogUtil().debugLog("Error saving player-heads to cache: " + ex.getMessage());
                        }
                    });
                    thread.start();

                    return skin_base64;
                }
            } catch (IOException | ParseException ex) {
                GUIShop.getINSTANCE().getLogUtil().debugLog("Error grabbing skin base64 from Mojang: " + ex.getMessage());
            }
        }
        return "";
    }

}
