package de.miraculixx.veinminerEnchant.paper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.Files;
import java.nio.file.Path;

public record VeinminerEnchantmentSettings(int minCost, int maxCost, int anvilCost, boolean pickaxeOnly) {
    private static final Path FILE_PATH = Path.of("plugins", "Veinminer", "enchantmentSettings.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public VeinminerEnchantmentSettings() {
        this(15, 65, 7, false);
    }

    public static VeinminerEnchantmentSettings get() {
        VeinminerEnchantmentSettings defaults = new VeinminerEnchantmentSettings();

        try {
            if (!Files.exists(FILE_PATH)) {
                Path parent = FILE_PATH.getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.writeString(FILE_PATH, GSON.toJson(defaults));
                System.out.println("[VeinminerEnchant] Created " + FILE_PATH.getFileName() + " default config");
                return defaults;
            }

            RawSettings raw = GSON.fromJson(Files.readString(FILE_PATH), RawSettings.class);
            return new VeinminerEnchantmentSettings(
                    raw != null && raw.minCost != null ? raw.minCost : defaults.minCost,
                    raw != null && raw.maxCost != null ? raw.maxCost : defaults.maxCost,
                    raw != null && raw.anvilCost != null ? raw.anvilCost : defaults.anvilCost,
                    raw != null && raw.pickaxeOnly != null ? raw.pickaxeOnly : defaults.pickaxeOnly
            );
        } catch (Exception e) {
            System.out.println("[VeinminerEnchant] Failed to load " + FILE_PATH.getFileName() + " config: Reason: " + e.getMessage());
            return defaults;
        }
    }

    private static final class RawSettings {
        Integer minCost;
        Integer maxCost;
        Integer anvilCost;
        Boolean pickaxeOnly;
    }
}

