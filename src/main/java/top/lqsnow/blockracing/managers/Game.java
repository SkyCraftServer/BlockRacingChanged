package top.lqsnow.blockracing.managers;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.tcoded.folialib.wrapper.task.WrappedTask;

import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.voicechat.VoicechatBridge;
import top.lqsnow.blockracing.utils.BiomeTranslation;
import top.lqsnow.blockracing.utils.ColorUtil;
import top.lqsnow.blockracing.utils.TranslationUtil;
import top.lqsnow.blockracing.scoreboard.Scoreboard;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import java.time.Duration;

import static top.lqsnow.blockracing.listeners.BasicListener.editAmountPlayer;
import static top.lqsnow.blockracing.managers.Block.*;
import static top.lqsnow.blockracing.managers.Gui.*;
import static top.lqsnow.blockracing.scoreboard.Scoreboard.updateScoreboard;
import static top.lqsnow.blockracing.managers.Team.*;
import static top.lqsnow.blockracing.utils.ColorUtil.t;
import static top.lqsnow.blockracing.utils.CommandUtil.*;

public class Game {
    public enum GameState {
        PREGAME, INGAME, END
    }

    public static volatile GameState currentGameState = GameState.PREGAME;
    public static List<String> readyPlayers = new CopyOnWriteArrayList<>();
    public static int redTeamScore = 0;
    public static int blueTeamScore = 0;
    public static int redTeamCurrentBlockAmount = 0;
    public static int blueTeamCurrentBlockAmount = 0;
    public static int redTeamTotalBlockAmount = 0;
    public static int blueTeamTotalBlockAmount = 0;
    public static List<String> freeRandomTPList = new CopyOnWriteArrayList<>();

    public static ArrayList<Inventory> redTeamChest = new ArrayList<>();
    public static ArrayList<Inventory> blueTeamChest = new ArrayList<>();
    public static Map<Integer, String> redTeamChestNameCache = new ConcurrentHashMap<>();
    public static Map<Integer, String> blueTeamChestNameCache = new ConcurrentHashMap<>();

    public static Map<Integer, Location> redWaypoint = new ConcurrentHashMap<>();
    public static Map<Integer, Location> blueWaypoint = new ConcurrentHashMap<>();

    public static Map<Integer, Material> redWaypointIconCache = new ConcurrentHashMap<>();
    public static Map<Integer, Material> blueWaypointIconCache = new ConcurrentHashMap<>();
    public static Map<Integer, String> redWaypointBiomeCache = new ConcurrentHashMap<>();
    public static Map<Integer, String> blueWaypointBiomeCache = new ConcurrentHashMap<>();
    public static Map<Integer, String> redWaypointNameCache = new ConcurrentHashMap<>();
    public static Map<Integer, String> blueWaypointNameCache = new ConcurrentHashMap<>();

    public static int redTeamRollCount;
    public static int blueTeamRollCount;
    public static int contestModeRollCount;
    public static List<String> redRollPlayers = new CopyOnWriteArrayList<>();
    public static List<String> blueRollPlayers = new CopyOnWriteArrayList<>();
    public static List<String> contestModeRollPlayers = new CopyOnWriteArrayList<>();
    public static List<String> inGamePlayers = new CopyOnWriteArrayList<>();
    public static int locateCost;
    public static List<String> locateCommandPermission = new CopyOnWriteArrayList<>();
    public static Map<String, Integer> collectAmount = new ConcurrentHashMap<>();
    private static final Deque<Location> randomTpPool = new ArrayDeque<>();

    private static World activeGameWorld;

    private static WrappedTask timeModeTask;
    private static WrappedTask preGameTask;
    private static WrappedTask inGameTask;
    private static int effectMaintenanceTicks;
    private static int timeModeRemainingSeconds;
    private static int timeModeDurationSeconds;
    private static boolean timeModeOvertime;
    private enum ComebackBuffState { NONE, RED, BLUE }
    private static ComebackBuffState comebackBuffState = ComebackBuffState.NONE;

    private enum RenameTarget {
        TEAM_CHEST,
        WAYPOINT
    }

    private static class RenameRequest {
        private final RenameTarget target;
        private final String team;
        private final int index;

        private RenameRequest(RenameTarget target, String team, int index) {
            this.target = target;
            this.team = team;
            this.index = index;
        }
    }

    private static final Map<String, RenameRequest> renameRequests = new ConcurrentHashMap<>();

    public static boolean isTimeModeActive() {
        return Setting.getCurrentGameMode().equals(Setting.GameMode.TIME);
    }

    public static boolean isContestModeActive() {
        return Setting.getCurrentGameMode().equals(Setting.GameMode.CONTEST);
    }

    public static boolean isTimeModeOvertime() {
        return timeModeOvertime;
    }

    public static int getTimeModeRemainingSeconds() {
        return timeModeRemainingSeconds;
    }

    public static int getTimeModeDurationSeconds() {
        return timeModeDurationSeconds;
    }

    /** Restore time-mode countdown state after recovering a saved game (TIME mode). */
    public static void restoreRecoveredTimeMode(int durationSeconds, int remainingSeconds, boolean overtime) {
        timeModeDurationSeconds = Math.max(0, durationSeconds);
        timeModeRemainingSeconds = Math.max(0, remainingSeconds);
        timeModeOvertime = overtime;
    }

    public static String getFormattedTimeModeRemaining() {
        int seconds = Math.max(0, timeModeRemainingSeconds);
        Duration duration = Duration.ofSeconds(seconds);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long secs = duration.toSecondsPart();
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%02d:%02d", minutes, secs);
    }

