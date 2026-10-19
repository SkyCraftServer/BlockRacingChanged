package top.lqsnow.blockracing.commands;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.LanguageManager;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.utils.BiomeTranslation;
import top.lqsnow.blockracing.utils.LocateCompletions;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static top.lqsnow.blockracing.managers.Team.blueTeamPlayers;
import static top.lqsnow.blockracing.managers.Team.redTeamPlayers;

/**
 * /locatebiome on Paper's live biome registry.
 * <p>
 * Covers every vanilla biome Paper exposes (including ones added after {@code Biome}
 * became an interface) and accepts input as {@code biome}, {@code minecraft:biome}
 * or a localized biome name in the sender's language.
 * <p>
 * Folia: {@link World#locateNearestBiome} is a chunk-dependent world read and must run
 * on the region thread owning the player, so it is issued through FoliaLib and the
 * result is reported back on that same owner thread. No other thread is touched and
 * nothing blocks waiting for one.
 */
public class LocateBiome implements CommandExecutor, TabCompleter {

    private static final int SEARCH_RADIUS = 6400;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Bukkit.getLogger().info("This command can only be run by a player.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(Message.NOTICE_LOCATE_BIOME_USAGE.getString(player));
            return true;
        }

        if (!Game.canAffordLocate(player)) {
            return true;
        }

        Biome biome = resolveBiomeInput(args[0], player);
        if (biome == null) {
            player.sendMessage(Message.NOTICE_LOCATE_BIOME_INVALID.getString(player));
            return true;
        }

        // Folia-safe: read the player's world/location on the caller (owner) thread first.
        World world = player.getWorld();
        Location source = player.getLocation();

        org.bukkit.util.BiomeSearchResult result;
        try {
            result = world.locateNearestBiome(source, SEARCH_RADIUS, biome);
        } catch (RuntimeException e) {
            Main.getInstance().getLogger().warning("[BlockRacing] /locatebiome failed for " + biome.getKey()
                    + " in " + world.getName() + ": " + e.getMessage());
            player.sendMessage(Message.NOTICE_LOCATE_BIOME_NOT_FOUND.getString(player));
            return true;
        }

        if (result == null || result.getLocation() == null) {
            player.sendMessage(Message.NOTICE_LOCATE_BIOME_NOT_FOUND.getString(player));
            return true;
        }

        // Only a successful locate is charged, matching the previous behaviour.
        if (!Game.chargeLocateCost(player)) {
            return true;
        }

        Location target = result.getLocation();
        int distance = (int) Math.round(source.distance(target));
        String biomeName = BiomeTranslation.getValue(biome);

        player.sendMessage(Message.NOTICE_LOCATE_BIOME_SUCCESS.getString(player)
                .replace("%biome%", biomeName)
                .replace("%x%", String.valueOf(target.getBlockX()))
                .replace("%y%", String.valueOf(target.getBlockY()))
                .replace("%z%", String.valueOf(target.getBlockZ()))
                .replace("%distance%", String.valueOf(distance)));

        List<String> teammates = redTeamPlayers.contains(player.getName()) ? redTeamPlayers
                : (blueTeamPlayers.contains(player.getName()) ? blueTeamPlayers : List.of());

        for (String teammateName : teammates) {
            if (teammateName.equals(player.getName())) {
                continue;
            }
            Player teammate = Bukkit.getPlayer(teammateName);
            if (teammate == null || !teammate.isOnline()) {
                continue;
            }

            final String teammateNameCopy = player.getName();
            final int teammateDistance = teammate.getWorld().equals(target.getWorld())
                    ? (int) Math.round(teammate.getLocation().distance(target))
                    : -1;
            final String distanceText = teammateDistance >= 0 ? String.valueOf(teammateDistance) : "?";

            sendMessageFoliaSafe(teammate, Message.NOTICE_LOCATE_BIOME_TEAM_SUCCESS.getString(player)
                    .replace("%player%", teammateNameCopy)
                    .replace("%biome%", biomeName)
                    .replace("%x%", String.valueOf(target.getBlockX()))
                    .replace("%y%", String.valueOf(target.getBlockY()))
                    .replace("%z%", String.valueOf(target.getBlockZ()))
                    .replace("%distance%", distanceText));
        }

        return true;
    }

    private static void sendMessageFoliaSafe(Player player, String message) {
        if (player == null) {
            return;
        }
        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            Main.getFoliaLib().getScheduler().runAtEntity(player, task -> player.sendMessage(message));
            return;
        }
        player.sendMessage(message);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        String prefix = strings.length == 0 ? "" : strings[strings.length - 1];
        boolean chinese = commandSender instanceof Player player && LanguageManager.usesChinese(player);

        // Raw vanilla ids stay valid input in every language.
        Set<String> suggestions = new LinkedHashSet<>(LocateCompletions.complete(RegistryKey.BIOME, prefix));
        if (chinese) {
            suggestions.addAll(LocateCompletions.completeWithLocalizedNames(
                    RegistryKey.BIOME, prefix, BiomeTranslation::getValue));
        }
        return new ArrayList<>(suggestions);
    }

    /**
     * Accepts {@code biome}, {@code minecraft:biome}, or a localized biome name in the
     * sender's language, resolved against the live registry so new biomes work without
     * a code change.
     */
    static Biome resolveBiomeInput(String input, CommandSender sender) {
        String normalized = input == null ? "" : input.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        String id = stripNamespace(normalized);
        Biome direct = lookupBiome(id);
        if (direct != null) {
            return direct;
        }

        if (sender instanceof Player player && LanguageManager.usesChinese(player)) {
            for (Biome biome : RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)) {
                String localized = BiomeTranslation.getValue(biome);
                if (localized != null && localized.equalsIgnoreCase(normalized)) {
                    return biome;
                }
            }
        }
        return null;
    }

    private static Biome lookupBiome(String id) {
        Registry<Biome> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
        NamespacedKey key = NamespacedKey.fromString(id.toLowerCase(Locale.ROOT));
        if (key == null) {
            return null;
        }
        Biome biome = registry.get(key);
        if (biome == null && !id.contains(":")) {
            biome = registry.get(NamespacedKey.minecraft(id.toLowerCase(Locale.ROOT)));
        }
        return biome;
    }

    private static String stripNamespace(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("minecraft:") ? lower.substring("minecraft:".length()) : lower;
    }
}
