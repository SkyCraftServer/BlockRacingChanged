package top.lqsnow.blockracing.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocateCompletionsTest {

    @Test
    void filtersByPrefixAndSorts() {
        assertEquals(
                List.of("abandoned_camp_dappled_forest", "abandoned_camp_forest"),
                LocateCompletions.filter(
                        Stream.of("village_plains", "abandoned_camp_forest", "abandoned_camp_dappled_forest"),
                        "abandoned"));
    }

    @Test
    void matchesTagSuggestions() {
        assertEquals(
                List.of("#abandoned_camp"),
                LocateCompletions.filter(Stream.of("#village", "#abandoned_camp"), "#ab"));
    }

    @Test
    void emptyPrefixReturnsEverythingSorted() {
        assertEquals(
                List.of("minecraft:diamond_ore", "minecraft:iron_ore", "minecraft:lapis_ore"),
                LocateCompletions.filter(
                        Stream.of("minecraft:iron_ore", "minecraft:diamond_ore", "minecraft:lapis_ore"), ""));
    }
}
