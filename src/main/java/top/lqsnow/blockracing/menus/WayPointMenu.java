package top.lqsnow.blockracing.menus;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.managers.Setting;
import top.lqsnow.blockracing.toolkit.item.ItemBuilder;
import top.lqsnow.blockracing.toolkit.menu.MenuButton;
import top.lqsnow.blockracing.toolkit.menu.MenuView;
import top.lqsnow.blockracing.utils.WorldTranslation;

import java.util.List;
import java.util.Map;

import static top.lqsnow.blockracing.managers.Game.getCoords;
import static top.lqsnow.blockracing.managers.Game.waypoint;
import static top.lqsnow.blockracing.managers.Gui.updateMenu;

/**
 * 传送点的箱子菜单实现（每行左侧显示方块图标）。
 *
 * <p>主菜单的记录点按钮【右键】打开此菜单，【左键】打开 {@link WayPointDialog} 列表菜单。</p>
 *
 * <p>渲染时绝不触碰 {@code waypoint.getBlock()/getBiome()} 等区块数据（Folia 线程限制），
 * 图标一律从缓存读取，缺失时回退为环境图标（主世界草方块 / 下界地狱岩 / 末地末地石）。</p>
 */
public final class WayPointMenu extends MenuView {
    private final Map<Integer, Location> waypoints;
    private final Map<Integer, Material> iconCache;
    private final Map<Integer, String> biomeCache;

    public WayPointMenu(Map<Integer, Location> waypoints, Map<Integer, Material> iconCache, Map<Integer, String> biomeCache) {
        super(GameMenu.menuSize(Setting.getMaxTeamWaypointNum()),
                player -> Message.MENU_WAYPOINT_TITLE.getString(player));
        this.waypoints = waypoints;
        this.iconCache = iconCache;
        this.biomeCache = biomeCache;

        for (int slot = 0; slot < Setting.getMaxTeamWaypointNum(); slot++) {
            int index = slot + 1;
            setButton(slot, MenuButton.of(
                    player -> createWaypointItem(player, index),
                    (player, click) -> {
                        if (waypoint(player, index, click)) {
                            updateMenu(this);
                        }
                    }
            ));
        }
        setButton(getSize() - 1, GameMenu.backButton());
    }

    private ItemStack createWaypointItem(Player player, int index) {
        Location waypoint = waypoints.get(index);
        if (waypoint == null) {
            iconCache.remove(index);
            return ItemBuilder.of(Material.MAP)
                    .name(Message.MENU_WAYPOINT_EMPTY.getString(player) + index)
                    .lore(Message.MENU_WAYPOINT_EMPTY_LORE.getStringList(player))
                    .build();
        }

        try {
            // Folia-safe: never touch waypoint.getBlock()/getBiome() while rendering the menu.
            Material icon = iconCache.getOrDefault(index, findWaypointIconFallback(waypoint));
            String dimensionName = WorldTranslation.getValue(waypoint.getWorld());
            String biomeName = biomeCache.getOrDefault(index, "N/A");
            return ItemBuilder.of(icon)
                    .name(Message.MENU_WAYPOINT_FILLED.getString(player) + index)
                    .lore(GameMenu.replaceWaypointPlaceholders(
                            Message.MENU_WAYPOINT_FILLED_LORE.getStringList(player),
                            dimensionName,
                            getCoords(waypoint),
                            biomeName
                    ))
                    .build();
        } catch (Exception ex) {
            // A waypoint whose biome/icon/world cannot be resolved (e.g. Folia thread-restricted
            // biome lookup, unloaded region) must never block the whole menu from opening.
            iconCache.put(index, Material.FILLED_MAP);
            return ItemBuilder.of(Material.FILLED_MAP)
                    .name(Message.MENU_WAYPOINT_FILLED.getString(player) + index)
                    .lore(GameMenu.replaceWaypointPlaceholders(
                            Message.MENU_WAYPOINT_FILLED_LORE.getStringList(player),
                            waypoint.getWorld() == null ? "" : WorldTranslation.getValue(waypoint.getWorld()),
                            getCoords(waypoint),
                            "N/A"
                    ))
                    .build();
        }
    }

    /** Folia-safe icon fallback: only reads world environment, never block/chunk data. */
    private Material findWaypointIconFallback(Location waypoint) {
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
}