    private static World resolveGameWorld() {
        if (Setting.isNetherMode()) {
            for (World world : Bukkit.getWorlds()) {
                if (world.getEnvironment() == World.Environment.NETHER) {
                    return world;
                }
            }
            World fallback = Bukkit.getWorld("world_nether");
            if (fallback != null) {
                return fallback;
            }
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    private static World getActiveGameWorld() {
        if (activeGameWorld != null) {
            return activeGameWorld;
        }
        World resolved = resolveGameWorld();
        if (resolved == null) {
            return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        }
        return resolved;
    }

    public static void initChest() {
        redTeamChest.clear();
        blueTeamChest.clear();
        redTeamChestNameCache.clear();
        blueTeamChestNameCache.clear();

        int teamChestNum = Setting.getMaxTeamChestNum();
        for (int i = 0; i < teamChestNum; i++) {
            int index = i + 1;
            redTeamChest.add(Bukkit.createInventory(null, 6 * 9, getTeamChestTitle("red", index)));
            blueTeamChest.add(Bukkit.createInventory(null, 6 * 9, getTeamChestTitle("blue", index)));
        }
    }

    public static String getTeamChestDisplayName(String team, int index) {
        String custom = getCustomName(team, index, redTeamChestNameCache, blueTeamChestNameCache);
        if (custom != null) {
            return custom;
        }
        return getTeamChestTitle(team, index);
    }

    private static String getTeamChestTitle(String team, int index) {
        String custom = getCustomName(team, index, redTeamChestNameCache, blueTeamChestNameCache);
        if (custom != null) {
            return custom;
        }
        String base = "red".equals(team) ? Message.MENU_RED_CHEST.getString() : Message.MENU_BLUE_CHEST.getString();
        return base + index;
    }

    public static String getWaypointDisplayName(String team, int index, boolean filled) {
        String custom = getCustomName(team, index, redWaypointNameCache, blueWaypointNameCache);
        if (custom != null) {
            return custom;
        }
        String base = filled ? Message.MENU_WAYPOINT_FILLED.getString() : Message.MENU_WAYPOINT_EMPTY.getString();
        return base + index;
    }

    private static String getCustomName(String team, int index, Map<Integer, String> redCache, Map<Integer, String> blueCache) {
        String value = "red".equals(team) ? redCache.get(index) : blueCache.get(index);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    public static boolean hasRenameRequest(Player player) {
        if (player == null) {
            return false;
        }
        return renameRequests.containsKey(player.getName());
    }

    public static void clearRenameRequest(String playerName) {
        if (playerName != null) {
            renameRequests.remove(playerName);
        }
    }

    public static void beginTeamChestRename(Player player, int index) {
        String team = resolvePlayerTeam(player);
        if (team.isEmpty()) {
            return;
        }
        renameRequests.put(player.getName(), new RenameRequest(RenameTarget.TEAM_CHEST, team, index));
        player.sendMessage(Message.NOTICE_SET_TEAM_CHEST_NAME.getString().replace("%index%", String.valueOf(index)));
        player.closeInventory();
    }

    public static void beginWaypointRename(Player player, int index) {
        String team = resolvePlayerTeam(player);
        if (team.isEmpty()) {
            return;
        }
        renameRequests.put(player.getName(), new RenameRequest(RenameTarget.WAYPOINT, team, index));
        player.sendMessage(Message.NOTICE_SET_WAYPOINT_NAME.getString().replace("%index%", String.valueOf(index)));
        player.closeInventory();
    }

    public static void handleRenameInput(Player player, String input) {
        if (player == null || input == null) {
            return;
        }

        RenameRequest request = renameRequests.get(player.getName());
        if (request == null) {
            return;
        }

        String normalized = input.trim();
        if (normalized.equalsIgnoreCase("quit")) {
            renameRequests.remove(player.getName());
            player.sendMessage(Message.NOTICE_SET_CUSTOM_NAME_QUIT.getString());
            return;
        }

        boolean reset = normalized.equalsIgnoreCase("reset");
        String customName = reset ? null : sanitizeCustomName(normalized);
        if (!reset && customName == null) {
            player.sendMessage(Message.NOTICE_SET_CUSTOM_NAME_EMPTY.getString());
            return;
        }

        if (request.target == RenameTarget.TEAM_CHEST) {
            applyTeamChestName(request.team, request.index, customName);
            if (reset) {
                player.sendMessage(Message.NOTICE_RESET_TEAM_CHEST_NAME_SUCCESS.getString().replace("%index%", String.valueOf(request.index)));
            } else {
                player.sendMessage(Message.NOTICE_SET_TEAM_CHEST_NAME_SUCCESS.getString()
                        .replace("%index%", String.valueOf(request.index))
                        .replace("%name%", customName));
            }
        } else {
            applyWaypointName(request.team, request.index, customName);
            if (reset) {
                player.sendMessage(Message.NOTICE_RESET_WAYPOINT_NAME_SUCCESS.getString().replace("%index%", String.valueOf(request.index)));
            } else {
                player.sendMessage(Message.NOTICE_SET_WAYPOINT_NAME_SUCCESS.getString()
                        .replace("%index%", String.valueOf(request.index))
                        .replace("%name%", customName));
            }
        }

        renameRequests.remove(player.getName());
        updateScoreboard();
    }

    private static String sanitizeCustomName(String raw) {
        if (raw == null) {
            return null;
        }
        String sanitized = raw.replace('\n', ' ').replace('\r', ' ').trim();
        if (sanitized.isEmpty()) {
            return null;
        }
        if (sanitized.length() > 32) {
            sanitized = sanitized.substring(0, 32);
        }
        return ColorUtil.t(sanitized);
    }

    private static String resolvePlayerTeam(Player player) {
        if (player == null) {
            return "";
        }
        if (redTeamPlayers.contains(player.getName())) {
            return "red";
        }
        if (blueTeamPlayers.contains(player.getName())) {
            return "blue";
        }
        return "";
    }

    private static void applyWaypointName(String team, int index, String customName) {
        if ("red".equals(team)) {
            if (customName == null) {
                redWaypointNameCache.remove(index);
            } else {
                redWaypointNameCache.put(index, customName);
            }
        } else if ("blue".equals(team)) {
            if (customName == null) {
                blueWaypointNameCache.remove(index);
            } else {
                blueWaypointNameCache.put(index, customName);
            }
        }
    }

    private static void applyTeamChestName(String team, int index, String customName) {
        if ("red".equals(team)) {
            if (customName == null) {
                redTeamChestNameCache.remove(index);
            } else {
                redTeamChestNameCache.put(index, customName);
            }
        } else if ("blue".equals(team)) {
            if (customName == null) {
                blueTeamChestNameCache.remove(index);
            } else {
                blueTeamChestNameCache.put(index, customName);
            }
        }

        retitleTeamChestInventory(team, index);
    }

    private static void retitleTeamChestInventory(String team, int index) {
        ArrayList<Inventory> inventories = "red".equals(team) ? redTeamChest : blueTeamChest;
        int slot = index - 1;
        if (slot < 0 || slot >= inventories.size()) {
            return;
        }

        Inventory oldInventory = inventories.get(slot);
        Inventory newInventory = Bukkit.createInventory(null, oldInventory.getSize(), getTeamChestTitle(team, index));
        newInventory.setContents(oldInventory.getContents());
        inventories.set(slot, newInventory);
    }

    // Team spawn locations (shared by team members)
    public static org.bukkit.Location redTeamSpawn = null;
    public static org.bukkit.Location blueTeamSpawn = null;

    // Generate safe team spawns for both teams
    public static void generateTeamSpawns() {
        World world = getActiveGameWorld();
        if (world == null) {
            return;
        }

        redTeamSpawn = generateSafeSpawn(world);
        if (Setting.isSharedTeamSpawn()) {
            blueTeamSpawn = redTeamSpawn == null ? null : redTeamSpawn.clone();
            return;
        }

        blueTeamSpawn = generateSafeSpawn(world);
        // ensure spawns are not too close
        if (redTeamSpawn != null && blueTeamSpawn != null && redTeamSpawn.getWorld().equals(blueTeamSpawn.getWorld())) {
            if (redTeamSpawn.distance(blueTeamSpawn) < 10) {
                blueTeamSpawn.add(20, 0, 0);
            }
        }
    }

    private static org.bukkit.Location generateSafeSpawn(World world) {
        if (world.getEnvironment() == World.Environment.NETHER) {
            return findSafeNetherLocation(world, new Random());
        }

        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            return resolveSafeOverworldLocationFolia(world, true, 0, 5);
        }

        Random random = new Random();
        org.bukkit.Location loc;
        int attempts = 0;
        do {
            double randX = random.nextInt(20000) - 10000;
            double randZ = random.nextInt(20000) - 10000;
            loc = world.getHighestBlockAt(new org.bukkit.Location(world, randX, 0, randZ)).getLocation();
            loc.setY(loc.getY() + 1);
            attempts++;
            Biome biome = loc.getBlock().getBiome();
            boolean isOcean = biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.DEEP_COLD_OCEAN
                    || biome == Biome.LUKEWARM_OCEAN || biome == Biome.DEEP_FROZEN_OCEAN || biome == Biome.COLD_OCEAN
                    || biome == Biome.WARM_OCEAN || biome == Biome.DEEP_LUKEWARM_OCEAN || biome == Biome.FROZEN_OCEAN;
            if (!isOcean) break;
        } while (attempts < 30);
        return loc;
    }

    private static Location findSafeNetherLocation(World world, Random random) {
        if (world == null) {
            return null;
        }

        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            return resolveSafeNetherLocationFolia(world, 0, 5);
        }

        // Folia enforces region/thread ownership across worlds.
        // During game start/menu click we may not be on the Nether's region thread,
        // so scanning Nether blocks can throw IllegalStateException (world mismatch).
        // Use a stable fallback spawn in this case.
        int attempt = 0;
        int maxAttempts = 100;
        try {
            while (attempt++ < maxAttempts) {
                int randX = random.nextInt(20000) - 10000;
                int randZ = random.nextInt(20000) - 10000;
                int ceilingLimit = Math.min(world.getMaxHeight() - 5, 118);
                for (int y = ceilingLimit; y >= 20; y--) {
                    org.bukkit.block.Block floor = world.getBlockAt(randX, y, randZ);
                    if (!isSafeNetherFloor(world, floor)) {
                        continue;
                    }
                    org.bukkit.block.Block head = floor.getRelative(0, 1, 0);
                    org.bukkit.block.Block above = floor.getRelative(0, 2, 0);
                    if (!isSafeNetherAir(world, head) || !isSafeNetherAir(world, above)) {
                        continue;
                    }
                    Location candidate = head.getLocation().add(0.5, 0, 0.5);
                    if (candidate.getBlock().getType() == Material.LAVA) {
                        continue;
                    }
                    return candidate;
                }
            }
        } catch (IllegalStateException ex) {
            Bukkit.getLogger().warning("[BlockRacing] Nether spawn scan failed on current thread, falling back to world spawn: " + ex.getMessage());
        }

        return world.getSpawnLocation();
    }

    private static boolean isSafeNetherFloor(World expectedWorld, org.bukkit.block.Block block) {
        if (block == null || expectedWorld == null || block.getWorld() == null || !expectedWorld.equals(block.getWorld())) {
            return false;
        }
        Material type = block.getType();
        if (type == Material.LAVA || type == Material.BEDROCK || type == Material.MAGMA_BLOCK) {
            return false;
        }
        return type.isSolid();
    }

    private static boolean isSafeNetherAir(World expectedWorld, org.bukkit.block.Block block) {
        if (block == null || expectedWorld == null || block.getWorld() == null || !expectedWorld.equals(block.getWorld())) {
            return false;
        }
        Material type = block.getType();
        return type.isAir() || type == Material.CAVE_AIR;
    }

    public static void playerLogin(Player player) {
        Team.refreshPlayerListName(player);
        LanguageManager.registerFirstJoin(player);
        Scoreboard.showScoreboard(player);

        if (getCurrentGameState().equals(GameState.PREGAME)) {
            player.setGameMode(GameMode.ADVENTURE);
            player.sendMessage(Message.NOTICE_WELCOME.getString(player));
            top.lqsnow.blockracing.menus.LanguageMenu.sendFirstJoinPrompt(player);
            giveRuleBook(player);
            // Delay teleport by 1 tick to avoid conflict with player chunk loader initialization on Folia
            Main.getFoliaLib().getScheduler().runNextTick(task -> {
                teleportPlayer(player, Bukkit.getWorlds().get(0).getSpawnLocation());
            });
        } else if (getCurrentGameState().equals(GameState.INGAME)) {
            // Spectator
            if (!redTeamPlayers.contains(player.getName()) && !blueTeamPlayers.contains(player.getName())) {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(Message.NOTICE_SPECTATOR_JOIN.getString());
                return;
            }

            // Players who choose a team before the start of the game and exit, but enter
            // after the start of the game
            if (!inGamePlayers.contains(player.getName())) {
                initPlayer(player);
                freeRandomTPList.add(player.getName());
            }
        }

        VoicechatBridge.syncPlayer(player);
        checkUpdate(player);
    }

    private static void checkUpdate(Player player) {
        player.resetTitle();
        if (!Config.CONFIG_VERSION.getString().equals(Main.getInstance().getDescription().getVersion())
            || !Message.MESSAGE_VERSION.getString().equals(Main.getInstance().getDescription().getVersion())) {
            if (Message.NOTICE_VERSION_MISMATCH.getString() != null) {
                player.sendMessage(Message.NOTICE_VERSION_MISMATCH.getString());
                player.sendTitle(Message.NOTICE_VERSION_MISMATCH_TITLE.getString(),
                        Message.NOTICE_VERSION_MISMATCH_SUBTITLE.getString(), 0, 2000, 0);
            } else {
                player.sendMessage(ColorUtil.t(
                    "&cWarning! The current file versions of your config.yml and language file do not correspond to the plugin version! You may have updated the plugin, but did not update the configuration file! This may lead to some unexpected errors! You can delete the configuration files in the \\plugins\\BlockRacing folder, and then restart the server, or download the latest version of the configuration files on GitHub to replace them!"));
                player.sendTitle(ColorUtil.t("&cWarning! Version Mismatch!"),
                        ColorUtil.t("&cPlease check the specific information in the chat!"), 20, 0, 0);
            }
            Bukkit.getLogger().severe(Message.NOTICE_VERSION_MISMATCH.getString());
        }
    }

    public static void playerQuit(Player player) {
        redRollPlayers.remove(player.getName());
        blueRollPlayers.remove(player.getName());
        readyPlayers.remove(player.getName());
        editAmountPlayer.remove(player.getName());
    }

    public static void playerReady(Player player) {
        if (!readyPlayers.contains(player.getName())) {
            readyPlayers.add(player.getName());
            sendAll(Message.NOTICE_READY.getString().replace("%player%", player.getName()));
            // Check if the game can start, just notice players
            if (readyPlayers.size() > 1 && readyPlayers.size() == Bukkit.getOnlinePlayers().size()) {
                sendAll(Message.NOTICE_ALL_READY.getString());
            }
        } else {
            readyPlayers.remove(player.getName());
            sendAll(Message.NOTICE_CANCEL_READY.getString().replace("%player%", player.getName()));
        }
    }

    // Check if the game can start. If not, send the reason to the player; if
    // possible, start the game directly
    public static void checkStartDemands(Player player) {

        // Game already start
        if (getCurrentGameState().equals(GameState.INGAME))
            return;

        // Not enough players
        if (!(Bukkit.getOnlinePlayers().size() > 1)) {
            player.sendMessage(Message.NOTICE_NOT_ENOUGH_PLAYERS.getString());
            return;
        }

        // Exist unready players
        if (!(readyPlayers.size() == Bukkit.getOnlinePlayers().size())) {
            List<String> unreadyPlayers = getOnlinePlayersString();
            unreadyPlayers.removeAll(readyPlayers);
            player.sendMessage(Message.NOTICE_EXIST_UNREADY.getString());
            player.sendMessage(Message.NOTICE_UNREADY_PLAYERS.getString() + unreadyPlayers);
            return;
        }

        // Exist empty team
        if (redTeamPlayers.isEmpty() || blueTeamPlayers.isEmpty()) {
            player.sendMessage(Message.NOTICE_EMPTY_TEAM.getString());
            return;
        }

        // Blocks have problems
        if (!checkBlock()) {
            return;
        }

        // Start the game
        sendAll(Message.NOTICE_START.getString());
        startGame();
    }

    // Force start by OP, ignoring ready-state checks.
    public static void forceStart(CommandSender sender) {

        // Game already start
        if (getCurrentGameState().equals(GameState.INGAME)) {
            return;
        }

        // Not enough players
        if (!(Bukkit.getOnlinePlayers().size() > 1)) {
            sender.sendMessage(Message.NOTICE_NOT_ENOUGH_PLAYERS.getString());
            return;
        }

        // Exist empty team
        if (redTeamPlayers.isEmpty() || blueTeamPlayers.isEmpty()) {
            sender.sendMessage(Message.NOTICE_EMPTY_TEAM.getString());
            return;
        }

        // Blocks have problems
        if (!checkBlock()) {
            return;
        }

        // Start the game
        sendAll(Message.NOTICE_START.getString());
        startGame();
    }

    private static void prepareTimeModeState() {
        stopTimeModeCountdown();
        if (isTimeModeActive()) {
            timeModeDurationSeconds = Math.max(60, Setting.getTimeModeDurationSeconds());
            timeModeRemainingSeconds = timeModeDurationSeconds;
        } else {
            timeModeDurationSeconds = 0;
            timeModeRemainingSeconds = 0;
        }
        timeModeOvertime = false;
    }

    private static void startTimeModeCountdownIfNeeded() {
        if (!isTimeModeActive()) {
            return;
        }
        stopTimeModeCountdown();
        timeModeTask = Main.getFoliaLib().getScheduler().runTimer(() -> {
            if (!getCurrentGameState().equals(GameState.INGAME)) {
                stopTimeModeCountdown();
                return;
            }
            if (timeModeOvertime) {
                updateScoreboard();
                return;
            }
            if (timeModeRemainingSeconds <= 0) {
                handleTimeModeCountdownFinished();
                return;
            }
            timeModeRemainingSeconds--;
            updateScoreboard();
            if (timeModeRemainingSeconds == 0) {
                handleTimeModeCountdownFinished();
            }
        }, 20L, 20L);
    }

    private static void stopTimeModeCountdown() {
        if (timeModeTask != null) {
            timeModeTask.cancel();
            timeModeTask = null;
        }
    }

    private static void handleTimeModeCountdownFinished() {
        if (!isTimeModeActive() || timeModeOvertime || !getCurrentGameState().equals(GameState.INGAME)) {
            return;
        }
        // Compare collected block counts in time mode rather than time-weighted scores
        if (redTeamCurrentBlockAmount > blueTeamCurrentBlockAmount) {
            redWin();
            showRanking();
        } else if (blueTeamCurrentBlockAmount > redTeamCurrentBlockAmount) {
            blueWin();
            showRanking();
        } else {
            beginTimeModeOvertime();
        }
    }

    private static void beginTimeModeOvertime() {
        if (timeModeOvertime) {
            return;
        }
        timeModeOvertime = true;
        timeModeRemainingSeconds = 0;
        sendAll(Message.NOTICE_OVERTIME_START.getString());
        updateScoreboard();
    }

    private static void finalizeOvertimeWin(boolean redTeam) {
        if (!isTimeModeActive() || !timeModeOvertime || !getCurrentGameState().equals(GameState.INGAME)) {
            return;
        }
        if (redTeam) {
            redWin();
        } else {
            blueWin();
        }
        showRanking();
    }

    public static void startGame() {
        // Init
        setCurrentGameState(GameState.INGAME);
        closeAllPlayersMenu();
        editAmountPlayer.clear();
        
        // Reset roll counts and players for all game modes
        redTeamRollCount = 0;
        blueTeamRollCount = 0;
        contestModeRollCount = 0;
        redRollPlayers.clear();
        blueRollPlayers.clear();
        contestModeRollPlayers.clear();
        
        setupBlocks();
        switch (Setting.getCurrentGameMode()) {
            case TIME -> {
                // In time mode we don't predefine ordered block lists; use pool size as total
                redTeamTotalBlockAmount = Block.blocks.size();
                blueTeamTotalBlockAmount = Block.blocks.size();
                prepareTimeModeState();
            }
            case CONTEST -> {
                redTeamTotalBlockAmount = Setting.getBlockAmount();
                blueTeamTotalBlockAmount = Setting.getBlockAmount();
            }
            default -> {
                redTeamTotalBlockAmount = redTeamBlocks.size();
                blueTeamTotalBlockAmount = blueTeamBlocks.size();
            }
        }
        setLocateScore();
        updateScoreboard();
        Bukkit.getOnlinePlayers().forEach((Player player) -> freeRandomTPList.add(player.getName()));
        startInGameLoop();
        activeGameWorld = resolveGameWorld();
        World world = getActiveGameWorld();
        if (world == null) {
            world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        }
        if (world == null) {
            Bukkit.getLogger().severe("[BlockRacing] No world available for teleport!");
            return;
        }
        applyGlobalWorldState(world);
        world.getEntities().stream().filter(e -> e instanceof Item).forEach(Entity::remove);

        // Generate shared team spawns for this game
        generateTeamSpawns();

        // Processing of unselected team players (spectators)
        inGamePlayers.addAll(getOnlinePlayersString());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!redTeamPlayers.contains(player.getName()) && !blueTeamPlayers.contains(player.getName())) {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(Message.NOTICE_SPECTATOR.getString());
                inGamePlayers.remove(player.getName());
            }
        }

