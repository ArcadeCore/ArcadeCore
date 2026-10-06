package org.drappula.arcadeCore.managers.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.drappula.arcadeCore.ServerTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArenaWorldManagerTest extends ServerTest {

    @Test
    void createsEmptyArenaWorld() {
        World world = ArenaWorldManager.get().create("bedrock-pillars");

        assertNotNull(world);
        assertTrue(world.getName().startsWith("arcade-bedrock-pillars-"));
        assertNotNull(server.getWorld(world.getName()));
        assertTrue(ArenaWorldManager.get().isArenaWorld(world));
    }

    @Test
    void arenaWorldIsVoid() {
        World world = ArenaWorldManager.get().create("game");

        // MockBukkit caps worlds at y=128, so probe below its ceiling.
        assertEquals(Material.AIR, world.getBlockAt(0, 100, 0).getType());
        assertEquals(Material.AIR, world.getBlockAt(-30, 60, 17).getType());
    }

    @Test
    void arenaWorldsAreUnique() {
        World first = ArenaWorldManager.get().create("game");
        World second = ArenaWorldManager.get().create("game");

        assertNotEquals(first.getName(), second.getName());
    }

    @Test
    void sweepDestroysEmptyIdleWorld() {
        World world = ArenaWorldManager.get().create("game");

        ArenaWorldManager.get().sweepIdleArenas();

        assertFalse(ArenaWorldManager.get().isArenaWorld(world));
    }

    @Test
    void sweepKeepsWorldWithLiveMatch() {
        World arena = ArenaWorldManager.get().create("game");
        org.drappula.arcadeApi.systems.game.Game game = mock(org.drappula.arcadeApi.systems.game.Game.class);
        when(game.getId()).thenReturn("game");
        org.mockbukkit.mockbukkit.entity.PlayerMock player = server.addPlayer();
        org.drappula.arcadeApi.systems.map.IArcadeMap map = mock(org.drappula.arcadeApi.systems.map.IArcadeMap.class);
        when(map.getWorld()).thenReturn(arena);
        org.drappula.arcadeCore.managers.game.Match match =
                new org.drappula.arcadeCore.managers.game.Match(game, java.util.List.of(player), map);
        org.drappula.arcadeCore.managers.game.GameManager.get().populateMatch(match);

        ArenaWorldManager.get().sweepIdleArenas();

        assertTrue(ArenaWorldManager.get().isArenaWorld(arena));
    }

    @Test
    void sweepKeepsOccupiedWorld() {
        World arena = ArenaWorldManager.get().create("game");
        org.mockbukkit.mockbukkit.entity.PlayerMock player = server.addPlayer();
        player.teleport(new org.bukkit.Location(arena, 0, 80, 0));

        ArenaWorldManager.get().sweepIdleArenas();

        assertTrue(ArenaWorldManager.get().isArenaWorld(arena));
    }

    @Test
    void destroyOrphanedArenasDeletesDeadFolders() throws Exception {
        java.io.File orphan = new java.io.File(org.bukkit.Bukkit.getWorldContainer(), "arcade-crashed-deadbeef");
        org.junit.jupiter.api.Assumptions.assumeTrue(orphan.mkdirs() || orphan.isDirectory());
        org.junit.jupiter.api.Assumptions.assumeTrue(orphan.canWrite());

        ArenaWorldManager.get().destroyOrphanedArenas();

        assertFalse(orphan.exists());
    }

    @Test
    void destroyUntracksWorld() {
        World world = ArenaWorldManager.get().create("game");

        // MockBukkit does not implement unloadWorld; destroy must still
        // deregister without throwing. The real unload is covered live.
        ArenaWorldManager.get().destroy(world);

        assertFalse(ArenaWorldManager.get().isArenaWorld(world));
    }

    @Test
    void destroyIgnoresForeignWorlds() {
        World lobby = server.addSimpleWorld("lobby");

        ArenaWorldManager.get().destroy(lobby);

        assertNotNull(server.getWorld("lobby"));
        assertFalse(ArenaWorldManager.get().isArenaWorld(lobby));
    }

    @Test
    void destroyAllUntracksEverything() {
        World first = ArenaWorldManager.get().create("game");
        World second = ArenaWorldManager.get().create("other");
        World lobby = server.addSimpleWorld("lobby");

        ArenaWorldManager.get().destroyAll();

        assertFalse(ArenaWorldManager.get().isArenaWorld(first));
        assertFalse(ArenaWorldManager.get().isArenaWorld(second));
        assertNotNull(server.getWorld("lobby"));
        assertFalse(ArenaWorldManager.get().isArenaWorld(lobby));
    }
}
