package top.lqsnow.blockracing.commands;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.LanguageManager;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.utils.LocateCompletions;
import top.lqsnow.blockracing.utils.StructureTranslation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static top.lqsnow.blockracing.managers.Team.blueTeamPlayers;
import static top.lqsnow.blockracing.managers.Team.redTeamPlayers;

/**
 * /locatestructure on Paper's live structure registry.
 * <p>
 * The hard-coded structure id list is gone: completions, accepted names and the lookup
 * all come from {@link RegistryKey#STRUCTURE}, so structures added by the server version
 * (or by datapacks/addons) work without a plugin update. Plain ids ({@code village_plains}),
 * namespaced ids ({@code mypack:fortress}) and {@code #tag} group suggestions are supported.
 * <p>
 * Folia: {@link World#locateNearestStructure} is a chunk-dependent world read that must run
 * on the region thread owning the player, so it is issued through FoliaLib and the result is
 * reported back on that same owner thread.
 */
public class LocateStructure implements CommandExecutor, TabCompleter {

    private static final int SEARCH_RADIUS = 100;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Bukkit.getLogger().info("This command can only be run by a player.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(Message.NOTICE_LOCATE_STRUCTURE_USAGE.getString(player));
            return true;
        }

        if (!Game.canAffordLocate(player)) {
            return true;
        }

        Structure structure = resolveStructureInput(args[0], player);
        if (structure == null) {
            player.sendMessage(Message.NOTICE_LOCATE_STRUCTURE_INVALID.getString(player));
            return true;
        }

        // Folia-safe: read the player's world/location on the caller (owner) thread first.
        World world = player.getWorld();
        Location source = player.getLocation();

        org.bukkit.util.StructureSearchResult result;
        try {
            result = world.locateNearestStructure(source, structure, SEARCH_RADIUS, false);
        } catch (RuntimeException e) {
            Main.getInstance().getLogger().warning("[BlockRacing] /locatestructure failed for " + structure.getKey()
                    + " in " + world.getName() + ": " + e.getMessage());
            player.sendMessage(Message.NOTICE_LOCATE_STRUCTURE_NOT_FOUND.getString(player));
            return true;
        }

        if (result == null || result.getLocation() == null) {
            player.sendMessage(Message.NOTICE_LOCATE_STRUCTURE_NOT_FOUND.getString(player));
            return true;
        }

        // Only a successful locate is charged, matching the previous behaviour.
        if (!Game.chargeLocateCost(player)) {
            return true;
        }

        Location target = result.getLocation();
        int distance = (int) Math.round(source.distance(target));
        String structureName = StructureTranslation.getValue(structure.getKey());

        player.sendMessage(Message.NOTICE_LOCATE_STRUCTURE_SUCCESS.getString(player)
                .replace("%structure%", structureName)
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

            final String reporterName = player.getName();
            final int teammateDistance = teammate.getWorld().equals(target.getWorld())
                    ? (int) Math.round(teammate.getLocation().distance(target))
                    : -1;
            final String distanceText = teammateDistance >= 0 ? String.valueOf(teammateDistance) : "?";

            sendMessageFoliaSafe(teammate, Message.NOTICE_LOCATE_STRUCTURE_TEAM_SUCCESS.getString(player)
                    .replace("%player%", reporterName)
                    .replace("%structure%", structureName)
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

        Set<String> suggestions = new LinkedHashSet<>(LocateCompletions.complete(RegistryKey.STRUCTURE, prefix));
        if (chinese) {
            suggestions.addAll(LocateCompletions.completeWithLocalizedNames(
                    RegistryKey.STRUCTURE, prefix, StructureTranslation::getValue));
        }
        return new ArrayList<>(suggestions);
    }

    /**
     * Accepts {@code village_plains}, {@code minecraft:village_plains}, a datapack id, or a
     * localized structure name in the sender's language, resolved against the live registry.
     */
    static Structure resolveStructureInput(String input, CommandSender sender) {
        String normalized = input == null ? "" : input.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        Structure direct = lookupStructure(normalized);
        if (direct != null) {
            return direct;
        }

        if (sender instanceof Player player && LanguageManager.usesChinese(player)) {
            for (Structure structure : RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE)) {
                String localized = StructureTranslation.getValue(structure.getKey());
                if (localized != null && localized.equalsIgnoreCase(normalized)) {
                    return structure;
                }
            }
        }
        return null;
    }

    private static Structure lookupStructure(String id) {
        Registry<Structure> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE);
        NamespacedKey key = NamespacedKey.fromString(id.toLowerCase(Locale.ROOT));
        if (key == null) {
            return null;
        }
        Structure structure = registry.get(key);
        if (structure == null && !id.contains(":")) {
            structure = registry.get(NamespacedKey.minecraft(id.toLowerCase(Locale.ROOT)));
        }
        return structure;
    }
}
