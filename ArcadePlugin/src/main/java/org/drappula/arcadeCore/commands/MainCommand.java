package org.drappula.arcadeCore.commands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.drappula.arcadeApi.message.Messages;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.map.IArcadeMap;
import org.drappula.arcadeApi.systems.map.MapConfigOption;
import org.drappula.arcadeApi.systems.queue.JoinResult;
import org.drappula.arcadeCore.ArcadeCore;
import org.drappula.arcadeCore.config.DataConfig;
import org.drappula.arcadeCore.config.MainConfig;
import org.drappula.arcadeCore.config.MessagesConfig;
import org.drappula.arcadeCore.managers.game.GameManager;
import org.drappula.arcadeCore.managers.map.ArcadeMap;
import org.drappula.arcadeCore.managers.map.MapManager;
import org.drappula.arcadeCore.managers.queue.QueueManager;
import org.drappula.arcadeCore.util.Log;
import org.drappula.arcadeCore.util.MessageUtil;

/**
 * {@code /arcade}. A plain Bukkit executor so it works on every server version; there is no Brigadier,
 * so argument checking and tab completion are done by hand below.
 */
public class MainCommand implements CommandExecutor, TabCompleter {
    private static final List<String> ROOT = Arrays.asList("reload", "setspawn", "start", "queue", "map");
    private static final List<String> MAP = Arrays.asList("create", "addspawn", "enable", "disable", "list", "delete", "config");
    private static final List<String> CONFIG = Arrays.asList("set", "unset", "list");

