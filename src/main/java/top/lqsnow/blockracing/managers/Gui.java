package top.lqsnow.blockracing.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import top.lqsnow.blockracing.menus.GameMenu;
import top.lqsnow.blockracing.menus.PreGameMenu;
import top.lqsnow.blockracing.toolkit.menu.MenuManager;
import top.lqsnow.blockracing.toolkit.menu.MenuView;

import static top.lqsnow.blockracing.managers.Game.redTeamChest;
import static top.lqsnow.blockracing.managers.Game.blueTeamChest;
import static top.lqsnow.blockracing.managers.Game.currentGameState;
import static top.lqsnow.blockracing.managers.Team.blueTeamPlayers;
import static top.lqsnow.blockracing.managers.Team.redTeamPlayers;

public class Gui {
    public static Inventory checkBlockInventory = Bukkit.createInventory(null, 9);

    public static void openMenu(Player player) {
        if (currentGameState.equals(Game.GameState.PREGAME)) new PreGameMenu().open(player);
        if (currentGameState.equals(Game.GameState.INGAME)) new GameMenu().open(player);
    }

    @SuppressWarnings("deprecation") // InventoryView has no component-based title setter in Paper 26.3.
    public static void openTeamChest(Player player, int index) {
        if (redTeamPlayers.contains(player.getName())) {
            player.openInventory(redTeamChest.get(index));
            player.getOpenInventory().setTitle(Message.MENU_RED_CHEST.getString() + (index + 1));
        } else if (blueTeamPlayers.contains(player.getName())) {
            player.openInventory(blueTeamChest.get(index));
            player.getOpenInventory().setTitle(Message.MENU_BLUE_CHEST.getString() + (index + 1));
        }
    }

    public static void closeAllPlayersMenu() {
        Bukkit.getOnlinePlayers().forEach((Player player) -> {
            player.closeInventory();
        });
    }

    // Update menu
    public static void updateMenu(MenuView menu) {
        MenuManager.refreshOpenMenus(menu.getClass());
    }
}
