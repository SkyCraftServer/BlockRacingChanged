package top.lqsnow.blockracing.menus;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.managers.Setting;
import top.lqsnow.blockracing.toolkit.text.Texts;
import top.lqsnow.blockracing.utils.WorldTranslation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static top.lqsnow.blockracing.managers.Team.blueTeamPlayers;
import static top.lqsnow.blockracing.managers.Team.redTeamPlayers;

/**
 * 传送点菜单的 Dialog API 实现。
 *
 * <p>使用 {@link DialogType#dialogList} 渲染一个由“图标 + 长按钮”组成的列表，
 * 每个列表项对应一个传送点槽位。点击列表项会打开对应的子对话框，
 * 子对话框内提供传送 / 设置 / 重命名 / 删除等操作按钮。</p>
 *
 * <p>所有点击回调都在 Paper 的 netty 线程执行，因此实际动作统一通过
 * FoliaLib 的 {@code runAtEntity} 调度回玩家所在的区域线程（非 Folia 即主线程）。</p>
 */
public final class WayPointDialog {

    /** 列表按钮的宽度（数值越大按钮越长）。 */
    private static final int BUTTON_WIDTH = 320;
    /** 列表列数，单列以保证按钮足够长。 */
    private static final int LIST_COLUMNS = 1;

    private WayPointDialog() {
    }

    /** 打开传送点对话框列表。 */
    public static void open(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        String team = resolveTeam(player);
        if (team.isEmpty()) {
            return;
        }

        // 关闭可能仍打开的箱子菜单，避免对话框关闭（如传送后）再次显示旧的箱子界面。
        player.closeInventory();

        // 每个传送点槽位对应一个子对话框（图标取自脚下的方块）
        List<Dialog> subDialogs = new ArrayList<>();
        for (int index = 1; index <= Setting.getMaxTeamWaypointNum(); index++) {
            subDialogs.add(buildWaypointDialog(player, team, index));
        }
        RegistrySet<Dialog> dialogs = RegistrySet.valueSet(RegistryKey.DIALOG, subDialogs);

        ActionButton exit = ActionButton.builder(Texts.component(Message.MENU_WAYPOINT_ACTION_CLOSE.getString(player)))
                .width(BUTTON_WIDTH)
                .build();

        DialogType type = DialogType.dialogList(dialogs)
                .exitAction(exit)
                .columns(LIST_COLUMNS)
                .buttonWidth(BUTTON_WIDTH)
                .build();

        DialogBase base = DialogBase.builder(Texts.component(Message.MENU_WAYPOINT_TITLE.getString(player)))
                .canCloseWithEscape(true)
                .build();

        Dialog menu = Dialog.create(builder -> builder.empty().base(base).type(type));
        player.showDialog(menu);
    }

    /** 构建单个传送点槽位的子对话框。 */
    private static Dialog buildWaypointDialog(Player player, String team, int index) {
        Location waypoint = Game.getWaypoint(team, index);
        boolean filled = waypoint != null;

        List<DialogBody> body = new ArrayList<>();
        body.add(iconBody(team, index, filled));
        body.add(detailBody(player, team, index, filled));

        // Dialog 不支持左右键区分，直接调用按编号操作的方法，不复用 ClickType 语义。
        List<ActionButton> actions = new ArrayList<>();
        if (filled) {
            actions.add(actionButton(Message.MENU_WAYPOINT_ACTION_TELEPORT.getString(player),
                    p -> Game.teleportToWaypoint(p, index)));
            actions.add(actionButton(Message.MENU_WAYPOINT_ACTION_RENAME.getString(player),
                    p -> Game.beginWaypointRename(p, index)));
            actions.add(actionButton(Message.MENU_WAYPOINT_ACTION_REMOVE.getString(player),
                    p -> removeWaypoint(p, index)));
        } else {
            actions.add(actionButton(Message.MENU_WAYPOINT_ACTION_SET.getString(player),
                    p -> setWaypoint(p, index)));
        }
        actions.add(actionButton(Message.MENU_WAYPOINT_ACTION_BACK.getString(player), WayPointDialog::open));

        DialogBase base = DialogBase.builder(Texts.component(Game.getWaypointDisplayName(team, index, filled)))
                .externalTitle(waypointButtonLabel(player, team, index, filled))
                .body(body)
                .canCloseWithEscape(true)
                .build();
        DialogType type = DialogType.multiAction(actions).columns(1).build();
        return Dialog.create(builder -> builder.empty().base(base).type(type));
    }

