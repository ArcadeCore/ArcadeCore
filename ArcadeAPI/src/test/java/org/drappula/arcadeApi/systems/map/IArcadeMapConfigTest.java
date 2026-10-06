package org.drappula.arcadeApi.systems.map;

import org.drappula.arcadeApi.ArcadeAPI;
import org.drappula.arcadeApi.ArcadeAPIProvider;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.game.IGameManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IArcadeMapConfigTest {

    private IArcadeMap map(Map<String, String> overrides) {
        return new IArcadeMap() {
            @Override
            public String getId() {
                return "map";
            }

            @Override
            public String getGameId() {
                return "game";
            }

            @Override
            public String getDisplayName() {
                return "Map";
            }

            @Override
            public String getWorldName() {
                return "world";
            }

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public boolean isInUse() {
                return false;
            }

            @Override
            public List<org.bukkit.Location> getSpawnPoints() {
                return List.of();
            }

            @Override
            public org.bukkit.World getWorld() {
                return null;
            }

            @Override
            public Map<String, String> getConfigOverrides() {
                return overrides;
            }
        };
    }

    @BeforeEach
    void registerApi() {
        ArcadeAPI api = mock(ArcadeAPI.class);
        IGameManager gameManager = mock(IGameManager.class);
        Game game = mock(Game.class);
        when(api.getGameManager()).thenReturn(gameManager);
        when(gameManager.getGame("game")).thenReturn(game);
        when(game.getMapConfigOptions()).thenReturn(List.of(
                MapConfigOption.integer("speed", 5, 0, 60),
                MapConfigOption.booleanOption("sudden-death", false),
                MapConfigOption.text("motd", "hello")));
        ArcadeAPIProvider.register(api);
    }

    @AfterEach
    void unregisterApi() {
        ArcadeAPIProvider.unregister();
    }

    @Test
    void overridesDefaultToEmpty() {
        IArcadeMap bare = new IArcadeMap() {
            @Override
            public String getId() {
                return "bare";
            }

            @Override
            public String getGameId() {
                return "game";
            }

            @Override
            public String getDisplayName() {
                return "Bare";
            }

            @Override
            public String getWorldName() {
                return "world";
            }

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public boolean isInUse() {
                return false;
            }

            @Override
            public List<org.bukkit.Location> getSpawnPoints() {
                return List.of();
            }

            @Override
            public org.bukkit.World getWorld() {
                return null;
            }
        };

        assertTrue(bare.getConfigOverrides().isEmpty());
    }

    @Test
    void overrideBeatsRegisteredDefault() {
        assertEquals("42", map(Map.of("speed", "42")).getConfigValue("speed"));
    }

    @Test
    void missingOverrideFallsBackToDefault() {
        IArcadeMap arcadeMap = map(Map.of());

        assertEquals("5", arcadeMap.getConfigValue("speed"));
        assertEquals("false", arcadeMap.getConfigValue("sudden-death"));
        assertEquals("hello", arcadeMap.getConfigValue("motd"));
    }

    @Test
    void unknownKeyThrows() {
        assertThrows(IllegalArgumentException.class, () -> map(Map.of()).getConfigValue("nope"));
    }

    @Test
    void typedGettersParseEffectiveValues() {
        IArcadeMap arcadeMap = map(Map.of("speed", "42"));

        assertEquals(42, arcadeMap.getIntConfig("speed"));
        assertEquals(5, map(Map.of()).getIntConfig("speed"));
        assertFalse(arcadeMap.getBooleanConfig("sudden-death"));
        assertEquals("hello", arcadeMap.getTextConfig("motd"));
    }

    @Test
    void invalidStoredOverrideFallsBackToDefault() {
        assertEquals("5", map(Map.of("speed", "fast")).getConfigValue("speed"));
    }
}
