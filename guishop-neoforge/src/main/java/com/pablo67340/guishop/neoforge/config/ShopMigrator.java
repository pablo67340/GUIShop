package com.pablo67340.guishop.neoforge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pablo67340.guishop.platform.GuiShopPlatform;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

/**
 * Converts the Bukkit plugin's YAML shop files to this mod's JSON format.
 * <p>
 * Runs once per file: the YAML is renamed to {@code .yml.bak} afterwards, so
 * nothing is destroyed and a later start does not convert it again and discard
 * edits made to the JSON.
 */
final class ShopMigrator {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    static final String BACKUP_SUFFIX = ".bak";

    private ShopMigrator() {
    }

    /**
     * Converts one legacy file, if it has not already been converted.
     *
     * @param source the {@code .yml} file
     * @param target where the JSON should be written
     * @param isMenu whether to expect the menu's extra nesting
     * @return the parsed document when it was converted, otherwise null
     */
    static Map<String, Object> migrate(Path source, Path target, String name, boolean isMenu) {
        if (Files.exists(target)) {
            // The JSON wins, and the stale YAML is set aside so it stops being
            // noticed on every start.
            backup(source);
            return null;
        }

        Map<String, Object> document = LegacyYamlReader.read(source, name, isMenu);
        if (document == null) {
            return null;
        }

        if (!write(target, document)) {
            return null;
        }

        if (!backup(source)) {
            // Leaving both in place would convert again next start and lose any
            // edits, so the half-done conversion is withdrawn.
            delete(target);
            return null;
        }

        int items = document.get("items") instanceof List<?> list ? list.size() : 0;
        GuiShopPlatform.get().logger().info("Converted " + source.getFileName()
                + " to " + target.getFileName() + " (" + items + " items); original kept as "
                + source.getFileName() + BACKUP_SUFFIX);

        return document;
    }

    private static boolean write(Path target, Map<String, Object> document) {
        try {
            Files.createDirectories(target.getParent());
            try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
                GSON.toJson(document, writer);
            }
            return true;
        } catch (IOException exception) {
            GuiShopPlatform.get().logger().warning(
                    "Could not write " + target.getFileName() + ": " + exception.getMessage());
            return false;
        }
    }

    private static boolean backup(Path source) {
        Path backup = source.resolveSibling(source.getFileName() + BACKUP_SUFFIX);
        try {
            Files.move(source, backup, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException exception) {
            GuiShopPlatform.get().logger().warning(
                    "Could not set aside " + source.getFileName() + ": " + exception.getMessage());
            return false;
        }
    }

    private static void delete(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            GuiShopPlatform.get().logger().warning(
                    "Could not remove the incomplete " + target.getFileName() + ": " + exception.getMessage());
        }
    }
}