    /** 空槽位：直接创建传送点，创建成功后刷新列表。 */
    private static void setWaypoint(Player player, int index) {
        Game.setWaypoint(player, index);
        open(player);
    }

    /** 已占用槽位：直接删除传送点，删除成功后刷新列表。 */
    private static void removeWaypoint(Player player, int index) {
        if (Game.removeWaypoint(player, index)) {
            open(player);
        }
    }

    /** 列表行左侧的图标：脚下的方块（未设置时使用地图）。 */
    private static DialogBody iconBody(String team, int index, boolean filled) {
        Material icon = filled ? Game.getWaypointIcon(team, index) : Material.MAP;
        // 兜底：图标必须为有效物品，否则 ItemDialogBody 校验会抛 "isn't an item"。
        if (icon == null || !icon.isItem()) {
            icon = Material.FILLED_MAP;
        }
        return DialogBody.item(new ItemStack(icon))
                .showTooltip(false)
                .showDecorations(false)
                .build();
    }

    /** 子对话框正文：显示维度 / 坐标 / 群系等细节。 */
    private static DialogBody detailBody(Player player, String team, int index, boolean filled) {
        // Dialog 不支持 shift+左键 / 左右键区分操作，详情只显示信息，不显示鼠标操作说明。
        if (!filled) {
            return DialogBody.plainMessage(
                    Texts.component(String.join("\n", Message.MENU_WAYPOINT_DIALOG_EMPTY_LORE.getStringList(player))));
        }
        Location waypoint = Game.getWaypoint(team, index);
        String dimension = waypoint.getWorld() == null ? "" : WorldTranslation.getValue(waypoint.getWorld());
        String coords = Game.getCoords(waypoint);
        String biome = Game.getWaypointBiome(team, index);
        List<String> lines = new ArrayList<>();
        for (String line : Message.MENU_WAYPOINT_DIALOG_FILLED_LORE.getStringList(player)) {
            lines.add(line.replace("%dimension%", dimension).replace("%coords%", coords).replace("%biome%", biome));
        }
        return DialogBody.plainMessage(Texts.component(String.join("\n", lines)));
    }

    /** 列表按钮上显示的名称：{@code 1# 主世界-群系名 坐标}（多语言适配）。 */
    private static Component waypointButtonLabel(Player player, String team, int index, boolean filled) {
        String template;
        if (filled) {
            Location waypoint = Game.getWaypoint(team, index);
            String dimension = waypoint.getWorld() == null ? "" : WorldTranslation.getValue(waypoint.getWorld());
            String biome = Game.getWaypointBiome(team, index);
            String coords = Game.getCoords(waypoint);
            template = Message.MENU_WAYPOINT_BUTTON_FILLED.getString(player)
                    .replace("%index%", String.valueOf(index))
                    .replace("%dimension%", dimension)
                    .replace("%biome%", biome)
                    .replace("%coords%", coords);
        } else {
            template = Message.MENU_WAYPOINT_BUTTON_EMPTY.getString(player)
                    .replace("%index%", String.valueOf(index));
        }
        return Texts.component(template);
    }

    private static ActionButton actionButton(String label, Consumer<Player> action) {
        return ActionButton.builder(Texts.component(label))
                .width(BUTTON_WIDTH)
                .action(clickAction(action))
                .build();
    }

    /**
     * 注册对话框按钮回调。
     *
     * <p>回调在 netty 事件循环线程执行，不能直接触碰 Bukkit 世界 / 玩家 API，
     * 因此通过 FoliaLib 调度到玩家所在区域线程（非 Folia 即主线程）后再执行动作。</p>
     */
    private static DialogAction clickAction(Consumer<Player> action) {
        DialogActionCallback callback = (view, audience) -> {
            if (audience instanceof Player player && Main.getFoliaLib() != null) {
                Main.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                    if (player.isOnline()) {
                        action.accept(player);
                    }
                });
            }
        };
        return DialogAction.customClick(callback, ClickCallback.Options.builder()
                .uses(1)
                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                .build());
    }

    private static String resolveTeam(Player player) {
        if (redTeamPlayers.contains(player.getName())) {
            return "red";
        }
        if (blueTeamPlayers.contains(player.getName())) {
            return "blue";
        }
        return "";
    }
}