        // Settings for each player
        for (String p : inGamePlayers) {
            // General
            Player player = Bukkit.getPlayer(p);
            initPlayer(player);
        }

        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            Main.getFoliaLib().getScheduler().runLater(Game::refreshComebackEffects, 5L);
        } else {
            refreshComebackEffects();
        }

        VoicechatBridge.syncAllPlayers();

        Bukkit.getLogger().info("Red team players: " + redTeamPlayers.toString());
        Bukkit.getLogger().info("Blue team players: " + blueTeamPlayers.toString());
        if (Setting.getCurrentGameMode().equals(Setting.GameMode.NORMAL))
            Bukkit.getLogger().info("Game mode: Normal");
        else if (Setting.getCurrentGameMode().equals(Setting.GameMode.RACING))
            Bukkit.getLogger().info("Game mode: Racing");
        else if (Setting.getCurrentGameMode().equals(Setting.GameMode.CONTEST))
            Bukkit.getLogger().info("Game mode: Contest");
        else if (Setting.getCurrentGameMode().equals(Setting.GameMode.TIME))
            Bukkit.getLogger().info("Game mode: Time");
        Bukkit.getLogger().info(Setting.isSpeedMode() ? "Speed mode: On" : "Speed mode: Off");
        Bukkit.getLogger().info(Setting.isNetherMode() ? "Nether mode: On" : "Nether mode: Off");
        startTimeModeCountdownIfNeeded();
    }

    private static void applyGlobalWorldState(World world) {
        // Apply KEEP_INVENTORY to all loaded worlds (overworld, nether, end)
        // On Folia, Bukkit.getWorlds() must not be called directly from event handlers
        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            Main.getFoliaLib().getScheduler().runNextTick(task -> {
                for (World w : Bukkit.getWorlds()) {
                    w.setGameRule(GameRules.KEEP_INVENTORY, true);
                }
            });
        } else {
            for (World w : Bukkit.getWorlds()) {
                w.setGameRule(GameRules.KEEP_INVENTORY, true);
            }
        }

        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            Main.getFoliaLib().getScheduler().runNextTick(task -> {
                world.setDifficulty(Difficulty.EASY);
                world.setTime(1000);
                world.setStorm(false);
                world.setThundering(false);
                world.getWorldBorder().setCenter(world.getSpawnLocation());
                world.getWorldBorder().setSize(59999968);
            });
            return;
        }

        world.setDifficulty(Difficulty.EASY);
        world.setTime(1000);
        world.setStorm(false);
        world.setThundering(false);
        world.getWorldBorder().setCenter(world.getSpawnLocation());
        world.getWorldBorder().setSize(59999968);
    }

    // Player init
    public static void initPlayer(Player player) {
        if (player == null) {
            return;
        }
        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            Main.getFoliaLib().getScheduler().runAtLocation(player.getLocation(), task -> initPlayerInternal(player));
            return;
        }
        initPlayerInternal(player);
    }

    private static void initPlayerInternal(Player player) {
        // General
        player.getInventory().clear();
        // Teleport to team spawn if available, otherwise random teleport
        org.bukkit.Location spawn = null;
        if (redTeamPlayers.contains(player.getName()) && redTeamSpawn != null) spawn = redTeamSpawn;
        else if (blueTeamPlayers.contains(player.getName()) && blueTeamSpawn != null) spawn = blueTeamSpawn;
        if (spawn != null) teleportPlayer(player, spawn);
        else randomTeleport(player, true);
        applyTeamRespawnLocation(player);
        player.setHealth(20);
        player.setExp(0);
        player.setLevel(0);
        player.setFoodLevel(20);
        player.setSaturation(10);
        player.setGameMode(GameMode.SURVIVAL);
        List<ItemStack> openingItems = Setting.isSpeedMode() ? Setting.getSpeedModeItems() : Setting.getBaseModeItems();
        for (ItemStack stack : openingItems) {
            if (stack == null) {
                continue;
            }
            player.getInventory().addItem(stack.clone());
        }
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            Main.getFoliaLib().getScheduler().runAtEntityLater(player,
                    () -> applyInitialPotionEffects(player), 4L);
        } else {
            applyInitialPotionEffects(player);
        }
        if (Setting.isSpeedMode()) {
            if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
                Main.getFoliaLib().getScheduler().runAtEntityLater(player,
                        () -> applyConfiguredSpeedModeEffects(player), 6L);
            } else {
                applyConfiguredSpeedModeEffects(player);
            }
        }
    }

    private static void applyInitialPotionEffects(Player player) {
        if (player == null) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 1200, 4, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1200, 4, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false));
    }

    // Roll
    public static void roll(Player player) {
        if (isContestModeActive()) {
            // 争夺模式：全场轮换，只有一次机会
            if (contestModeRollCount >= 1) {
                player.sendMessage(Message.NOTICE_CANNOT_ROLL_CONTEST.getString());
                return;
            }
            if (!contestModeRollPlayers.contains(player.getName())) {
                contestModeRollPlayers.add(player.getName());
                sendAll(Message.NOTICE_ROLL_REQUEST_CONTEST.getString().replace("%player%", player.getName()));
            } else {
                contestModeRollPlayers.remove(player.getName());
                sendAll(Message.NOTICE_ROLL_REQUEST_CANCEL.getString().replace("%player%", player.getName()));
            }
        } else {
            // 其他模式：队伍轮换，每队三次
            if (redTeamPlayers.contains(player.getName())) {
                List<String> candidates = new ArrayList<>(blocks);
                candidates.removeAll(redTeamBlocks);
                if (candidates.isEmpty()) {
                    player.sendMessage(Message.NOTICE_CANNOT_ROLL_NO_CANDIDATE.getString());
                    return;
                }
                if (redTeamRollCount >= Setting.getMaxRollCount()) {
                    player.sendMessage(Message.NOTICE_CANNOT_ROLL.getString());
                    return;
                }
                if (!redRollPlayers.contains(player.getName())) {
                    redRollPlayers.add(player.getName());
                    sendRed(Message.NOTICE_ROLL_REQUEST.getString().replace("%player%", player.getName()));
                } else {
                    redRollPlayers.remove(player.getName());
                    sendRed(Message.NOTICE_ROLL_REQUEST_CANCEL.getString().replace("%player%", player.getName()));
                }
            } else if (blueTeamPlayers.contains(player.getName())) {
                List<String> candidates = new ArrayList<>(blocks);
                candidates.removeAll(blueTeamBlocks);
                if (candidates.isEmpty()) {
                    player.sendMessage(Message.NOTICE_CANNOT_ROLL_NO_CANDIDATE.getString());
                    return;
                }
                if (blueTeamRollCount >= Setting.getMaxRollCount()) {
                    player.sendMessage(Message.NOTICE_CANNOT_ROLL.getString());
                    return;
                }
                if (!blueRollPlayers.contains(player.getName())) {
                    blueRollPlayers.add(player.getName());
                    sendBlue(Message.NOTICE_ROLL_REQUEST.getString().replace("%player%", player.getName()));
                } else {
                    blueRollPlayers.remove(player.getName());
                    sendBlue(Message.NOTICE_ROLL_REQUEST_CANCEL.getString().replace("%player%", player.getName()));
                }
            }
        }
    }

    public static void locate(Player player) {
        player.sendMessage(Message.NOTICE_LOCATE_COMMAND_HINT.getString().replace("%score%", String.valueOf(locateCost)));
    }

    public static boolean canAffordLocate(Player player) {
        if (redTeamPlayers.contains(player.getName())) {
            if (redTeamScore < locateCost) {
                player.sendMessage(Message.NOTICE_LOCATE_NOT_ENOUGH_SCORE.getString().replace("%score%", String.valueOf(locateCost)));
                return false;
            }
            return true;
        } else if (blueTeamPlayers.contains(player.getName())) {
            if (blueTeamScore < locateCost) {
                player.sendMessage(Message.NOTICE_LOCATE_NOT_ENOUGH_SCORE.getString().replace("%score%", String.valueOf(locateCost)));
                return false;
            }
            return true;
        }

        return false;
    }

    public static boolean chargeLocateCost(Player player) {
        if (redTeamPlayers.contains(player.getName())) {
            if (redTeamScore < locateCost) {
                player.sendMessage(Message.NOTICE_LOCATE_NOT_ENOUGH_SCORE.getString().replace("%score%", String.valueOf(locateCost)));
                return false;
            }
            redTeamScore -= locateCost;
        } else if (blueTeamPlayers.contains(player.getName())) {
            if (blueTeamScore < locateCost) {
                player.sendMessage(Message.NOTICE_LOCATE_NOT_ENOUGH_SCORE.getString().replace("%score%", String.valueOf(locateCost)));
                return false;
            }
            blueTeamScore -= locateCost;
        } else {
            return false;
        }

        updateScoreboard();
        return true;
    }

    // Random Teleport
    public static void randomTeleport(Player player, boolean avoidOcean) {
        World world = getActiveGameWorld();
        if (world == null) {
            return;
        }

        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            if (Setting.isNetherMode() && world.getEnvironment() == World.Environment.NETHER) {
                randomTeleportFoliaNether(player, world, 0);
            } else {
                randomTeleportFolia(player, world, avoidOcean, 0);
            }
            return;
        }

        // Paper async chunk API: avoid blocking the main thread
        if (Setting.isNetherMode() && world.getEnvironment() == World.Environment.NETHER) {
            randomTeleportAsyncNether(player, world, 0);
        } else {
            randomTeleportAsyncOverworld(player, world, avoidOcean, 0);
        }
    }

    private static void randomTeleportAsyncOverworld(Player player, World world, boolean avoidOcean, int attempts) {
        if (player == null || !player.isOnline()) return;
        final int maxAttempts = 60;
        if (attempts >= maxAttempts) {
            Location fallback = world.getSpawnLocation().clone().add(0, 1, 0);
            teleportPlayer(player, fallback);
            applyPostTeleportProtection(player);
            sendTeleportSuccessMessage(player, fallback);
            return;
        }
        Random random = new Random();
        int randX = random.nextInt(20000) - 10000;
        int randZ = random.nextInt(20000) - 10000;
        int chunkX = randX >> 4;
        int chunkZ = randZ >> 4;
        world.getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
            if (!player.isOnline()) return;
            int blockX = ((randX % 16) + 16) % 16;
            int blockZ = ((randZ % 16) + 16) % 16;
            org.bukkit.ChunkSnapshot snapshot = chunk.getChunkSnapshot();
            int highestY = snapshot.getHighestBlockYAt(blockX, blockZ);
            Location loc = new Location(world, randX + 0.5, highestY + 1, randZ + 0.5);
            if (avoidOcean && isOceanBiome(loc.getBlock().getBiome())) {
                randomTeleportAsyncOverworld(player, world, avoidOcean, attempts + 1);
                return;
            }
            teleportPlayer(player, loc);
            applyPostTeleportProtection(player);
            sendTeleportSuccessMessage(player, loc);
        }).exceptionally(ex -> {
            Main.getInstance().getLogger().warning("[BlockRacing] Async chunk load failed for teleport: " + ex.getMessage());
            return null;
        });
    }

    private static void randomTeleportAsyncNether(Player player, World world, int attempts) {
        if (player == null || !player.isOnline()) return;
        final int maxAttempts = 60;
        if (attempts >= maxAttempts) {
            Location fallback = world.getSpawnLocation().clone().add(0, 1, 0);
            teleportPlayer(player, fallback);
            applyPostTeleportProtection(player);
            sendTeleportSuccessMessage(player, fallback);
            return;
        }
        Random random = new Random();
        int randX = random.nextInt(20000) - 10000;
        int randZ = random.nextInt(20000) - 10000;
        int chunkX = randX >> 4;
        int chunkZ = randZ >> 4;
        world.getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
            if (!player.isOnline()) return;
            int ceilingLimit = Math.min(world.getMaxHeight() - 5, 118);
            for (int y = ceilingLimit; y >= 20; y--) {
                org.bukkit.block.Block floor = world.getBlockAt(randX, y, randZ);
                if (!isSafeNetherFloor(world, floor)) continue;
                org.bukkit.block.Block head = floor.getRelative(0, 1, 0);
                org.bukkit.block.Block above = floor.getRelative(0, 2, 0);
                if (!isSafeNetherAir(world, head) || !isSafeNetherAir(world, above)) continue;
                Location candidate = head.getLocation().add(0.5, 0, 0.5);
                if (candidate.getBlock().getType() == Material.LAVA) continue;
                teleportPlayer(player, candidate);
                applyPostTeleportProtection(player);
                sendTeleportSuccessMessage(player, candidate);
                return;
            }
            // No safe spot in this chunk, try another
            randomTeleportAsyncNether(player, world, attempts + 1);
        }).exceptionally(ex -> {
            Main.getInstance().getLogger().warning("[BlockRacing] Async chunk load failed for nether teleport: " + ex.getMessage());
            return null;
        });
    }

    private static void randomTeleportFolia(Player player, World world, boolean avoidOcean, int attempts) {
        if (player == null || world == null) {
            return;
        }

        final int maxAttempts = 60;
        if (attempts >= maxAttempts) {
            Location fallback = world.getSpawnLocation().clone().add(0, 1, 0);
            teleportPlayer(player, fallback);
            applyPostTeleportProtection(player);
            sendTeleportSuccessMessage(player, fallback);
            return;
        }

        Location seed = pickOverworldTeleportSeed(world, attempts == 0);
        Main.getFoliaLib().getScheduler().runAtLocationLater(seed, () -> {
            Location safe = resolveOverworldCandidate(world, seed, avoidOcean);
            if (safe == null) {
                randomTeleportFolia(player, world, avoidOcean, attempts + 1);
                return;
            }

            if (avoidOcean && isOceanBiome(safe.getBlock().getBiome())) {
                if (attempts == 0 && Message.NOTICE_TP_OCEAN.getString() != null) {
                    sendPlayerMessage(player, Message.NOTICE_TP_OCEAN.getString());
                }
                randomTeleportFolia(player, world, avoidOcean, attempts + 1);
                return;
            }

            teleportPlayer(player, safe);
            applyPostTeleportProtection(player);
            sendTeleportSuccessMessage(player, safe);
        }, 1L);
    }

    private static void randomTeleportFoliaNether(Player player, World world, int attempts) {
        if (player == null || world == null) {
            return;
        }

        final int maxAttempts = 100;
        if (attempts >= maxAttempts) {
            Location fallback = world.getSpawnLocation().clone().add(0, 1, 0);
            teleportPlayer(player, fallback);
            applyPostTeleportProtection(player);
            sendTeleportSuccessMessage(player, fallback);
            return;
        }

        Location seed = pickNetherTeleportSeed(world);
        Main.getFoliaLib().getScheduler().runAtLocationLater(seed, () -> {
            Location safe = resolveNetherCandidate(world, seed);
            if (safe == null) {
                randomTeleportFoliaNether(player, world, attempts + 1);
                return;
            }

            teleportPlayer(player, safe);
            applyPostTeleportProtection(player);
            sendTeleportSuccessMessage(player, safe);
        }, 1L);
    }

    private static Location pickOverworldTeleportSeed(World world, boolean preferCandidatePool) {
        if (preferCandidatePool) {
            Location candidate = pollRandomTeleportCandidate();
            if (candidate != null) {
                return new Location(world, candidate.getX(), world.getMinHeight(), candidate.getZ());
            }
        }

        Random random = new Random();
        double randX = random.nextInt(20000) - 10000;
        double randZ = random.nextInt(20000) - 10000;
        return new Location(world, randX, world.getMinHeight(), randZ);
    }

    private static Location pickNetherTeleportSeed(World world) {
        Random random = new Random();
        double randX = random.nextInt(20000) - 10000;
        double randZ = random.nextInt(20000) - 10000;
        return new Location(world, randX, world.getMinHeight(), randZ);
    }

    private static Location resolveOverworldCandidate(World world, Location seed, boolean avoidOcean) {
        Location loc = world.getHighestBlockAt(new Location(world, seed.getBlockX(), 0, seed.getBlockZ())).getLocation();
        loc.setY(loc.getY() + 1);
        if (avoidOcean && isOceanBiome(loc.getBlock().getBiome())) {
            return null;
        }
        return loc;
    }

    private static Location resolveNetherCandidate(World world, Location seed) {
        int ceilingLimit = Math.min(world.getMaxHeight() - 5, 118);
        for (int y = ceilingLimit; y >= 20; y--) {
            org.bukkit.block.Block floor = world.getBlockAt(seed.getBlockX(), y, seed.getBlockZ());
            if (!isSafeNetherFloor(world, floor)) {
                continue;
            }
            org.bukkit.block.Block head = floor.getRelative(0, 1, 0);
            org.bukkit.block.Block above = floor.getRelative(0, 2, 0);
            if (!isSafeNetherAir(world, head) || !isSafeNetherAir(world, above)) {
                continue;
            }
            Location candidate = head.getLocation().add(0.5, 0, 0.5);
            if (candidate.getBlock().getType() == Material.LAVA) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private static Location resolveSafeOverworldLocationFolia(World world, boolean avoidOcean, int attempt, int maxAttempts) {
        if (world == null) {
            return null;
        }
        if (attempt >= maxAttempts) {
            return world.getSpawnLocation().clone().add(0, 1, 0);
        }

        CompletableFuture<Location> future = new CompletableFuture<>();
        Location seed = pickOverworldTeleportSeed(world, attempt == 0);
        Main.getFoliaLib().getScheduler().runAtLocationLater(seed, () -> {
            try {
                future.complete(resolveOverworldCandidate(world, seed, avoidOcean));
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        }, 1L);

        try {
            Location location = future.get(5, TimeUnit.SECONDS);
            if (location != null) {
                return location;
            }
        } catch (Exception ignored) {
        }
        return resolveSafeOverworldLocationFolia(world, avoidOcean, attempt + 1, maxAttempts);
    }

    private static Location resolveSafeNetherLocationFolia(World world, int attempt, int maxAttempts) {
        if (world == null) {
            return null;
        }
        if (attempt >= maxAttempts) {
            return world.getSpawnLocation().clone().add(0, 1, 0);
        }

        CompletableFuture<Location> future = new CompletableFuture<>();
        Location seed = pickNetherTeleportSeed(world);
        Main.getFoliaLib().getScheduler().runAtLocationLater(seed, () -> {
            try {
                future.complete(resolveNetherCandidate(world, seed));
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        }, 1L);

        try {
            Location location = future.get(5, TimeUnit.SECONDS);
            if (location != null) {
                return location;
            }
        } catch (Exception ignored) {
        }
        return resolveSafeNetherLocationFolia(world, attempt + 1, maxAttempts);
    }

    private static Location findSafeOverworldLocation(World world, boolean avoidOcean) {
        // Try to use a candidate from the pool first
        Location candidate = pollRandomTeleportCandidate();
        if (candidate != null) {
            double randX = candidate.getX();
            double randZ = candidate.getZ();
            Location loc = world.getHighestBlockAt(new Location(world, randX, 0, randZ)).getLocation();
            loc.setY(loc.getY() + 1);
            if (!avoidOcean || !isOceanBiome(loc.getBlock().getBiome())) {
                return loc;
            }
        }

        // Fall back to random location generation
        Random random = new Random();
        Location fallback = world.getSpawnLocation();
        for (int attempts = 0; attempts < 60; attempts++) {
            double randX = random.nextInt(20000) - 10000;
            double randZ = random.nextInt(20000) - 10000;
            Location loc = world.getHighestBlockAt(new Location(world, randX, 0, randZ)).getLocation();
            loc.setY(loc.getY() + 1);
            if (avoidOcean && isOceanBiome(loc.getBlock().getBiome())) {
                continue;
            }
            return loc;
        }
        return fallback;
    }

    public static boolean isOceanBiome(Biome biome) {
        return biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.DEEP_COLD_OCEAN
                || biome == Biome.LUKEWARM_OCEAN || biome == Biome.DEEP_FROZEN_OCEAN || biome == Biome.COLD_OCEAN
                || biome == Biome.WARM_OCEAN || biome == Biome.DEEP_LUKEWARM_OCEAN || biome == Biome.FROZEN_OCEAN;
    }

    private static Location randomFoliaLocation(World world, int radius) {
        Random random = new Random();
        double x = random.nextInt(radius * 2 + 1) - radius;
        double z = random.nextInt(radius * 2 + 1) - radius;
        double y = world.getSpawnLocation().getY() + 1.0;
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    private static void applyPostTeleportProtection(Player player) {
        if (player == null) {
            return;
        }

        Runnable action = () -> {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 15, 1, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 8, 1, false, false));
        };

        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            Main.getFoliaLib().getScheduler().runAtEntityLater(player, action, 3L);
        } else {
            action.run();
        }
    }

    private static void sendTeleportSuccessMessage(Player player, Location offset) {
        String x = String.format("%.1f", offset.getX());
        String y = String.format("%.1f", offset.getY());
        String z = String.format("%.1f", offset.getZ());
        sendPlayerMessage(player, Message.NOTICE_TP_SUCCESS.getString().replace("%x%", x).replace("%y%", y).replace("%z%", z));
    }

    private static void sendPlayerMessage(Player player, String message) {
        if (player == null || message == null) {
            return;
        }
        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            Main.getFoliaLib().getScheduler().runAtEntity(player, task -> player.sendMessage(message));
            return;
        }
        player.sendMessage(message);
    }

    public static void applyTeamRespawnLocation(Player player) {
        if (player == null) {
            return;
        }

        Location spawn = null;
        if (redTeamPlayers.contains(player.getName())) {
            spawn = redTeamSpawn;
        } else if (blueTeamPlayers.contains(player.getName())) {
            spawn = blueTeamSpawn;
        }

        if (spawn == null) {
            return;
        }

        Location respawnLocation = spawn.clone();
        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            Main.getFoliaLib().getScheduler().runAtEntity(player, task -> player.setRespawnLocation(respawnLocation, true));
            return;
        }

        player.setRespawnLocation(respawnLocation, true);
    }

    public static synchronized void addRandomTeleportCandidate(Location location) {
        if (location != null) {
            randomTpPool.addLast(location);
        }
    }

    public static synchronized Location pollRandomTeleportCandidate() {
        return randomTpPool.pollFirst();
    }

    public static synchronized int getRandomTeleportPoolSize() {
        return randomTpPool.size();
    }

    public static synchronized List<Location> getRandomTeleportPoolSnapshot() {
        return List.copyOf(randomTpPool);
    }

    // Waypoints
    // Return value: true -> waypoint changed, false -> waypoint doesn't change
    public static boolean waypoint(Player player, int index, ClickType clickType) {
        String team = redTeamPlayers.contains(player.getName()) ? "red"
                : (blueTeamPlayers.contains(player.getName()) ? "blue" : "");

        if (!team.isEmpty()) {
            Location waypoint = getWaypoint(team, index);
            if (clickType.isShiftClick() && clickType.isLeftClick() && waypoint != null) {
                if (removeWaypoint(team, index)) {
                    if ("red".equals(team)) {
                        sendRed(Message.NOTICE_RED_REMOVE_WAYPOINT.getString().replace("%player%", player.getName()).replace("%index%", String.valueOf(index)));
                    } else {
                        sendBlue(Message.NOTICE_BLUE_REMOVE_WAYPOINT.getString().replace("%player%", player.getName()).replace("%index%", String.valueOf(index)));
                    }
                    return true;
                }
                return false;
            }

            if (clickType.isLeftClick()) {
                if (waypoint == null) {
                    setWaypoint(player, team, index);
                    return true;
                }
                teleportPlayer(player, waypoint);
                String x = String.format("%.1f", waypoint.getX());
                String y = String.format("%.1f", waypoint.getY());
                String z = String.format("%.1f", waypoint.getZ());

                player.sendMessage(Message.NOTICE_TP_SUCCESS.getString().replace("%x%", x).replace("%y%", y)
                        .replace("%z%", z));
                return false;
            }

            if (clickType.isRightClick()) {
                if (waypoint != null) {
                    beginWaypointRename(player, index);
                }
            }
        }
        return false;
    }

    public static Location getWaypoint(String team, int index) {
        return switch (team) {
            case "red" -> redWaypoint.get(index);
            case "blue" -> blueWaypoint.get(index);
            default -> null;
        };
    }

    public static String getWaypointBiome(String team, int index) {
        return switch (team) {
            case "red" -> redWaypointBiomeCache.getOrDefault(index, "N/A");
            case "blue" -> blueWaypointBiomeCache.getOrDefault(index, "N/A");
            default -> "N/A";
        };
    }

    public static Material getWaypointIcon(String team, int index) {
        Material icon = switch (team) {
            case "red" -> redWaypointIconCache.get(index);
            case "blue" -> blueWaypointIconCache.get(index);
            default -> null;
        };
        // 记录的图标材质可能不是有效物品（例如水下记录时可能是 WATER/LAVA），
        // 必须回退为环境图标，否则 new ItemStack(...) 会抛 "isn't an item"。
        if (icon != null && icon.isItem()) {
            return icon;
        }
        return fallbackWaypointIcon(team, index);
    }

    private static Material fallbackWaypointIcon(String team, int index) {
        Location waypoint = getWaypoint(team, index);
        if (waypoint == null || waypoint.getWorld() == null) {
            return Material.FILLED_MAP;
        }
        return switch (waypoint.getWorld().getEnvironment()) {
            case NORMAL -> Material.GRASS_BLOCK;
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            default -> Material.FILLED_MAP;
        };
    }

    public static void setWaypoint(Player player, String team, int index) {
        Location waypoint = player.getLocation();
        String biomeLabel = "N/A";
        Material icon = resolveWaypointIconAtRecord(waypoint);
        try {
            biomeLabel = BiomeTranslation.getValue(waypoint.getBlock().getBiome());
        } catch (Exception ignored) {
            // Biome lookup can fail (Folia thread-restricted world access, unloaded region, etc.).
            // The waypoint must still be recorded so the waypoint menu stays usable.
        }

        switch (team) {
            case "red" -> {
                redWaypoint.put(index, waypoint);
                redWaypointBiomeCache.put(index, biomeLabel);
                redWaypointIconCache.put(index, icon);
            }
            case "blue" -> {
                blueWaypoint.put(index, waypoint);
                blueWaypointBiomeCache.put(index, biomeLabel);
                blueWaypointIconCache.put(index, icon);
            }
        }
    }

    /**
     * Dialog 专用：按编号直接设置传送点（不使用点击语义）。
     *
     * <p>Dialog API 不支持 shift+左键 / 左右键区分操作，因此不复用
     * {@link #waypoint(Player, int, ClickType)} 的 ClickType 分支。</p>
     */
    public static void setWaypoint(Player player, int index) {
        String team = resolvePlayerTeam(player);
        if (team.isEmpty()) {
            return;
        }
        setWaypoint(player, team, index);
    }

    private static Material resolveWaypointIconAtRecord(Location waypoint) {
        if (waypoint == null || waypoint.getWorld() == null) {
            return Material.FILLED_MAP;
        }

        try {
            org.bukkit.block.Block block = waypoint.getBlock();
            // 跳过空气与流体（水/岩浆），避免记录到 WATER/LAVA 等非物品材质。
            while ((block.isEmpty() || block.isLiquid()) && block.getY() > waypoint.getWorld().getMinHeight()) {
                block = block.getRelative(0, -1, 0);
            }

            if (!block.isEmpty()) {
                Material material = block.getType();
                if (material != null && material.isItem()) {
                    return material;
                }
            }
        } catch (Exception ignored) {
            // Fallback to environment icon when block material cannot be resolved safely.
        }

        return switch (waypoint.getWorld().getEnvironment()) {
            case NORMAL -> Material.GRASS_BLOCK;
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            default -> Material.FILLED_MAP;
        };
    }

    public static boolean removeWaypoint(String team, int index) {
        return switch (team) {
            case "red" -> {
                redWaypointIconCache.remove(index);
                redWaypointBiomeCache.remove(index);
                redWaypointNameCache.remove(index);
                yield redWaypoint.remove(index) != null;
            }
            case "blue" -> {
                blueWaypointIconCache.remove(index);
                blueWaypointBiomeCache.remove(index);
                blueWaypointNameCache.remove(index);
                yield blueWaypoint.remove(index) != null;
            }
            default -> false;
        };
    }

    /**
     * Dialog 专用：按编号直接删除传送点并广播（不使用点击语义）。
     *
     * @return 是否成功删除
     */
    public static boolean removeWaypoint(Player player, int index) {
        String team = resolvePlayerTeam(player);
        if (team.isEmpty()) {
            return false;
        }
        if (removeWaypoint(team, index)) {
            if ("red".equals(team)) {
                sendRed(Message.NOTICE_RED_REMOVE_WAYPOINT.getString().replace("%player%", player.getName()).replace("%index%", String.valueOf(index)));
            } else {
                sendBlue(Message.NOTICE_BLUE_REMOVE_WAYPOINT.getString().replace("%player%", player.getName()).replace("%index%", String.valueOf(index)));
            }
            return true;
        }
        return false;
    }

    /**
     * Dialog 专用：按编号直接传送到传送点并发送成功提示（不使用点击语义）。
     */
    public static void teleportToWaypoint(Player player, int index) {
        String team = resolvePlayerTeam(player);
        if (team.isEmpty()) {
            return;
        }
        Location waypoint = getWaypoint(team, index);
        if (waypoint == null) {
            return;
        }
        teleportPlayer(player, waypoint);
        String x = String.format("%.1f", waypoint.getX());
        String y = String.format("%.1f", waypoint.getY());
        String z = String.format("%.1f", waypoint.getZ());
        player.sendMessage(Message.NOTICE_TP_SUCCESS.getString().replace("%x%", x).replace("%y%", y)
                .replace("%z%", z));
    }

    private static void teleportPlayer(Player player, Location location) {
        if (player == null || location == null) {
            return;
        }
        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            Main.getFoliaLib().getScheduler().teleportAsync(player, location);
            return;
        }
        player.teleport(location);
    }

    public static void startPreGameLoop() {
        stopPreGameLoop();
        preGameTask = Main.getFoliaLib().getScheduler().runTimer(Game::tickPreGame, 0L, 2L);
    }

    private static void stopPreGameLoop() {
        if (preGameTask != null) {
            preGameTask.cancel();
            preGameTask = null;
        }
    }

    private static void startInGameLoop() {
        stopInGameLoop();
        inGameTask = Main.getFoliaLib().getScheduler().runTimer(Game::tickInGame, 0L, 2L);
    }

    /** Resume the in-game loop after a recovered game progress. */
    public static void resumeRecoveredGame() {
        startInGameLoop();
        // Restart the time-mode countdown (TIME mode) so a recovered game resumes its timer.
        startTimeModeCountdownIfNeeded();
    }

    private static void stopInGameLoop() {
        if (inGameTask != null) {
            inGameTask.cancel();
            inGameTask = null;
        }
    }

    // Run per 2t before the game, provide regeneration and saturation effects
    private static void tickPreGame() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
                Main.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10, 255));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 10, 255));
                });
            } else {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10, 255));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 10, 255));
            }
        }
        if (getCurrentGameState().equals(GameState.INGAME)) {
            stopPreGameLoop();
        }
    }

    // Run per 5t during the game
    private static void tickInGame() {

        if (!getCurrentGameState().equals(GameState.INGAME)) {
            stopInGameLoop();
            return;
        }

        // Inventory check
        checkRedInventory();
        checkBlueInventory();

        // Keep persistent in-game effects stable across death/respawn timing differences.
        effectMaintenanceTicks++;
        if (effectMaintenanceTicks >= 10) {
            effectMaintenanceTicks = 0;
            maintainIngamePlayerEffects();
        }

        // Win check
        if (!isContestModeActive()) {
            if (redTeamRemainingBlocks.isEmpty()) {
                redWin();
                showRanking();
                stopInGameLoop();
            }
            if (blueTeamRemainingBlocks.isEmpty()) {
                blueWin();
                showRanking();
                stopInGameLoop();
            }
        }

        // Roll check
        if (isContestModeActive()) {
            if (!contestModeRollPlayers.isEmpty())
                checkContestModeRoll();
        } else {
            if (!redRollPlayers.isEmpty())
                checkRedRoll();
            if (!blueRollPlayers.isEmpty())
                checkBlueRoll();
        }
    }

    private static void showRanking() {
        Map<String, Integer> rankingMap = new HashMap<>();
        for (String member : redTeamPlayers) {
            rankingMap.put(member, collectAmount.getOrDefault(member, 0));
        }
        for (String member : blueTeamPlayers) {
            rankingMap.put(member, collectAmount.getOrDefault(member, 0));
        }
        for (Map.Entry<String, Integer> entry : collectAmount.entrySet()) {
            rankingMap.put(entry.getKey(), entry.getValue());
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(rankingMap.entrySet());
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        sendAll(Message.NOTICE_RANKING.getString());
        for (Map.Entry<String, Integer> entry : entries) {
            if (redTeamPlayers.contains(entry.getKey())) {
                sendAll(Message.NOTICE_RANKING_RED.getString().replace("%player%", entry.getKey()).replace("%amount%",
                        entry.getValue().toString()));
            } else if (blueTeamPlayers.contains(entry.getKey())) {
                sendAll(Message.NOTICE_RANKING_BLUE.getString().replace("%player%", entry.getKey()).replace("%amount%",
                        entry.getValue().toString()));
            } else {
                sendAll(Message.NOTICE_RANKING_OFFLINE.getString().replace("%player%", entry.getKey())
                        .replace("%amount%", entry.getValue().toString()));
            }
        }
    }

    private static void maintainIngamePlayerEffects() {
        if (!getCurrentGameState().equals(GameState.INGAME)) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null) {
                continue;
            }
            if (!redTeamPlayers.contains(player.getName()) && !blueTeamPlayers.contains(player.getName())) {
                continue;
            }
            if (player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }

            runOnEntityThread(player, () -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false));
            });

            if (Setting.isSpeedMode()) {
                applyConfiguredSpeedModeEffects(player, true);
            }
        }

        refreshComebackEffects();
    }

    private static void checkRedRoll() {
        Set<String> redSet = new HashSet<>(redRollPlayers);
        Set<String> redOnlineSet = new HashSet<>(getOnlineTeamPlayers("red"));
        if (!getOnlineTeamPlayers("red").isEmpty() && redSet.containsAll(redOnlineSet)) {
            List<String> b = new ArrayList<>(blocks);
            b.removeAll(redTeamBlocks);
            if (b.isEmpty()) {
                sendRed(Message.NOTICE_CANNOT_ROLL_NO_CANDIDATE.getString());
                redRollPlayers.clear();
                return;
            }
            Random random = new Random();
            int rollAmount = getCurrentBlocks("red").size();
            for (int i = 0; i < rollAmount; i++) {
                redTeamRemainingBlocks.set(i, b.get(random.nextInt(b.size())));
            }
            sendAll(Message.NOTICE_RED_ROLL_SUCCESS.getString());
            redTeamRollCount += 1;
            redRollPlayers.clear();
            updateScoreboard();
        }
    }

    private static void checkBlueRoll() {
        Set<String> blueSet = new HashSet<>(blueRollPlayers);
        Set<String> blueOnlineSet = new HashSet<>(getOnlineTeamPlayers("blue"));
        if (!getOnlineTeamPlayers("blue").isEmpty() && blueSet.containsAll(blueOnlineSet)) {
            List<String> b = new ArrayList<>(blocks);
            b.removeAll(blueTeamBlocks);
            if (b.isEmpty()) {
                sendBlue(Message.NOTICE_CANNOT_ROLL_NO_CANDIDATE.getString());
                blueRollPlayers.clear();
                return;
            }
            Random random = new Random();
            int rollAmount = getCurrentBlocks("blue").size();
            for (int i = 0; i < rollAmount; i++) {
                blueTeamRemainingBlocks.set(i, b.get(random.nextInt(b.size())));
            }
            sendAll(Message.NOTICE_BLUE_ROLL_SUCCESS.getString());
            blueTeamRollCount += 1;
            blueRollPlayers.clear();
            updateScoreboard();
        }
    }

    private static void checkContestModeRoll() {
        // 获取所有在线玩家
        Set<String> allOnlinePlayers = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (inGamePlayers.contains(player.getName())) {
                allOnlinePlayers.add(player.getName());
            }
        }
        
        Set<String> contestRollSet = new HashSet<>(contestModeRollPlayers);
        
        // 检查是否所有在线玩家都同意轮换
        if (!allOnlinePlayers.isEmpty() && contestRollSet.containsAll(allOnlinePlayers)) {
            List<String> b = new ArrayList<>(blocks);
            Random random = new Random();
            int rollAmount = redTeamRemainingBlocks.size();
            for (int i = 0; i < rollAmount; i++) {
                redTeamRemainingBlocks.set(i, b.get(random.nextInt(b.size())));
            }
            sendAll(Message.NOTICE_RED_ROLL_SUCCESS.getString());
            contestModeRollCount += 1;
            contestModeRollPlayers.clear();
            updateScoreboard();
        }
    }

    private static void setLocateScore() {
        if (Setting.getCurrentGameMode() == Setting.GameMode.TIME) {
            locateCost = 16;
            return;
        }

        if (Setting.getCurrentGameMode() == Setting.GameMode.RACING) {
            locateCost = 16;
            return;
        }

        if (Setting.getBlockAmount() <= 20)
            locateCost = 5;
        else if (Setting.getBlockAmount() <= 50)
            locateCost = 8;
        else if (Setting.getBlockAmount() <= 100)
            locateCost = 16;
        else if (Setting.getBlockAmount() <= 200)
            locateCost = 20;
        else
            locateCost = 30;
    }

    private static void checkRedInventory() {
        // In CONTEST mode, check if both teams have the same block first
        if (isContestModeActive()) {
            for (String block : getCurrentBlocks("red")) {
                // Check if red team has the block
                boolean redHasBlock = false;
                for (String player : redTeamPlayers) {
                    Player p = Bukkit.getPlayer(player);
                    if (p != null && p.getInventory().contains(Material.valueOf(block))) {
                        redHasBlock = true;
                        break;
                    }
                }
                if (!redHasBlock) {
                    for (Inventory chest : redTeamChest) {
                        if (chest.contains(Material.valueOf(block))) {
                            redHasBlock = true;
                            break;
                        }
                    }
                }
                
                // Check if blue team has the block
                boolean blueHasBlock = false;
                for (String player : blueTeamPlayers) {
                    Player p = Bukkit.getPlayer(player);
                    if (p != null && p.getInventory().contains(Material.valueOf(block))) {
                        blueHasBlock = true;
                        break;
                    }
                }
                if (!blueHasBlock) {
                    for (Inventory chest : blueTeamChest) {
                        if (chest.contains(Material.valueOf(block))) {
                            blueHasBlock = true;
                            break;
                        }
                    }
                }
                
                // If both teams have the block, execute joint completion
                if (redHasBlock && blueHasBlock) {
                    bothTaskComplete(block);
                    return;
                }
            }
        }
        
        // Normal mode: Complete from player
        for (String player : redTeamPlayers) {
            for (String block : getCurrentBlocks("red")) {
                Player p = Bukkit.getPlayer(player);
                if (p == null)
                    continue;
                if (p.getInventory().contains(Material.valueOf(block))) {
                    redTaskComplete(block, player);
                    return;
                }
            }
        }
        // Complete from team chest
        for (String block : getCurrentBlocks("red")) {
            for (Inventory chest : redTeamChest) {
                if (chest.contains(Material.valueOf(block))) {
                    redTaskComplete(block, Message.NOTICE_RED_TEAM_CHEST.getString());
                    return;
                }
            }
        }
    }

    private static void checkBlueInventory() {
        // In CONTEST mode, both-team completion is handled in checkRedInventory.
        // Still allow blue to complete individually here.
        
        // Complete from player
        for (String player : blueTeamPlayers) {
            for (String block : getCurrentBlocks("blue")) {
                Player p = Bukkit.getPlayer(player);
                if (p == null)
                    continue;
                if (p.getInventory().contains(Material.valueOf(block))) {
                    blueTaskComplete(block, player);
                    return;
                }
            }
        }
        // Complete from team chest
        for (String block : getCurrentBlocks("blue")) {
            for (Inventory chest : blueTeamChest) {
                if (chest.contains(Material.valueOf(block))) {
                    blueTaskComplete(block, Message.NOTICE_BLUE_TEAM_CHEST.getString());
                    return;
                }
            }
        }
    }

    public static void bothTaskComplete(String block) {
        // Both teams collect the same block in CONTEST mode
        sendAll(Message.NOTICE_BOTH_COLLECT_CONTEST.getString().replace("%block%", TranslationUtil.getValue(block)));
        Bukkit.getLogger().info(Message.NOTICE_BOTH_COLLECT_CONTEST.getString()
                .replace("%block%", TranslationUtil.getValue(block)).replaceAll("§.", ""));
        playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        
        // Remove block from both teams' remaining blocks
        redTeamRemainingBlocks.remove(block);
        blueTeamRemainingBlocks.remove(block);
        
        // Mark as completed for global tracking
        redCompletedBlocks.add(block);
        blueCompletedBlocks.add(block);
        
        // Both teams get 1 point
        redTeamScore += 1;
        blueTeamScore += 1;
        redTeamCurrentBlockAmount += 1;
        blueTeamCurrentBlockAmount += 1;
        
        updateScoreboard();
        
        // Check if either team reached target amount
        if (redTeamCurrentBlockAmount >= Setting.getBlockAmount()
                && blueTeamCurrentBlockAmount >= Setting.getBlockAmount()) {
            drawGame();
            showRanking();
            return;
        }
        if (redTeamCurrentBlockAmount >= Setting.getBlockAmount()) {
            redWin();
            showRanking();
            return;
        }
        if (blueTeamCurrentBlockAmount >= Setting.getBlockAmount()) {
            blueWin();
            showRanking();
            return;
        }
    }

    public static void redTaskComplete(String block, String player) {
        sendAll(Message.NOTICE_RED_COLLECT.getString().replace("%block%", TranslationUtil.getValue(block))
                .replace("%player%", player));
        Bukkit.getLogger().info(Message.NOTICE_RED_COLLECT.getString()
                .replace("%block%", TranslationUtil.getValue(block)).replace("%player%", player).replaceAll("§.", ""));
        playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        redTeamRemainingBlocks.remove(block);
        // mark as completed for global tracking
        redCompletedBlocks.add(block);
        redTeamScore += 1;
        redTeamCurrentBlockAmount += 1;
        collect(player);
        updateScoreboard();
        if (isContestModeActive() && redTeamCurrentBlockAmount >= Setting.getBlockAmount()) {
            redWin();
            showRanking();
            return;
        }
        // Only declare victory for this team if, in time mode, that team's
        // completed list contains all global blocks (i.e. the team alone
        // has collected every block in the pool).
        if (isTimeModeActive() && redCompletedBlocks.containsAll(blocks)) {
            redWin();
            showRanking();
            return;
        }
        // In time mode, when a block is completed (and not in overtime),
        // generate and add a replacement target so teams always have targets.
        if (isTimeModeActive() && !timeModeOvertime) {
            float progress = 0f;
            if (timeModeDurationSeconds > 0) {
                progress = (float) (timeModeDurationSeconds - timeModeRemainingSeconds) / (float) timeModeDurationSeconds;
                progress = Math.max(0f, Math.min(1f, progress));
            }
            String newBlock = selectBlockByTimeProgress("red", progress);
            redTeamRemainingBlocks.add(newBlock);
            updateScoreboard();
        }
        if (isTimeModeActive() && timeModeOvertime) {
            finalizeOvertimeWin(true);
            return;
        }
        // Put items into the opponent's team chest (controlled by team-chest-gift toggle)
        if (Setting.isTeamChestGift()) {
            for (int i = blueTeamChest.size() - 1; i >= 0; i--) {
                Inventory chest = blueTeamChest.get(i);
                int emptyPos = chest.firstEmpty();
                if (emptyPos == -1) {
                    continue;
                }
                chest.setItem(emptyPos, new ItemStack(Material.valueOf(block), Setting.getTeamChestGiftAmount()));
                return;
            }
            sendAll(Message.NOTICE_TEAM_CHEST_FULL.getString().replace("%team%", Message.TEAM_BLUE_NAME.getString())
                    .replace("%block%", TranslationUtil.getValue(block)));
        }
    }

    public static void blueTaskComplete(String block, String player) {
        sendAll(Message.NOTICE_BLUE_COLLECT.getString().replace("%block%", TranslationUtil.getValue(block))
                .replace("%player%", player));
        Bukkit.getLogger().info(Message.NOTICE_BLUE_COLLECT.getString()
                .replace("%block%", TranslationUtil.getValue(block)).replace("%player%", player).replaceAll("§.", ""));
        playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        blueTeamRemainingBlocks.remove(block);
        // mark as completed for global tracking
        blueCompletedBlocks.add(block);
        blueTeamScore += 1;
        blueTeamCurrentBlockAmount += 1;
        collect(player);
        updateScoreboard();
        if (isContestModeActive() && blueTeamCurrentBlockAmount >= Setting.getBlockAmount()) {
            blueWin();
            showRanking();
            return;
        }
        // Only declare victory for this team if, in time mode, that team's
        // completed list contains all global blocks (i.e. the team alone
        // has collected every block in the pool).
        if (isTimeModeActive() && blueCompletedBlocks.containsAll(blocks)) {
            blueWin();
            showRanking();
            return;
        }
        // In time mode, when a block is completed (and not in overtime),
        // generate and add a replacement target so teams always have targets.
        if (isTimeModeActive() && !timeModeOvertime) {
            float progress = 0f;
            if (timeModeDurationSeconds > 0) {
                progress = (float) (timeModeDurationSeconds - timeModeRemainingSeconds) / (float) timeModeDurationSeconds;
                progress = Math.max(0f, Math.min(1f, progress));
            }
            String newBlock = selectBlockByTimeProgress("blue", progress);
            blueTeamRemainingBlocks.add(newBlock);
            updateScoreboard();
        }
        if (isTimeModeActive() && timeModeOvertime) {
            finalizeOvertimeWin(false);
            return;
        }
        // Put items into the opponent's team chest (controlled by team-chest-gift toggle)
        if (Setting.isTeamChestGift()) {
            for (int i = redTeamChest.size() - 1; i >= 0; i--) {
                Inventory chest = redTeamChest.get(i);
                int emptyPos = chest.firstEmpty();
                if (emptyPos == -1) {
                    continue;
                }
                chest.setItem(emptyPos, new ItemStack(Material.valueOf(block), Setting.getTeamChestGiftAmount()));
                return;
            }
            sendAll(Message.NOTICE_TEAM_CHEST_FULL.getString().replace("%team%", Message.TEAM_RED_NAME.getString())
                    .replace("%block%", TranslationUtil.getValue(block)));
        }
    }

    public static void redWin() {
        stopTimeModeCountdown();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.closeInventory();
            player.sendTitle(Message.NOTICE_RED_WIN.getString(), null);
            player.setGameMode(GameMode.SPECTATOR);
        }
        sendAll(Message.NOTICE_RED_WIN.getString());
        playSound(Sound.UI_TOAST_CHALLENGE_COMPLETE);
        setCurrentGameState(GameState.END);
        VoicechatBridge.syncAllPlayers();
        updateScoreboard();
    }

    public static void blueWin() {
        stopTimeModeCountdown();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.closeInventory();
            player.sendTitle(Message.NOTICE_BLUE_WIN.getString(), null);
            player.setGameMode(GameMode.SPECTATOR);
        }
        sendAll(Message.NOTICE_BLUE_WIN.getString());
        playSound(Sound.UI_TOAST_CHALLENGE_COMPLETE);
        setCurrentGameState(GameState.END);
        VoicechatBridge.syncAllPlayers();
        updateScoreboard();
    }

    public static void drawGame() {
        stopTimeModeCountdown();
        String drawMessage = Message.NOTICE_DRAW.getString();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.closeInventory();
            player.sendTitle(drawMessage, null);
            player.setGameMode(GameMode.SPECTATOR);
        }
        sendAll(drawMessage);
        playSound(Sound.UI_TOAST_CHALLENGE_COMPLETE);
        setCurrentGameState(GameState.END);
        VoicechatBridge.syncAllPlayers();
        updateScoreboard();
    }

    public static List<String> getCurrentBlocks(String team) {
        List<String> source = switch (team) {
            case "red" -> redTeamRemainingBlocks;
            case "blue" -> blueTeamRemainingBlocks;
            default -> throw new IllegalStateException("Unexpected value: " + team);
        };
        // Snapshot via indexed access so async scoreboard reads (SimpleScore) never iterate a
        // live sublist that the game thread mutates concurrently (avoids ConcurrentModificationException).
        int count = Math.min(source.size(), 4);
        List<String> snapshot = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            if (i < source.size()) {
                snapshot.add(source.get(i));
            }
        }
        return snapshot;
    }

    public static List<String> getOnlinePlayersString() {
        List<String> onlinePlayers = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach((Player player) -> onlinePlayers.add(player.getName()));
        return onlinePlayers;
    }

    public static List<String> getOnlineTeamPlayers(String team) {
        List<String> onlineTeamPlayers = new ArrayList<>();

        if (team.equals("red")) {
            for (String player : redTeamPlayers) {
                if (Bukkit.getPlayer(player) != null) {
                    onlineTeamPlayers.add(player);
                }
            }
        } else {
            for (String player : blueTeamPlayers) {
                if (Bukkit.getPlayer(player) != null) {
                    onlineTeamPlayers.add(player);
                }
            }
        }
        return onlineTeamPlayers;
    }

    public static void playSound(Sound sound) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player, sound, 1F, 1F);
        }
    }

    public static String getCoords(Location location) {
        return String.format("%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ());
    }

    public static void collect(String p) {
        if (collectAmount.containsKey(p)) {
            int currentAmount = collectAmount.get(p);
            collectAmount.put(p, currentAmount + 1);
        } else {
            collectAmount.put(p, 1);
        }
    }

    public static void applyConfiguredSpeedModeEffects(Player player) {
        applyConfiguredSpeedModeEffects(player, false);
    }

    public static void applyConfiguredSpeedModeEffects(Player player, boolean ignoreDelayTicks) {
        if (player == null || !Setting.isSpeedMode()) {
            return;
        }
        for (Setting.SpeedModeEffectOption option : Setting.getSpeedModeEffects()) {
            if (option == null || option.getEffect() == null) {
                continue;
            }
            if (!ignoreDelayTicks && option.getDelayTicks() > 0L) {
                Main.getFoliaLib().getScheduler().runAtEntityLater(player,
                        () -> player.addPotionEffect(option.getEffect()), option.getDelayTicks());
            } else {
                runOnEntityThread(player, () -> player.addPotionEffect(option.getEffect()));
            }
        }
    }

    private static void reapplyBaselineMobility(Player player) {
        if (player == null) {
            return;
        }
        if (Setting.isSpeedMode()) {
            applyConfiguredSpeedModeEffects(player, true);
        }
    }

    private static void clearComebackEffects(List<String> members) {
        for (String name : members) {
            Player player = Bukkit.getPlayer(name);
            if (player == null) {
                continue;
            }
            boolean removed = false;

            for (PotionEffect configured : Setting.getComebackBuffEffects()) {
                if (configured == null || configured.getType() == null) {
                    continue;
                }
                PotionEffect active = player.getPotionEffect(configured.getType());
                if (active != null && active.getAmplifier() >= configured.getAmplifier()) {
                    runOnEntityThread(player, () -> player.removePotionEffect(configured.getType()));
                    removed = true;
                }
            }

            if (removed) {
                reapplyBaselineMobility(player);
            }
        }
    }

    private static void applyComebackEffects(String team) {
        List<String> members = getOnlineTeamPlayers(team);
        for (String name : members) {
            Player player = Bukkit.getPlayer(name);
            if (player == null) {
                continue;
            }
            for (PotionEffect effect : Setting.getComebackBuffEffects()) {
                if (effect == null || effect.getType() == null) {
                    continue;
                }
                runOnEntityThread(player, () -> player.addPotionEffect(effect));
            }
        }
    }

    private static void runOnEntityThread(Player player, Runnable action) {
        if (player == null || action == null) {
            return;
        }
        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            Main.getFoliaLib().getScheduler().runAtEntity(player, task -> action.run());
            return;
        }
        action.run();
    }

    public static void refreshComebackEffects() {
        int threshold = Math.max(1, Setting.getComebackBuffThresholdPoints());
        if (!getCurrentGameState().equals(GameState.INGAME) || !Setting.isComebackBuffEnabled()) {
            if (comebackBuffState != ComebackBuffState.NONE) {
                sendAll(Message.NOTICE_COMEBACK_CLEAR.getString().replace("%points%", String.valueOf(threshold)));
            }
            comebackBuffState = ComebackBuffState.NONE;
            clearComebackEffects(redTeamPlayers);
            clearComebackEffects(blueTeamPlayers);
            return;
        }

        int diff = redTeamCurrentBlockAmount - blueTeamCurrentBlockAmount;
        if (Math.abs(diff) < threshold) {
            if (comebackBuffState != ComebackBuffState.NONE) {
                sendAll(Message.NOTICE_COMEBACK_CLEAR.getString().replace("%points%", String.valueOf(threshold)));
            }
            comebackBuffState = ComebackBuffState.NONE;
            clearComebackEffects(redTeamPlayers);
            clearComebackEffects(blueTeamPlayers);
            return;
        }

        if (diff >= threshold) {
            if (comebackBuffState != ComebackBuffState.BLUE) {
                sendAll(Message.NOTICE_COMEBACK_APPLY.getString()
                        .replace("%team%", Message.TEAM_BLUE_NAME.getString())
                        .replace("%points%", String.valueOf(threshold)));
            }
            comebackBuffState = ComebackBuffState.BLUE;
            applyComebackEffects("blue");
            clearComebackEffects(redTeamPlayers);
        } else if (diff <= -threshold) {
            if (comebackBuffState != ComebackBuffState.RED) {
                sendAll(Message.NOTICE_COMEBACK_APPLY.getString()
                        .replace("%team%", Message.TEAM_RED_NAME.getString())
                        .replace("%points%", String.valueOf(threshold)));
            }
            comebackBuffState = ComebackBuffState.RED;
            applyComebackEffects("red");
            clearComebackEffects(blueTeamPlayers);
        }
    }

    public static GameState getCurrentGameState() {
        return currentGameState;
    }

    public static void setCurrentGameState(GameState currentGameState) {
        Game.currentGameState = currentGameState;
        VoicechatBridge.syncAllPlayers();
        Motd.refresh();
        updateScoreboard();
    }

    /**
     * Buys team supplies (speed mode only). Costs a configurable amount of team
     * points and gives the configured supply items to either only the buyer or
     * to every online teammate, depending on the supply-give-all setting.
     */
    public static void buySupply(Player player) {
        if (!Setting.isSpeedMode()) {
            player.sendMessage(Message.NOTICE_SUPPLY_SPEED_ONLY.getString(player));
            return;
        }
        boolean isRed = redTeamPlayers.contains(player.getName());
        boolean isBlue = blueTeamPlayers.contains(player.getName());
        if (!isRed && !isBlue) return;

        int price = Setting.getSupplyPrice();
        if (isRed && redTeamScore < price || isBlue && blueTeamScore < price) {
            player.sendMessage(Message.NOTICE_NOT_ENOUGH_SCORE.getString(player));
            return;
        }

        if (isRed) redTeamScore -= price;
        else blueTeamScore -= price;

        List<Player> recipients = new ArrayList<>();
        if (Setting.isSupplyGiveAll()) {
            List<String> team = isRed ? redTeamPlayers : blueTeamPlayers;
            for (String name : team) {
                Player teammate = Bukkit.getPlayerExact(name);
                if (teammate != null && teammate.isOnline()) {
                    recipients.add(teammate);
                }
            }
        } else {
            recipients.add(player);
        }

        List<ItemStack> supply = Setting.getSupplyItems();
        for (Player recipient : recipients) {
            for (ItemStack item : supply) {
                ItemStack toGive = item.clone();
                if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
                    Main.getFoliaLib().getScheduler().runAtEntity(recipient, task -> giveOrDrop(recipient, toGive));
                } else {
                    giveOrDrop(recipient, toGive);
                }
            }
        }

        String teamName = isRed ? Message.TEAM_RED_NAME.getString() : Message.TEAM_BLUE_NAME.getString();
        sendAll(Message.NOTICE_SUPPLY_PURCHASED,
                (viewer, text) -> text.replace("%player%", player.getName())
                        .replace("%team%", teamName));
        updateScoreboard();
        GameProgressStore.saveNow();
    }

    private static void giveOrDrop(Player player, ItemStack item) {
        var leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    /**
     * Gives a rule book to the player during pregame, in the player's language.
     * Pages come from rule-book.pages in the lang file (bundled defaults are used
     * when the on-disk lang file is outdated and does not contain the key).
     */
    private static void giveRuleBook(Player player) {
        try {
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();
            if (meta == null) return;
            meta.setTitle(Message.RULE_BOOK_TITLE.getString(player));
            meta.setAuthor(Message.RULE_BOOK_AUTHOR.getString(player));
            for (String page : Message.RULE_BOOK_PAGES.getStringList(player)) {
                meta.addPage(page);
            }
            book.setItemMeta(meta);
            player.getInventory().addItem(book);
        } catch (Exception ex) {
            Main.getInstance().getLogger().warning("[BlockRacing] Failed to give rule book: " + ex.getMessage());
        }
    }

    /**
     * Clears the player's pregame inventory and re-gives the rule book in the
     * player's current language. Runs on the entity scheduler so that, on Folia,
     * the clear and the book are applied in order on the same thread.
     */
    public static void refreshRuleBook(Player player) {
        if (player == null || getCurrentGameState() != GameState.PREGAME) return;
        Runnable refresh = () -> {
            player.getInventory().clear();
            giveRuleBook(player);
        };
        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            Main.getFoliaLib().getScheduler().runAtEntity(player, task -> refresh.run());
        } else {
            refresh.run();
        }
    }
}
