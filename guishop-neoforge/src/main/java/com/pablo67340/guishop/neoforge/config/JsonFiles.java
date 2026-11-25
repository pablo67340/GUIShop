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
import java.util.Map;

/**
 * Writing of the mod's JSON files, shared so every writer formats the same way
 * and is equally careful about not truncating a file it fails to finish.
 */
public final class JsonFiles {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private JsonFiles() {
    }

    /**
     * Writes a document, via a temporary file so an interrupted write cannot
     * leave a half-written config that fails to load next start.
     *
     * @return whether the write succeeded
     */
    public static boolean write(Path file, Map<String, Object> document) {
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(document, writer);
            }
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException exception) {
            GuiShopPlatform.get().logger().warning(
                    "Could not write " + file.getFileName() + ": " + exception.getMessage());
            return false;
        }
    }
}
