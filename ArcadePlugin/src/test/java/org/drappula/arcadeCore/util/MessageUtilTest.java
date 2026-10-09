package org.drappula.arcadeCore.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

/** Routing itself is tested in ArcadeAPI's MessagesTest; this only checks the thin wrapper. */
class MessageUtilTest {

    @Test
    void nullAndEmptyAreIgnored() {
        Player player = mock(Player.class);

        assertDoesNotThrow(() -> {
            MessageUtil.sendMessage(player, null);
            MessageUtil.sendMessage(player, "");
        });
        verifyNoInteractions(player);
    }

    @Test
    void messageSegmentIsDelivered() {
        Player player = mock(Player.class);

        MessageUtil.sendMessage(player, "message:hello <name>", "name", "Bob");

        verify(player, times(1)).sendMessage("hello Bob");
        verifyNoMoreInteractions(player);
    }

    @Test
    void consoleGetsChatTextWithoutPrefix() {
        CommandSender console = mock(CommandSender.class);

        MessageUtil.sendMessage(console, "title:Big;message:small");

        verify(console, times(1)).sendMessage("small");
        verifyNoMoreInteractions(console);
    }
}
