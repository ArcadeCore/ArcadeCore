package org.drappula.arcadeApi.message;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessagesTest {
    private ScreenText screen;

    @BeforeEach
    void setUp() {
        screen = mock(ScreenText.class);
        Messages.useScreenText(screen);
    }

    @AfterEach
    void tearDown() {
        Messages.useScreenText(null);
    }

    @Test
    void nullAndEmptyAreIgnored() {
        Player player = mock(Player.class);
        assertDoesNotThrow(() -> {
            Messages.send(player, null);
            Messages.send(player, "");
        });
        verifyNoInteractions(player, screen);
    }

    @Test
    void chatSegmentIsSentWithoutPrefix() {
        Player player = mock(Player.class);
        Messages.send(player, "message:hello");
        verify(player, times(1)).sendMessage("hello");
        verifyNoMoreInteractions(player);
    }

    @Test
    void placeholdersAreSubstitutedLiterally() {
        Player player = mock(Player.class);
        Messages.send(player, "message:<red>Hi <name>", "name", "<b>Bob");
        verify(player, times(1)).sendMessage("§cHi <b>Bob");
    }

    @Test
    void unprefixedTextIsDroppedForPlayers() {
        Player player = mock(Player.class);
        Messages.send(player, "no prefix here");
        verifyNoInteractions(player, screen);
    }

    @Test
    void eachSegmentUsesItsOwnPrefix() {
        Player player = mock(Player.class);
        Messages.send(player, "title:Big;message:small");
        verify(screen, times(1)).title(eq(player), eq("Big"), isNull(), anyInt(), anyInt(), anyInt());
        verify(player, times(1)).sendMessage("small");
    }

    @Test
    void titleSubtitleAndTimesAreSentTogetherOnce() {
        Player player = mock(Player.class);
        Messages.send(player, "title:A;subtitle:B;titletime:500:1000:200");
        // milliseconds -> ticks (50 ms per tick)
        verify(screen, times(1)).title(player, "A", "B", 10, 20, 4);
    }

    @Test
    void blankTitleTimesFallBackToDefaults() {
        Player player = mock(Player.class);
        Messages.send(player, "title:A;titletime:::");
        verify(screen, times(1)).title(player, "A", null, 10, 70, 20);
    }

    @Test
    void actionBarIsRouted() {
        Player player = mock(Player.class);
        Messages.send(player, "actionbar:<gray>Hi");
        verify(screen, times(1)).actionBar(player, "§7Hi");
    }

    @Test
    void escapedSemicolonStaysInTheSegment() {
        Player player = mock(Player.class);
        Messages.send(player, "message:a\\;b");
        verify(player, times(1)).sendMessage("a;b");
    }

    @Test
    void malformedSegmentsAreSkippedWithoutThrowing() {
        Player player = mock(Player.class);
        assertDoesNotThrow(() -> {
            Messages.send(player, "titletime:10:70");
            Messages.send(player, "titletime:a:b:c");
            Messages.send(player, "bossbar:broken");
            Messages.send(player, "bossbar:Text:not-a-number:RED:PROGRESS");
        });
        verify(screen, never()).title(any(), anyString(), any(), anyInt(), anyInt(), anyInt());
        verifyNoInteractions(player);
    }

    @Test
    void bossBarFallsBackToActionBarWhenServerHasNoBossBarApi() {
        // The test classpath is Spigot 1.8.8, which has no org.bukkit.boss.
        Player player = mock(Player.class);
        Messages.send(player, "bossbar:Fight:1:RED:PROGRESS");
        verify(screen, times(1)).actionBar(player, "Fight");
    }

    @Test
    void consoleGetsChatAndActionBarAsText() {
        CommandSender console = mock(CommandSender.class);
        Messages.send(console, "message:hello;actionbar:there");
        verify(console, times(1)).sendMessage("hello");
        verify(console, times(1)).sendMessage("there");
        verifyNoMoreInteractions(console);
        verifyNoInteractions(screen);
    }

    @Test
    void consoleSkipsVisualOnlySegments() {
        CommandSender console = mock(CommandSender.class);
        Messages.send(console, "title:Big;subtitle:x;bossbar:a:1:RED:PROGRESS;message:small");
        verify(console, times(1)).sendMessage("small");
        verifyNoMoreInteractions(console);
    }

    @Test
    void consoleGetsUnprefixedText() {
        CommandSender console = mock(CommandSender.class);
        Messages.send(console, "<red>plain");
        verify(console, times(1)).sendMessage("§cplain");
    }

    @Test
    void newlinesInChatBecomeLineBreaks() {
        Player player = mock(Player.class);
        Messages.send(player, "message:a\nb");
        verify(player, times(1)).sendMessage("a\nb");
    }
}
