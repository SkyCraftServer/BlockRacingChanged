package top.lqsnow.blockracing.utils;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.managers.Message;

import java.io.File;
import java.io.FileReader;
import java.util.logging.Level;

public class StructureTranslation {

    public static String getValue(String structureId) {
        if (structureId == null || structureId.isBlank()) {
            return "";
        }

        try {
            String key = "structure.minecraft." + structureId.toLowerCase();

            File file = resolveLangFile();
            FileReader reader = new FileReader(file);
            JSONParser parser = new JSONParser();
            Object object = parser.parse(reader);
            JSONObject jsonObject = (JSONObject) object;

            Object value = jsonObject.get(key);
            if (value != null) {
                return value.toString();
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.FINE, "[BlockRacing] Error getting structure translation", e);
        }

        String nice = structureId.toLowerCase().replace('_', ' ');
        if (!nice.isEmpty()) {
            nice = nice.substring(0, 1).toUpperCase() + nice.substring(1);
        }
        return nice;
    }

    /**
     * Translates a structure from a live-registry key. Vanilla ids keep the historic
     * {@code structure.minecraft.<id>} lookup; namespaced (datapack) structures fall
     * back to a readable version of their path so addon structures stay usable.
     */
    public static String getValue(org.bukkit.NamespacedKey structureKey) {
        if (structureKey == null) {
            return "";
        }
        return getValue(structureKey.getKey());
    }

    /** Translates a structure taken straight from the live structure registry. */
    public static String getValue(org.bukkit.generator.structure.Structure structure) {
        if (structure == null) {
            return "";
        }
        return getValue(structure.key().value());
    }

    private static File resolveLangFile() {
        String langCode = Message.getLanguageCode();
        File dir = new File(Main.getInstance().getDataFolder(), "minecraftlang");
        File langFile = new File(dir, langCode + ".json");

        if (!langFile.exists()) {
            langFile = new File(dir, Message.getDefaultLanguageCode() + ".json");
        }

        return langFile;
    }
}