    private static final String USAGE = "<red>Usage: /arcade [reload|setspawn|start <game>|queue <game>|map ...]";
    private static final String DB_FAILED = "<red>Failed to update the map. See the console for more details.";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            info(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload":
                reload(sender);
                return true;
            case "setspawn":
                setSpawn(sender);
                return true;
            case "start":
                if (!sender.hasPermission("arcade.force-start")) return denied(sender);
                if (args.length < 2) return usage(sender);
                start(sender, args[1]);
                return true;
            case "queue":
                if (args.length < 2) return usage(sender);
                queue(sender, args[1]);
                return true;
            case "map":
                if (!sender.hasPermission("arcade.map.admin")) return denied(sender);
                return map(sender, args);
            default:
                return usage(sender);
        }
    }

    private static boolean usage(CommandSender sender) {
        Messages.chat(sender, USAGE);
        return true;
    }

    private static boolean denied(CommandSender sender) {
        Messages.chat(sender, "<red>You do not have permission to use this command.");
        return true;
    }

    private boolean map(CommandSender sender, String[] args) {
        if (args.length < 2) return usage(sender);
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create":
                if (args.length < 4) return usage(sender);
                mapCreate(sender, args[2], args[3]);
                return true;
            case "addspawn":
                if (args.length < 3) return usage(sender);
                mapAddSpawn(sender, args[2]);
                return true;
            case "enable":
                if (args.length < 3) return usage(sender);
                mapSetEnabled(sender, args[2], true);
                return true;
            case "disable":
                if (args.length < 3) return usage(sender);
                mapSetEnabled(sender, args[2], false);
                return true;
            case "list":
                mapList(sender, args.length > 2 ? args[2] : null);
                return true;
            case "delete":
                if (args.length < 3) return usage(sender);
                mapDelete(sender, args[2]);
                return true;
            case "config":
                return mapConfig(sender, args);
            default:
                return usage(sender);
        }
    }

    private boolean mapConfig(CommandSender sender, String[] args) {
        if (args.length < 4) return usage(sender);
        String mapId = args[2];
        switch (args[3].toLowerCase(Locale.ROOT)) {
            case "set":
                if (args.length < 6) return usage(sender);
                mapConfigSet(sender, mapId, args[4], String.join(" ", Arrays.copyOfRange(args, 5, args.length)));
                return true;
            case "unset":
                if (args.length < 5) return usage(sender);
                mapConfigUnset(sender, mapId, args[4]);
                return true;
            case "list":
                mapConfigList(sender, mapId);
                return true;
            default:
                return usage(sender);
        }
    }

    // ---- tab completion -------------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<String>();
        if (args.length == 1) {
            // Only offer what the sender may run, like the old Brigadier tree did.
            for (String root : ROOT) {
                if (root.equals("start") && !sender.hasPermission("arcade.force-start")) continue;
                if (root.equals("map") && !sender.hasPermission("arcade.map.admin")) continue;
                options.add(root);
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("start") && sender.hasPermission("arcade.force-start")) options.addAll(gameIds());
            else if (args[0].equalsIgnoreCase("queue")) options.addAll(gameIds());
            else if (args[0].equalsIgnoreCase("map") && sender.hasPermission("arcade.map.admin")) options.addAll(MAP);
        } else if (args[0].equalsIgnoreCase("map") && sender.hasPermission("arcade.map.admin")) {
            options.addAll(mapOptions(args));
        }
        return startingWith(options, args[args.length - 1]);
    }

    private List<String> mapOptions(String[] args) {
        String sub = args[1].toLowerCase(Locale.ROOT);
        if (args.length == 3) {
            if (sub.equals("create")) return new ArrayList<String>();
            if (sub.equals("list")) return gameIds();
            return mapIds();
        }
        if (args.length == 4 && sub.equals("create")) return gameIds();
        if (sub.equals("config")) {
            if (args.length == 4) return CONFIG;
            if (args.length == 5 && (args[3].equalsIgnoreCase("set") || args[3].equalsIgnoreCase("unset"))) {
                return configKeys(args[2]);
            }
        }
        return new ArrayList<String>();
    }

    private static List<String> startingWith(Collection<String> options, String typed) {
        List<String> out = new ArrayList<String>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(typed.toLowerCase(Locale.ROOT))) out.add(option);
        }
        return out;
    }

    private static List<String> gameIds() {
        List<String> ids = new ArrayList<String>();
        for (Game game : GameManager.get().getGames().values()) ids.add(game.getId());
        return ids;
    }

    private static List<String> mapIds() {
        List<String> ids = new ArrayList<String>();
        for (Game game : GameManager.get().getGames().values()) {
            for (IArcadeMap map : MapManager.get().getMaps(game.getId())) ids.add(map.getId());
        }
        return ids;
    }

    private static List<String> configKeys(String mapId) {
        List<String> keys = new ArrayList<String>();
        ArcadeMap map = MapManager.get().getMap(mapId);
        Game game = map == null ? null : GameManager.get().getGame(map.getGameId());
        if (game != null) {
            for (MapConfigOption option : game.getMapConfigOptions()) keys.add(option.key());
        }
        return keys;
    }

    // ---- handlers -------------------------------------------------------------------------------

    private static MapConfigOption findConfigOption(Game game, String key) {
        if (game == null) return null;
        for (MapConfigOption option : game.getMapConfigOptions()) {
            if (option.key().equals(key)) return option;
        }
        return null;
    }

    private static String registeredKeys(Game game) {
        if (game == null || game.getMapConfigOptions().isEmpty()) return "(none)";
        StringBuilder keys = new StringBuilder();
        for (MapConfigOption option : game.getMapConfigOptions()) {
            if (keys.length() > 0) keys.append(", ");
            keys.append(option.key());
        }
        return keys.toString();
    }

    private static void unknownMap(CommandSender sender, String mapId) {
        MessageUtil.sendMessage(sender, MessagesConfig.get().getString("map-not-found"), "id", mapId);
    }

    private static void unknownGame(CommandSender sender, String gameId) {
        MessageUtil.sendMessage(sender, MessagesConfig.get().getString("bad-arguments.unknown-game"), "id", gameId);
    }

    private void start(CommandSender sender, String gameId) {
        Game game = GameManager.get().getGame(gameId);
        if (game == null) {
            Messages.chat(sender, "<red>Unknown game: <gray><id></gray>", "id", gameId);
            return;
        }
        if (!QueueManager.get().forceStart(game)) {
            Messages.chat(sender, "<red>No players are queued for that game.");
        }
    }

    private void queue(CommandSender sender, String gameId) {
        Game game = GameManager.get().getGame(gameId);
        if (game == null) {
            unknownGame(sender, gameId);
            return;
        }
        if (!(sender instanceof Player)) {
            Messages.chat(sender, "<red>Only players can join the queue.");
            return;
        }
        Player player = (Player) sender;
        JoinResult result = QueueManager.get().joinQueue(player, game);
        String key = result == JoinResult.SUCCESS ? "queue-joined" : "queue-denied";
        MessageUtil.sendMessage(player, MessagesConfig.get().getString(key), "game", gameId);
    }

    private void mapCreate(CommandSender sender, String mapId, String gameId) {
        Game game = GameManager.get().getGame(gameId);
        if (game == null) {
            unknownGame(sender, gameId);
            return;
        }
        try {
            MapManager.get().createMap(mapId, gameId, mapId, locationOf(sender).getWorld().getName());
            MessageUtil.sendMessage(sender, MessagesConfig.get().getString("map-created"), "id", mapId, "game", gameId);
        } catch (SQLException e) {
            Messages.chat(sender, "<red>Failed to create the map. See the console for more details.");
            Log.error("Failed to create map {}", mapId, e);
        }
    }

    private void mapAddSpawn(CommandSender sender, String mapId) {
        if (MapManager.get().getMap(mapId) == null) {
            unknownMap(sender, mapId);
            return;
        }
        // A console/RCON/command-block sender has no position of its own to store.
        if (!(sender instanceof Player)) {
            Messages.chat(sender, "<red>Only players can add spawn points (uses your position).");
            return;
        }
        try {
            MapManager.get().addSpawn(mapId, ((Player) sender).getLocation());
            Messages.chat(sender, "<green>Added a spawn point to map <gray><id></gray>.", "id", mapId);
        } catch (SQLException e) {
            Messages.chat(sender, "<red>Failed to add the spawn point. See the console for more details.");
            Log.error("Failed to add a spawn point to map {}", mapId, e);
        }
    }

    private void mapSetEnabled(CommandSender sender, String mapId, boolean enabled) {
        if (MapManager.get().getMap(mapId) == null) {
            unknownMap(sender, mapId);
            return;
        }
        try {
            MapManager.get().setEnabled(mapId, enabled);
            Messages.chat(sender, enabled ? "<green>Map <gray><id></gray> enabled." : "<yellow>Map <gray><id></gray> disabled.", "id", mapId);
        } catch (SQLException e) {
            Messages.chat(sender, DB_FAILED);
            Log.error("Failed to set enabled={} on map {}", enabled, mapId, e);
        }
    }

    private void mapList(CommandSender sender, String gameId) {
        // Ids go in as placeholder values (m0, g0, ...) so an id that looks like markup prints as typed.
        StringBuilder message = new StringBuilder("<aqua>Maps:</aqua>");
        List<String> values = new ArrayList<String>();
        for (Game game : GameManager.get().getGames().values()) {
            if (gameId != null && !game.getId().equalsIgnoreCase(gameId)) continue;
            for (IArcadeMap map : MapManager.get().getMaps(game.getId())) {
                int n = values.size() / 4;
                values.add("m" + n);
                values.add(map.getId());
                values.add("g" + n);
                values.add(game.getId());
                message.append("<br><gray> - <m").append(n).append("> (<g").append(n).append(">)")
                        .append(map.isEnabled() ? "" : " <red>[disabled]</red>")
                        .append(map.isInUse() ? " <yellow>[in use]</yellow>" : "");
            }
        }
        Messages.chat(sender, message.toString(), values.toArray(new String[0]));
    }

    private void mapConfigSet(CommandSender sender, String mapId, String key, String value) {
        ArcadeMap map = MapManager.get().getMap(mapId);
        if (map == null) {
            unknownMap(sender, mapId);
            return;
        }
        Game game = GameManager.get().getGame(map.getGameId());
        MapConfigOption option = findConfigOption(game, key);
        if (option == null) {
            MessageUtil.sendMessage(sender, MessagesConfig.get().getString("map-config-unknown-key"),
                    "key", key, "keys", registeredKeys(game));
            return;
        }
        String canonical;
        try {
            canonical = option.validateAndCanonicalize(value);
        } catch (IllegalArgumentException e) {
            MessageUtil.sendMessage(sender, MessagesConfig.get().getString("map-config-invalid-value"),
                    "key", key, "expected", e.getMessage());
            return;
        }
        try {
            MapManager.get().setMapConfig(mapId, key, canonical);
            MessageUtil.sendMessage(sender, MessagesConfig.get().getString("map-config-set"),
                    "id", mapId, "key", key, "value", canonical);
        } catch (SQLException e) {
            Messages.chat(sender, DB_FAILED);
            Log.error("Failed to set config {} on map {}", key, mapId, e);
        }
    }

    private void mapConfigUnset(CommandSender sender, String mapId, String key) {
        ArcadeMap map = MapManager.get().getMap(mapId);
        if (map == null) {
            unknownMap(sender, mapId);
            return;
        }
        Game game = GameManager.get().getGame(map.getGameId());
        if (findConfigOption(game, key) == null) {
            MessageUtil.sendMessage(sender, MessagesConfig.get().getString("map-config-unknown-key"),
                    "key", key, "keys", registeredKeys(game));
            return;
        }
        if (!map.getConfigOverrides().containsKey(key)) {
            MessageUtil.sendMessage(sender, MessagesConfig.get().getString("map-config-no-override"), "id", mapId, "key", key);
            return;
        }
        try {
            MapManager.get().deleteMapConfig(mapId, key);
            MessageUtil.sendMessage(sender, MessagesConfig.get().getString("map-config-unset"), "id", mapId, "key", key);
        } catch (SQLException e) {
            Messages.chat(sender, DB_FAILED);
            Log.error("Failed to delete config {} on map {}", key, mapId, e);
        }
    }

    private void mapConfigList(CommandSender sender, String mapId) {
        ArcadeMap map = MapManager.get().getMap(mapId);
        if (map == null) {
            unknownMap(sender, mapId);
            return;
        }
        Game game = GameManager.get().getGame(map.getGameId());
        if (game == null || game.getMapConfigOptions().isEmpty()) {
            Messages.chat(sender, "<yellow>Map <gray><id></gray> has no config options.", "id", mapId);
            return;
        }
        StringBuilder message = new StringBuilder("<aqua>Config for <id>:</aqua>");
        List<String> values = new ArrayList<String>(Arrays.asList("id", mapId));
        int n = 0;
        for (MapConfigOption option : game.getMapConfigOptions()) {
            String override = map.getConfigOverrides().get(option.key());
            // Keys and values go in as placeholder values so text that looks like markup prints as typed.
            message.append("<br><gray> - <k").append(n).append(">=<v").append(n).append(">");
            values.add("k" + n);
            values.add(option.key());
            values.add("v" + n);
            values.add(override != null ? override : option.defaultValue());
            if (override != null) message.append(" <yellow>[override]</yellow>");
            n++;
        }
        Messages.chat(sender, message.toString(), values.toArray(new String[0]));
    }

    private void mapDelete(CommandSender sender, String mapId) {
        if (MapManager.get().getMap(mapId) == null) {
            unknownMap(sender, mapId);
            return;
        }
        try {
            MapManager.get().deleteMap(mapId);
            Messages.chat(sender, "<green>Deleted map <gray><id></gray>.", "id", mapId);
        } catch (SQLException e) {
            Messages.chat(sender, "<red>Failed to delete the map. See the console for more details.");
            Log.error("Failed to delete map {}", mapId, e);
        }
    }

    private void info(CommandSender sender) {
        org.bukkit.plugin.PluginDescriptionFile meta = ArcadeCore.get().getDescription();
        String description = meta.getDescription();
        Messages.chat(sender, "<aqua><b><name></b></aqua> <dark_gray><i>(v<version>)</i></dark_gray><br><gray><description>",
                "name", meta.getName(), "description", description == null ? "" : description, "version", meta.getVersion());
    }

    private void reload(CommandSender sender) {
        try {
            DataConfig.get().reload();
            MainConfig.get().reload();
            MessagesConfig.get().reload();
            Messages.chat(sender, "<green>Reloaded the plugin configuration.");
        } catch (Exception e) {
            Messages.chat(sender, "<red>Failed to reload the plugin! See the console for more details.");
            throw new RuntimeException(e);
        }
    }

    private void setSpawn(CommandSender sender) {
        try {
            // A console or RCON has no position, so it sets the default world's spawn (as it did with Brigadier).
            DataConfig.setSpawnLocation(locationOf(sender));
            Messages.chat(sender, "<green>Spawn location updated.");
        } catch (Exception e) {
            Messages.chat(sender, "<red>An error occurred while trying to save spawn location. See the console for more details.");
            Log.error("An error occurred while trying to save spawn location", e);
        }
    }

    /** The sender's position, or the default world's spawn for senders that have none. */
    private static Location locationOf(CommandSender sender) {
        if (sender instanceof Player) return ((Player) sender).getLocation();
        return Bukkit.getWorlds().get(0).getSpawnLocation();
    }
}
