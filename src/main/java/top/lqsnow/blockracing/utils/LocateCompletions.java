package top.lqsnow.blockracing.utils;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Tab completion for /locatebiome and /locatestructure, read from Paper's live
 * registries so new vanilla biomes, structures and tags appear automatically.
 * Typing "#" switches to tag suggestions (e.g. #village, #abandoned_camp), which
 * vanilla /locate accepts to find the nearest member of a group.
 * <p>
 * Registry reads are plain data reads (no world or region-thread access), so this
 * is safe to call from Paper's async tab-complete thread and from Folia as well.
 * For Folia parity with vanilla tab completion, callers may additionally use
 * {@link #completeWithLocalizedNames(RegistryKey, String, java.util.function.Function)}
 * from a region thread.
 */
public final class LocateCompletions {
    private LocateCompletions() {
    }

    public static <T extends Keyed> List<String> complete(RegistryKey<T> registryKey, String input) {
        Registry<T> registry = RegistryAccess.registryAccess().getRegistry(registryKey);
        String prefix = input == null ? "" : input.toLowerCase(Locale.ROOT);
        if (prefix.startsWith("#")) {
            Stream<String> tags;
            try {
                tags = registry.getTags().stream()
                        .map(tag -> vanillaPath(tag.tagKey().key().namespace(), tag.tagKey().key().value()));
            } catch (UnsupportedOperationException e) {
                return List.of();
            }
            return filter(tags.filter(Objects::nonNull).map(tag -> "#" + tag), prefix);
        }
        return filter(registry.keyStream()
                .map(key -> vanillaPath(key.getNamespace(), key.getKey()))
                .filter(Objects::nonNull), prefix);
    }

    /**
     * Same as {@link #complete(RegistryKey, String)} but maps every vanilla id to a
     * display name (usually a localized one) while keeping the vanilla id at hand.
     * Only suggestions whose display name starts with the input are returned, so
     * localized typing keeps working. The returned ids are vanilla
     * {@code minecraft:} namespace paths so vanilla /locate still accepts them; the
     * display name callback receives the live registry entry, not a raw string.
     */
    public static <T extends Keyed> List<String> completeWithLocalizedNames(RegistryKey<T> registryKey,
                                                                          String input,
                                                                          java.util.function.Function<T, String> displayName) {
        Registry<T> registry = RegistryAccess.registryAccess().getRegistry(registryKey);
        String trimmed = input == null ? "" : input.trim();
        String lowerTrimmed = trimmed.toLowerCase(Locale.ROOT);

        List<String> localized = registry.keyStream()
                .map(key -> {
                    String id = vanillaPath(key.getNamespace(), key.getKey());
                    if (id == null) {
                        return null;
                    }
                    T entry = registry.get(key);
                    if (entry == null) {
                        return null;
                    }
                    String name = displayName.apply(entry);
                    if (name == null || name.isBlank()) {
                        return null;
                    }
                    boolean matches = name.toLowerCase(Locale.ROOT).startsWith(lowerTrimmed)
                            || id.startsWith(trimmed);
                    return matches ? id : null;
                })
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        if (localized.isEmpty() && lowerTrimmed.isEmpty()) {
            // Every vanilla entry has a display name normally; fall back to raw ids.
            return complete(registryKey, input);
        }
        return localized;
    }

    public static List<String> filter(Stream<String> candidates, String prefix) {
        return candidates
                .filter(candidate -> candidate.startsWith(prefix))
                .sorted()
                .toList();
    }

    private static String vanillaPath(String namespace, String path) {
        return NamespacedKey.MINECRAFT.equals(namespace) ? path : null;
    }
}
