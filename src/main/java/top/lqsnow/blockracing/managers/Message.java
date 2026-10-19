package top.lqsnow.blockracing.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.utils.MiniMessageUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public enum Message {
    // scoreboard
    SCOREBOARD_MODE_NORMAL("scoreboard.game-mode.normal"),
    SCOREBOARD_MODE_RACING("scoreboard.game-mode.racing"),
    SCOREBOARD_MODE_CONTEST("scoreboard.game-mode.contest"),
    SCOREBOARD_MODE_TIME("scoreboard.game-mode.time"),
    SCOREBOARD_MODE_NETHER("scoreboard.game-mode.nether"),
    SCOREBOARD_MODE_SPEED("scoreboard.game-mode.speed"),
    SCOREBOARD_MODE_GIFT("scoreboard.game-mode.gift"),
    SCOREBOARD_BLOCKS_EASY("scoreboard.blocks.easy"),
    SCOREBOARD_BLOCKS_MEDIUM("scoreboard.blocks.medium"),
    SCOREBOARD_BLOCKS_HARD("scoreboard.blocks.hard"),
    SCOREBOARD_BLOCKS_DYED("scoreboard.blocks.dyed"),
    SCOREBOARD_BLOCKS_ADDON("scoreboard.blocks.addon"),
    SCOREBOARD_BLOCKS_END("scoreboard.blocks.end"),
    SCOREBOARD_BLOCKS_NETHER("scoreboard.blocks.nether"),
    SCOREBOARD_PREGAME_TITLE("scoreboard.pregame.title"),
    SCOREBOARD_PREGAME_SLOT11("scoreboard.pregame.slot11"),
    SCOREBOARD_PREGAME_SLOT10("scoreboard.pregame.slot10"),
    SCOREBOARD_PREGAME_SLOT9("scoreboard.pregame.slot9"),
    SCOREBOARD_PREGAME_SLOT8("scoreboard.pregame.slot8"),
    SCOREBOARD_PREGAME_SLOT7("scoreboard.pregame.slot7"),
    SCOREBOARD_PREGAME_SLOT6("scoreboard.pregame.slot6"),
    SCOREBOARD_PREGAME_SLOT5("scoreboard.pregame.slot5"),
    SCOREBOARD_PREGAME_SLOT4("scoreboard.pregame.slot4"),
    SCOREBOARD_PREGAME_SLOT3("scoreboard.pregame.slot3"),
    SCOREBOARD_PREGAME_SLOT2("scoreboard.pregame.slot2"),
    SCOREBOARD_PREGAME_SLOT1("scoreboard.pregame.slot1"),
    SCOREBOARD_PREGAME_TIME_MODE_INFO("scoreboard.pregame.time-mode-info"),
    SCOREBOARD_INGAME_TITLE("scoreboard.ingame.title"),
    SCOREBOARD_RED_SCORE("scoreboard.ingame.red-score"),
    SCOREBOARD_BLUE_SCORE("scoreboard.ingame.blue-score"),
    SCOREBOARD_RED_SCORE_TIME("scoreboard.ingame.red-score-time"),
    SCOREBOARD_BLUE_SCORE_TIME("scoreboard.ingame.blue-score-time"),
    SCOREBOARD_BLOCK_FORMAT("scoreboard.ingame.block-format"),
    SCOREBOARD_DIVIDING_LINE("scoreboard.ingame.dividing-line"),
    SCOREBOARD_BOTTOM_SLOT("scoreboard.ingame.bottom-slot"),
    SCOREBOARD_TIME_MODE_TIME_LEFT("scoreboard.ingame.time-left"),
    SCOREBOARD_TIME_MODE_OVERTIME("scoreboard.ingame.overtime"),
    SCOREBOARD_BLOCK_DIFFICULTY_EASY("scoreboard.ingame.block-difficulty.easy"),
    SCOREBOARD_BLOCK_DIFFICULTY_MEDIUM("scoreboard.ingame.block-difficulty.medium"),
    SCOREBOARD_BLOCK_DIFFICULTY_HARD("scoreboard.ingame.block-difficulty.hard"),
    SCOREBOARD_BLOCK_DIFFICULTY_DYED("scoreboard.ingame.block-difficulty.dyed"),
    SCOREBOARD_BLOCK_DIFFICULTY_ADDON("scoreboard.ingame.block-difficulty.addon"),
    SCOREBOARD_BLOCK_DIFFICULTY_END("scoreboard.ingame.block-difficulty.end"),
    SCOREBOARD_BLOCK_DIFFICULTY_NETHER("scoreboard.ingame.block-difficulty.nether"),
    SCOREBOARD_COMMON_BRAND("scoreboard.common.brand"),
    SCOREBOARD_COMMON_MODE_LINE("scoreboard.common.mode-line"),
    SCOREBOARD_COMMON_MODE_DETAIL_TIME_LEFT("scoreboard.common.mode-detail.time-left"),
    SCOREBOARD_COMMON_MODE_DETAIL_OVERTIME("scoreboard.common.mode-detail.overtime"),
    SCOREBOARD_COMMON_MODE_DETAIL_DEFAULT("scoreboard.common.mode-detail.default"),
    SCOREBOARD_END_TITLE("scoreboard.end.title"),
    SCOREBOARD_END_STATUS("scoreboard.end.status"),
    SCOREBOARD_END_WINNER_RED("scoreboard.end.winner.red"),
    SCOREBOARD_END_WINNER_BLUE("scoreboard.end.winner.blue"),
    SCOREBOARD_END_WINNER_DRAW("scoreboard.end.winner.draw"),
    SCOREBOARD_END_RED_SCORE("scoreboard.end.red-score"),
    SCOREBOARD_END_BLUE_SCORE("scoreboard.end.blue-score"),
    SCOREBOARD_END_RED_SCORE_TIME("scoreboard.end.red-score-time"),
    SCOREBOARD_END_BLUE_SCORE_TIME("scoreboard.end.blue-score-time"),

    // motd
    MOTD_PREGAME("motd.pregame"),
    MOTD_INGAME("motd.ingame"),
    MOTD_END("motd.end"),

    // team
    TEAM_RED_NAME("team.red.name"),
    TEAM_RED_PREFIX("team.red.prefix"),
    TEAM_RED_CHAT("team.red.chat"),
    TEAM_RED_COLOR("team.red.color"),
    TEAM_BLUE_NAME("team.blue.name"),
    TEAM_BLUE_PREFIX("team.blue.prefix"),
    TEAM_BLUE_CHAT("team.blue.chat"),
    TEAM_BLUE_COLOR("team.blue.color"),

    // menu
    MENU_PREGAME_TITLE("menu.pregame-menu.title"),
    MENU_JOIN_RED("menu.pregame-menu.join-red"),
    MENU_JOIN_RED_LORE("menu.pregame-menu.join-red-lore"),
    MENU_JOIN_BLUE("menu.pregame-menu.join-blue"),
    MENU_JOIN_BLUE_LORE("menu.pregame-menu.join-blue-lore"),
    MENU_READY("menu.pregame-menu.ready"),
    MENU_READY_LORE("menu.pregame-menu.ready-lore"),
    MENU_START("menu.pregame-menu.start"),
    MENU_START_LORE("menu.pregame-menu.start-lore"),
    MENU_START_FORCE_SHIFT_LORE("menu.pregame-menu.start-force-shift-lore"),
    MENU_BLOCK_AMOUNT("menu.pregame-menu.block-amount"),
    MENU_BLOCK_AMOUNT_LORE("menu.pregame-menu.block-amount-lore"),
    MENU_TIME_MODE_DURATION("menu.pregame-menu.time-mode-duration"),
    MENU_TIME_MODE_DURATION_LORE("menu.pregame-menu.time-mode-duration-lore"),
    MENU_COMEBACK_THRESHOLD("menu.pregame-menu.comeback-threshold"),
    MENU_COMEBACK_THRESHOLD_LORE("menu.pregame-menu.comeback-threshold-lore"),
    MENU_MEDIUM_BLOCKS("menu.pregame-menu.medium-blocks"),
    MENU_HARD_BLOCKS("menu.pregame-menu.hard-blocks"),
    MENU_DYED_BLOCKS("menu.pregame-menu.dyed-blocks"),
    MENU_END_BLOCKS("menu.pregame-menu.end-blocks"),
    MENU_ADDON_BLOCKS("menu.pregame-menu.addon-blocks"),
    MENU_DISABLED("menu.pregame-menu.disabled"),
    MENU_ENABLED("menu.pregame-menu.enabled"),
    MENU_CURRENT_MODE("menu.pregame-menu.current-mode"),
    MENU_SWITCH_TO("menu.pregame-menu.switch-to"),
    MENU_NORMAL_MODE("menu.pregame-menu.normal-mode"),
    MENU_RACING_MODE("menu.pregame-menu.racing-mode"),
    MENU_CONTEST_MODE("menu.pregame-menu.contest-mode"),
    MENU_TIME_MODE("menu.pregame-menu.time-mode"),
    MENU_SPEED_MODE_ENABLED("menu.pregame-menu.speed-mode-enabled"),
    MENU_SPEED_MODE_DISABLED("menu.pregame-menu.speed-mode-disabled"),
    MENU_TEAM_CHEST_GIFT_ENABLED("menu.pregame-menu.team-chest-gift-enabled"),
    MENU_TEAM_CHEST_GIFT_DISABLED("menu.pregame-menu.team-chest-gift-disabled"),
    MENU_NETHER_MODE_ENABLED("menu.pregame-menu.nether-mode-enabled"),
    MENU_NETHER_MODE_DISABLED("menu.pregame-menu.nether-mode-disabled"),
    MENU_SHARED_TEAM_SPAWN_ENABLED("menu.pregame-menu.shared-team-spawn-enabled"),
    MENU_SHARED_TEAM_SPAWN_DISABLED("menu.pregame-menu.shared-team-spawn-disabled"),
    MENU_END_PORTAL_BROADCAST_ENABLED("menu.pregame-menu.end-portal-broadcast-enabled"),
    MENU_END_PORTAL_BROADCAST_DISABLED("menu.pregame-menu.end-portal-broadcast-disabled"),
    MENU_NORMAL_MODE_LORE("menu.pregame-menu.normal-mode-lore"),
    MENU_RACING_MODE_LORE("menu.pregame-menu.racing-mode-lore"),
    MENU_CONTEST_MODE_LORE("menu.pregame-menu.contest-mode-lore"),
    MENU_TIME_MODE_LORE("menu.pregame-menu.time-mode-lore"),
    MENU_SPEED_MODE_LORE("menu.pregame-menu.speed-mode-lore"),
    MENU_TEAM_CHEST_GIFT_LORE("menu.pregame-menu.team-chest-gift-lore"),
    MENU_TEAM_CHEST_COUNT("menu.pregame-menu.team-chest-count"),
    MENU_TEAM_CHEST_COUNT_LORE("menu.pregame-menu.team-chest-count-lore"),
    MENU_WAYPOINT_COUNT("menu.pregame-menu.waypoint-count"),
    MENU_WAYPOINT_COUNT_LORE("menu.pregame-menu.waypoint-count-lore"),
    MENU_ROLL_COUNT("menu.pregame-menu.roll-count"),
    MENU_ROLL_COUNT_LORE("menu.pregame-menu.roll-count-lore"),
    MENU_NETHER_MODE_LORE("menu.pregame-menu.nether-mode-lore"),
    MENU_SHARED_TEAM_SPAWN_LORE("menu.pregame-menu.shared-team-spawn-lore"),
    MENU_END_PORTAL_BROADCAST_LORE("menu.pregame-menu.end-portal-broadcast-lore"),
    MENU_PREGAME_INFO("menu.pregame-menu.info"),
    MENU_PREGAME_INFO_LORE("menu.pregame-menu.info-lore"),
    MENU_LOCKED_BY_NETHER("menu.pregame-menu.locked-by-nether"),
    MENU_SELECT_TEAM("menu.pregame-menu.select-team"),
    MENU_BLOCK_SETTING("menu.pregame-menu.block-setting"),
    MENU_SELECT_MODE("menu.pregame-menu.select-mode"),
    MENU_READY_AND_START("menu.pregame-menu.ready-and-start"),
    MENU_GAME_SETTING("menu.pregame-menu.game-setting"),
    MENU_GAME_TITLE("menu.game-menu.title"),
    MENU_TEAM_CHEST("menu.game-menu.team-chest"),
    MENU_TEAM_CHEST_LORE("menu.game-menu.team-chest-lore"),
    MENU_ROLL("menu.game-menu.roll"),
    MENU_ROLL_LORE("menu.game-menu.roll-lore"),
    MENU_ROLL_LORE_NORMAL("menu.game-menu.roll-lore-normal"),
    MENU_ROLL_LORE_CONTEST("menu.game-menu.roll-lore-contest"),
    MENU_LOCATE("menu.game-menu.locate"),
    MENU_LOCATE_LORE("menu.game-menu.locate-lore"),
    MENU_WAYPOINTS("menu.game-menu.waypoints"),
    MENU_WAYPOINTS_LORE("menu.game-menu.waypoints-lore"),
    MENU_RANDOM_TP("menu.game-menu.random-tp"),
    MENU_RANDOM_TP_LORE("menu.game-menu.random-tp-lore"),
    MENU_RANDOM_TP_DIM_OVERWORLD("menu.game-menu.random-tp-dimension-overworld"),
    MENU_RANDOM_TP_DIM_NETHER("menu.game-menu.random-tp-dimension-nether"),
    MENU_TEAM_CHEST_SELECT_TITLE("menu.team-chest-select-menu.title"),
    MENU_TEAM_CHEST_SELECT_CHEST("menu.team-chest-select-menu.chest"),
    MENU_TEAM_CHEST_SELECT_CHEST_LORE("menu.team-chest-select-menu.chest-lore"),
    MENU_RED_CHEST("menu.team-chest.red-chest"),
    MENU_BLUE_CHEST("menu.team-chest.blue-chest"),
    MENU_WAYPOINT_TITLE("menu.way-point.title"),
    MENU_WAYPOINT_EMPTY("menu.way-point.empty.waypoint"),
    MENU_WAYPOINT_EMPTY_LORE("menu.way-point.empty.lore"),
    MENU_WAYPOINT_FILLED("menu.way-point.filled.waypoint"),
    MENU_WAYPOINT_FILLED_LORE("menu.way-point.filled.lore"),
    MENU_WAYPOINT_BUTTON_FILLED("menu.way-point.button.filled"),
    MENU_WAYPOINT_BUTTON_EMPTY("menu.way-point.button.empty"),
    MENU_WAYPOINT_ACTION_TELEPORT("menu.way-point.action.teleport"),
    MENU_WAYPOINT_ACTION_SET("menu.way-point.action.set"),
    MENU_WAYPOINT_ACTION_RENAME("menu.way-point.action.rename"),
    MENU_WAYPOINT_ACTION_REMOVE("menu.way-point.action.remove"),
    MENU_WAYPOINT_ACTION_BACK("menu.way-point.action.back"),
    MENU_WAYPOINT_ACTION_CLOSE("menu.way-point.action.close"),
    MENU_WAYPOINT_DIALOG_FILLED_LORE("menu.way-point.dialog.filled.lore"),
    MENU_WAYPOINT_DIALOG_EMPTY_LORE("menu.way-point.dialog.empty.lore"),
    MENU_RANDOM_TEAM("menu.pregame-menu.random-team"),
    MENU_RANDOM_TEAM_LORE("menu.pregame-menu.random-team-lore"),
    MENU_TEAMMATE_TELEPORT("menu.game-menu.teammate-teleport"),
    MENU_TEAMMATE_TELEPORT_LORE("menu.game-menu.teammate-teleport-lore"),
    MENU_CURRENT_BLOCKS("menu.game-menu.current-blocks"),
    MENU_CURRENT_BLOCKS_LORE("menu.game-menu.current-blocks-lore"),
    MENU_SUPPLY("menu.game-menu.supply"),
    MENU_SUPPLY_LORE("menu.game-menu.supply-lore"),
    MENU_SUPPLY_TARGET_TEAM("menu.game-menu.supply-target-team"),
    MENU_SUPPLY_TARGET_SELF("menu.game-menu.supply-target-self"),
    MENU_SUPPLY_ITEM_LINE("menu.game-menu.supply-item-line"),
    MENU_GAME_INFO("menu.game-menu.info"),
    MENU_GAME_INFO_LORE("menu.game-menu.info-lore"),
    MENU_TEAMMATE_TELEPORT_TITLE("menu.teammate-teleport.title"),
    MENU_TEAMMATE_TELEPORT_EMPTY("menu.teammate-teleport.empty"),
    MENU_TEAMMATE_TELEPORT_EMPTY_LORE("menu.teammate-teleport.empty-lore"),
    MENU_TEAMMATE_TELEPORT_PLAYER_LORE("menu.teammate-teleport.player-lore"),
    MENU_ALL_RETURN_BACK("menu.all.return-back"),

    // chat
    CHAT_GLOBAL_FORMAT("chat.global-format"),
    CHAT_TEAM_HINT("chat.team-hint"),

    // rule book
    RULE_BOOK_TITLE("rule-book.title"),
    RULE_BOOK_AUTHOR("rule-book.author"),
    RULE_BOOK_PAGES("rule-book.pages"),

    // notice
    NOTICE_WELCOME("notice.welcome"),
    NOTICE_JOIN_RED("notice.join-red"),
    NOTICE_JOIN_BLUE("notice.join-blue"),
    NOTICE_ALREADY_IN_RED("notice.already-in-red"),
    NOTICE_ALREADY_IN_BLUE("notice.already-in-blue"),
    NOTICE_READY("notice.ready"),
    NOTICE_CANCEL_READY("notice.cancel-ready"),
    NOTICE_ALL_READY("notice.all-ready"),
    NOTICE_START("notice.start"),
    NOTICE_EXIST_UNREADY("notice.exist-unready"),
    NOTICE_UNREADY_PLAYERS("notice.unready-players"),
    NOTICE_NOT_ENOUGH_PLAYERS("notice.not-enough-players"),
    NOTICE_EMPTY_TEAM("notice.empty-team"),
    NOTICE_TOO_MUCH_BLOCKS("notice.too-much-blocks"),
    NOTICE_SET_BLOCKS("notice.set-blocks"),
    NOTICE_SET_BLOCKS_QUIT("notice.set-blocks-quit"),
    NOTICE_SET_BLOCKS_ERROR("notice.set-blocks-error"),
    NOTICE_SET_BLOCKS_SUCCESS("notice.set-blocks-success"),
    NOTICE_SET_TIME_MODE_MINUTES("notice.set-time-mode-minutes"),
    NOTICE_SET_TIME_MODE_MINUTES_SUCCESS("notice.set-time-mode-minutes-success"),
    NOTICE_SET_COMEBACK_THRESHOLD("notice.set-comeback-threshold"),
    NOTICE_SET_COMEBACK_THRESHOLD_SUCCESS("notice.set-comeback-threshold-success"),
    NOTICE_SET_COMEBACK_THRESHOLD_ERROR("notice.set-comeback-threshold-error"),
    NOTICE_SET_TEAM_CHEST_GIFT_AMOUNT("notice.set-team-chest-gift-amount"),
    NOTICE_SET_TEAM_CHEST_GIFT_AMOUNT_SUCCESS("notice.set-team-chest-gift-amount-success"),
    NOTICE_SET_TEAM_CHEST_GIFT_AMOUNT_ERROR("notice.set-team-chest-gift-amount-error"),
    NOTICE_SET_TEAM_CHEST_COUNT("notice.set-team-chest-count"),
    NOTICE_SET_TEAM_CHEST_COUNT_SUCCESS("notice.set-team-chest-count-success"),
    NOTICE_SET_TEAM_CHEST_COUNT_ERROR("notice.set-team-chest-count-error"),
    NOTICE_SET_WAYPOINT_COUNT("notice.set-waypoint-count"),
    NOTICE_SET_WAYPOINT_COUNT_SUCCESS("notice.set-waypoint-count-success"),
    NOTICE_SET_WAYPOINT_COUNT_ERROR("notice.set-waypoint-count-error"),
    NOTICE_SET_ROLL_COUNT("notice.set-roll-count"),
    NOTICE_SET_ROLL_COUNT_SUCCESS("notice.set-roll-count-success"),
    NOTICE_SET_ROLL_COUNT_ERROR("notice.set-roll-count-error"),
    NOTICE_SET_TEAM_CHEST_NAME("notice.set-team-chest-name"),
    NOTICE_SET_WAYPOINT_NAME("notice.set-waypoint-name"),
    NOTICE_SET_CUSTOM_NAME_QUIT("notice.set-custom-name-quit"),
    NOTICE_SET_CUSTOM_NAME_EMPTY("notice.set-custom-name-empty"),
    NOTICE_SET_TEAM_CHEST_NAME_SUCCESS("notice.set-team-chest-name-success"),
    NOTICE_SET_WAYPOINT_NAME_SUCCESS("notice.set-waypoint-name-success"),
    NOTICE_RESET_TEAM_CHEST_NAME_SUCCESS("notice.reset-team-chest-name-success"),
    NOTICE_RESET_WAYPOINT_NAME_SUCCESS("notice.reset-waypoint-name-success"),
    NOTICE_SPAWN_PROTECT("notice.spawn-protect-notice"),
    NOTICE_ERROR_BLOCK("notice.exist-error-block"),
    NOTICE_NOT_ENOUGH_SCORE("notice.not-enough-score"),
    NOTICE_RANDOM_TP("notice.random-tp"),
    NOTICE_RED_COLLECT("notice.red-collect"),
    NOTICE_BLUE_COLLECT("notice.blue-collect"),
    NOTICE_TEAM_CHEST_FULL("notice.team-chest-full"),
    NOTICE_RED_WIN("notice.red-win"),
    NOTICE_BLUE_WIN("notice.blue-win"),
    NOTICE_DRAW("notice.draw"),
    NOTICE_RED_TEAM_CHEST("notice.red-team-chest"),
    NOTICE_BLUE_TEAM_CHEST("notice.blue-team-chest"),
    NOTICE_BOTH_COLLECT_CONTEST("notice.both-collect-contest"),
    NOTICE_REMOVE_WAYPOINT("notice.remove-waypoint"),
    NOTICE_RELOAD_COMPLETE("notice.reload-complete"),
    NOTICE_RELOAD_COMPLETE_ERROR("notice.reload-complete-error"),
    NOTICE_NETHER_PORTAL_BLOCKED("notice.nether-portal-blocked"),
    NOTICE_TP_SUCCESS("notice.tp-success"),
    NOTICE_TP_OCEAN("notice.tp-ocean"),
    NOTICE_RED_REMOVE_WAYPOINT("notice.red-remove-waypoint"),
    NOTICE_BLUE_REMOVE_WAYPOINT("notice.blue-remove-waypoint"),
    NOTICE_CANNOT_ROLL("notice.cannot-roll"),
    NOTICE_CANNOT_ROLL_NO_CANDIDATE("notice.cannot-roll-no-candidate"),
    NOTICE_CANNOT_ROLL_CONTEST("notice.cannot-roll-contest"),
    NOTICE_ROLL_REQUEST("notice.roll-request"),
    NOTICE_ROLL_REQUEST_CONTEST("notice.roll-request-contest"),
    NOTICE_ROLL_REQUEST_CANCEL("notice.roll-request-cancel"),
    NOTICE_RED_ROLL_SUCCESS("notice.red-roll-success"),
    NOTICE_BLUE_ROLL_SUCCESS("notice.blue-roll-success"),
    NOTICE_SPECTATOR("notice.spectator"),
    NOTICE_SPECTATOR_JOIN("notice.spectator-join"),
    NOTICE_ERROR_COMMAND("notice.error-command"),
    NOTICE_LOCATE_COMMAND_HINT("notice.locate-command-hint"),
    NOTICE_LOCATE_NOT_ENOUGH_SCORE("notice.locate-not-enough-score"),
    NOTICE_LOCATE_BIOME_USAGE("notice.locate-biome-usage"),
    NOTICE_LOCATE_STRUCTURE_USAGE("notice.locate-structure-usage"),
    NOTICE_LOCATE_BIOME_INVALID("notice.locate-biome-invalid"),
    NOTICE_LOCATE_STRUCTURE_INVALID("notice.locate-structure-invalid"),
    NOTICE_LOCATE_BIOME_NOT_FOUND("notice.locate-biome-not-found"),
    NOTICE_LOCATE_STRUCTURE_NOT_FOUND("notice.locate-structure-not-found"),
    NOTICE_LOCATE_BIOME_SUCCESS("notice.locate-biome-success"),
    NOTICE_LOCATE_STRUCTURE_SUCCESS("notice.locate-structure-success"),
    NOTICE_LOCATE_BIOME_TEAM_SUCCESS("notice.locate-biome-team-success"),
    NOTICE_LOCATE_STRUCTURE_TEAM_SUCCESS("notice.locate-structure-team-success"),
    NOTICE_RESTART("notice.restart"),
    NOTICE_RESTART_CANCEL("notice.restart-cancel"),
    NOTICE_GAME_NOT_START("notice.game-not-start"),
    NOTICE_GAME_HAS_START("notice.game-has-start"),
    NOTICE_CANNOT_USE_COMMAND_INGAME("notice.cannot-use-command-ingame"),
    NOTICE_TP_PLAYER_SUCCESS("notice.tp-player-success"),
    NOTICE_SPECTATOR_TP_PLAYER_SUCCESS("notice.spectator-tp-player-success"),
    NOTICE_PLAYER_NOT_EXIST("notice.player-not-exist"),
    NOTICE_PLAYER_NOT_IN_SAME_TEAM("notice.player-not-in-same-team"),
    NOTICE_RANKING("notice.ranking"),
    NOTICE_RANKING_RED("notice.ranking-red"),
    NOTICE_RANKING_BLUE("notice.ranking-blue"),
    NOTICE_RANKING_OFFLINE("notice.ranking-offline"),
    NOTICE_VERSION_MISMATCH("notice.version-mismatch"),
    NOTICE_VERSION_MISMATCH_TITLE("notice.version-mismatch-title"),
    NOTICE_VERSION_MISMATCH_SUBTITLE("notice.version-mismatch-subtitle"),
    NOTICE_TEAM_SHUFFLE("notice.team-shuffle"),
    NOTICE_TEAM_SHUFFLE_CONFIRM("notice.team-shuffle-confirm"),
    NOTICE_TEAM_SHUFFLE_CONFIRM_BUTTON("notice.team-shuffle-confirm-button"),
    NOTICE_TEAM_SHUFFLE_CONFIRM_HOVER("notice.team-shuffle-confirm-hover"),
    NOTICE_TEAM_SHUFFLE_CONFIRM_EXPIRED("notice.team-shuffle-confirm-expired"),
    NOTICE_OVERTIME_START("notice.overtime-start"),
    NOTICE_COMEBACK_APPLY("notice.comeback-apply"),
    NOTICE_COMEBACK_CLEAR("notice.comeback-clear"),
    NOTICE_END_PORTAL_OPEN("notice.end-portal-open"),
    NOTICE_BLOCK_OVERVIEW_TITLE("notice.block-overview.title"),
    NOTICE_BLOCK_OVERVIEW_RED("notice.block-overview.red"),
    NOTICE_BLOCK_OVERVIEW_BLUE("notice.block-overview.blue"),
    NOTICE_BLOCK_OVERVIEW_ENTRY("notice.block-overview.entry"),
    NOTICE_BLOCK_OVERVIEW_DIVIDER("notice.block-overview.divider"),
    NOTICE_SUPPLY_PURCHASED("notice.supply-purchased"),
    NOTICE_SUPPLY_SPEED_ONLY("notice.supply-speed-only"),

    // other
    MESSAGE_PREFIX("prefix"),
    MESSAGE_VERSION("lang-version");

    private static final String DEFAULT_LANG = "zh_cn";
    private static File file;
    private static String languageCode;
    private String path;
    private String cacheString;
    private List<String> cacheStringList;
    private String cacheMiniMessage;

    Message(String path) {
        this.path = path;
    }

    public static void saveDefaultConfig() {
        languageCode = resolveLanguageCode();

        File langDir = new File(Main.getInstance().getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        File messageFile = new File(langDir, languageCode + ".yml");
        if (!messageFile.exists()) {
            String resourcePath = "lang/" + languageCode + ".yml";

            if (Main.getInstance().getResource(resourcePath) != null) {
                Main.getInstance().saveResource(resourcePath, false);
            } else {
                Main.getInstance().getLogger().warning("[BlockRacing] Language file '" + resourcePath + "' not found, using default '" + DEFAULT_LANG + "'.");
                languageCode = DEFAULT_LANG;
                messageFile = new File(langDir, languageCode + ".yml");
                if (!messageFile.exists()) {
                    Main.getInstance().saveResource("lang/" + languageCode + ".yml", false);
                }
            }
        }
    }

    public static void load() {
        languageCode = resolveLanguageCode();
        file = new File(Main.getInstance().getDataFolder(), "lang/" + languageCode + ".yml");

        if (!file.exists()) {
            languageCode = DEFAULT_LANG;
            file = new File(Main.getInstance().getDataFolder(), "lang/" + languageCode + ".yml");
        }

        for (Message m : values()) {
            m.cacheString = null;
            m.cacheMiniMessage = null;
            m.cacheStringList = null;
        }
    }

    private static FileConfiguration getMessageConfig() {
        if (languageCode == null) {
            languageCode = resolveLanguageCode();
        }

        if (file == null) {
            file = new File(Main.getInstance().getDataFolder(), "lang/" + languageCode + ".yml");
        }

        FileConfiguration messageConfig = YamlConfiguration.loadConfiguration(file);

        String resourcePath = "lang/" + languageCode + ".yml";
        try (Reader reader = new InputStreamReader(Main.getInstance().getResource(resourcePath), StandardCharsets.UTF_8)) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
            messageConfig.setDefaults(defConfig);
        } catch (IOException | NullPointerException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "Error reading " + resourcePath + "!", e);
            if (!DEFAULT_LANG.equals(languageCode)) {
                try (Reader reader = new InputStreamReader(Main.getInstance().getResource("lang/" + DEFAULT_LANG + ".yml"), StandardCharsets.UTF_8)) {
                    YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
                    messageConfig.setDefaults(defConfig);
                } catch (IOException | NullPointerException ex) {
                    Main.getInstance().getLogger().log(Level.SEVERE, "Error reading default language file!", ex);
                }
            }
        }

        return messageConfig;
    }

    public String getString() {
        if (cacheString != null) return cacheString;
        String raw = getRawValue();
        // Replace %prefix% with the raw prefix MiniMessage string before converting
        raw = raw.replace("%prefix%", Message.MESSAGE_PREFIX.getRawValue());
        return cacheString = MiniMessageUtil.toLegacyString(raw);
    }

    /** Per-player language aware string resolution. */
    public String getString(org.bukkit.entity.Player player) {
        if (player == null) return getString();
        try {
            return LanguageManager.getString(this, player);
        } catch (Exception ex) {
            return getString();
        }
    }

    /** Returns the config path for this message key. */
    public String getPath() {
        return path;
    }

    /**
     * Returns the raw MiniMessage string for this key with built-in prefix substitution, without
     * converting to legacy. Useful for Adventure or when further placeholder replacement is needed.
     */
    public String getMiniMessage() {
        if (cacheMiniMessage != null) return cacheMiniMessage;
        String raw = getRawValue();
        if (raw == null) return "";
        return cacheMiniMessage = raw.replace("%prefix%", Message.MESSAGE_PREFIX.getRawValue());
    }

    public List<String> getStringList() {
        if (cacheStringList != null) return cacheStringList;
        cacheStringList = Collections.unmodifiableList(
                getMessageConfig().getStringList(path).stream()
                        .map(msg -> {
                            String replaced = msg == null ? "" : msg.replace("%prefix%", Message.MESSAGE_PREFIX.getRawValue());
                            return MiniMessageUtil.toLegacyString(replaced);
                        })
                        .collect(Collectors.toList())
        );
        return cacheStringList;
    }

    /** Per-player language aware string list resolution. */
    public List<String> getStringList(org.bukkit.entity.Player player) {
        if (player == null) return getStringList();
        try {
            return LanguageManager.getStringList(this, player);
        } catch (Exception ex) {
            return getStringList();
        }
    }

    private String getRawValue() {
        String value = getMessageConfig().getString(path);
        return value == null ? "" : value;
    }

    public static String getLanguageCode() {
        if (languageCode == null) {
            languageCode = resolveLanguageCode();
        }
        return languageCode;
    }

    public static String getDefaultLanguageCode() {
        return DEFAULT_LANG;
    }

    private static String resolveLanguageCode() {
        try {
            String fromConfig = Config.LANG.getString();
            if (fromConfig == null || fromConfig.isBlank()) {
                return DEFAULT_LANG;
            }
            return fromConfig.toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return DEFAULT_LANG;
        }
    }
}
