package org.drappula.arcadeApi.systems.game;

import org.jspecify.annotations.Nullable;

import java.util.Collection;

public interface IGameManager {
    void registerGame(Game game);
    void unregisterGame(String id);
    void unregisterGame(Game game);
    @Nullable
    Game getGame(String id);
    Collection<Game> getRegisteredGames();
    boolean isRegistered(String id);
}
