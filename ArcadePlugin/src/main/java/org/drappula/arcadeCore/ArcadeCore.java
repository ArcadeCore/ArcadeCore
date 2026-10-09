package org.drappula.arcadeCore;

import org.bukkit.plugin.java.JavaPlugin;
import org.drappula.arcadeApi.ArcadeAPI;
import org.drappula.arcadeApi.ArcadeAPIProvider;
import org.drappula.arcadeApi.message.Messages;
import org.drappula.arcadeCore.api.ArcadeAPIImpl;
import org.drappula.arcadeCore.commands.MainCommand;
import org.drappula.arcadeCore.config.DataConfig;
import org.drappula.arcadeCore.config.MainConfig;
import org.drappula.arcadeCore.config.MessagesConfig;
import org.drappula.arcadeCore.database.Database;
import org.drappula.arcadeCore.listeners.LobbyListener;
import org.drappula.arcadeCore.listeners.MatchListener;
import org.drappula.arcadeCore.managers.game.GameManager;
import org.drappula.arcadeCore.managers.map.MapManager;
import org.drappula.arcadeCore.managers.world.ArenaWorldManager;

import java.sql.SQLException;

public final class ArcadeCore extends JavaPlugin {
    private static ArcadeCore instance;
    public static ArcadeCore get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        Messages.useScreenText(new org.drappula.arcadeCore.util.XSeriesScreenText());
        setupConfig();
        connectDatabase();
        MapManager.get().load();
        ArenaWorldManager.get().destroyOrphanedArenas();
        registerCommands();
        registerListeners();
        getServer().getScheduler().runTaskTimer(this,
                () -> ArenaWorldManager.get().sweepIdleArenas(), 600L, 600L);
        registerAPI();
    }
    private void setupConfig() {
        DataConfig.setup();
        MainConfig.setup();
        MessagesConfig.setup();
    }
    private void connectDatabase() {
        try {
            Database.connect();
        } catch (SQLException e) {
            getLogger().severe("Failed to connect to database:");
            throw new RuntimeException(e);
        }
    }
    private void registerCommands() {
        MainCommand command = new MainCommand();
        getCommand("arcade").setExecutor(command);
        getCommand("arcade").setTabCompleter(command);
    }
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new LobbyListener(), this);
        getServer().getPluginManager().registerEvents(new MatchListener(), this);
    }
    private void registerAPI() {
        ArcadeAPI impl = new ArcadeAPIImpl();
        ArcadeAPIProvider.register(impl);
    }

    @Override
    public void onDisable() {
        GameManager.get().reload();
        ArenaWorldManager.get().destroyAll();
        MapManager.get().releaseAll();
        try {
            Database.disconnect();
        } catch (SQLException e) {
            getLogger().severe("Failed to close database connection:");
        }
    }
}
