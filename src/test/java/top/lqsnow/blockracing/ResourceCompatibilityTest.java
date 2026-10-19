package top.lqsnow.blockracing;

import org.bukkit.Material;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceCompatibilityTest {
    private static final List<String> BLOCK_LISTS = List.of(
            "EasyBlocks.txt",
            "MediumBlocks.txt",
            "HardBlocks.txt",
            "DyedBlocks.txt",
            "EndBlocks.txt"
    );

    @Test
    void everyConfiguredBlockExistsInPaper263() throws Exception {
        for (String resource : BLOCK_LISTS) {
            for (String name : readLines(resource)) {
                Material material = Material.getMaterial(name);
                assertNotNull(material, () -> resource + " contains unknown material " + name);
            }
        }
    }

    @Test
    void blockPoolsDoNotContainDuplicates() throws Exception {
        Set<String> seen = new HashSet<>();
        for (String resource : BLOCK_LISTS) {
            for (String name : readLines(resource)) {
                assertTrue(seen.add(name), () -> name + " occurs in more than one block pool");
            }
        }
    }

    @Test
    void everyConfiguredBlockHasBundledTranslations() throws Exception {
        JSONObject english = readJson("minecraftlang/en_us.json");
        JSONObject chinese = readJson("minecraftlang/zh_cn.json");
        for (String resource : BLOCK_LISTS) {
            for (String name : readLines(resource)) {
                Material material = Material.getMaterial(name);
                assertNotNull(material);
                String key = "block.minecraft." + name.toLowerCase();
                assertTrue(english.containsKey(key), () -> "en_us.json is missing " + key);
                assertTrue(chinese.containsKey(key), () -> "zh_cn.json is missing " + key);
            }
        }
    }

    @Test
    void includesMinecraft262Blocks() throws Exception {
        assertConfiguredContains("SULFUR", "CINNABAR", "SULFUR_SPIKE", "POTENT_SULFUR", "CHISELED_CINNABAR", "CHISELED_SULFUR");
    }

    @Test
    void includesMinecraft263Blocks() throws Exception {
        assertConfiguredContains(
                "POPLAR_LOG",
                "POPLAR_PLANKS",
                "RED_POPLAR_LEAVES",
                "POPLAR_SHELF",
                "RED_SHRUB",
                "SHELF_MUSHROOM",
                "STRAW_BED",
                "WHITE_WOOL_SLAB",
                "BLACK_WOOL_STAIRS",
                "RED_CONCRETE_SLAB",
                "LIGHT_BLUE_CONCRETE_STAIRS"
        );
    }

    @Test
    void includesPreviouslyMissingBlocks() throws Exception {
        assertConfiguredContains(
                "BIRCH_HANGING_SIGN",
                "STRIPPED_ACACIA_LOG",
                "LAPIS_ORE",
                "DIAMOND_BLOCK",
                "NETHERITE_BLOCK",
                "TUBE_CORAL_FAN",
                "DEAD_HORN_CORAL_BLOCK",
                "PINK_WOOL",
                "BLACK_GLAZED_TERRACOTTA",
                "WHITE_CONCRETE"
        );
    }

    @Test
    void translationsCoverEveryVanillaBlockAndBiome() throws Exception {
        JSONObject english = readJson("minecraftlang/en_us.json");
        JSONObject chinese = readJson("minecraftlang/zh_cn.json");
        // Vanilla 26.3 ships 1975 block names and 67 (non-removed) biomes in en_us.
        assertEquals(1975, countKeys(english, "block.minecraft."), "en_us.json block coverage drifted");
        assertEquals(1975, countKeys(chinese, "block.minecraft."), "zh_cn.json block coverage drifted");
        assertEquals(67, countKeys(english, "biome.minecraft."), "en_us.json biome coverage drifted");
        assertEquals(67, countKeys(chinese, "biome.minecraft."), "zh_cn.json biome coverage drifted");
        for (String key : List.of(
                "block.minecraft.poplar_leaves",
                "block.minecraft.poplar_wall_sign",
                "block.minecraft.poplar_wall_hanging_sign",
                "block.minecraft.potted_poplar_sapling",
                "biome.minecraft.dappled_forest",
                "biome.minecraft.sulfur_caves")) {
            assertTrue(english.containsKey(key), () -> "en_us.json is missing " + key);
            assertTrue(chinese.containsKey(key), () -> "zh_cn.json is missing " + key);
        }
    }

    private static long countKeys(JSONObject json, String prefix) {
        return json.keySet().stream().filter(key -> key.toString().startsWith(prefix)).count();
    }

    private static void assertConfiguredContains(String... names) throws Exception {
        Set<String> configured = new HashSet<>();
        for (String resource : BLOCK_LISTS) {
            configured.addAll(readLines(resource));
        }
        assertTrue(configured.containsAll(List.of(names)));
    }

    private static List<String> readLines(String resource) throws Exception {
        try (InputStream input = ResourceCompatibilityTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, () -> "Missing test resource " + resource);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .toList();
            }
        }
    }

    private static JSONObject readJson(String resource) throws Exception {
        try (InputStream input = ResourceCompatibilityTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, () -> "Missing test resource " + resource);
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return (JSONObject) new JSONParser().parse(reader);
            }
        }
    }
}
