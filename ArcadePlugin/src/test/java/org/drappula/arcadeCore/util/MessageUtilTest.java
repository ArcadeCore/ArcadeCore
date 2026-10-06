package org.drappula.arcadeCore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    void plainMessageSendsRichMessage() {
        Player player = mock(Player.class);

        MessageUtil.sendMessage(player, "message:hello");

        verify(player, times(1)).sendRichMessage(eq("hello"), any());
        verifyNoMoreInteractions(player);
    }

    @Test
    void eachSegmentUsesItsOwnPrefix() {
        Player player = mock(Player.class);

        // The second segment must be routed as a chat message even though the
        // whole string starts with "title:". A whole-string prefix check would
        // send both segments to the title.
        MessageUtil.sendMessage(player, "title:Big;message:small");

        verify(player, times(1)).sendTitlePart(eq(TitlePart.TITLE), any());
        verify(player, times(1)).sendRichMessage(eq("small"), any());
    }

    @Test
    void actionBarAndTitleTimeAreRouted() {
        Player player = mock(Player.class);

        MessageUtil.sendMessage(player, "actionbar:Hi");
        MessageUtil.sendMessage(player, "titletime:10:70:20");

        verify(player, times(1)).sendActionBar((net.kyori.adventure.text.Component) any());
        verify(player, times(1)).sendTitlePart(eq(TitlePart.TIMES), any());
    }

    @Test
    void malformedSegmentsAreSkippedWithoutThrowing() {
        Player player = mock(Player.class);

        assertDoesNotThrow(() -> {
            MessageUtil.sendMessage(player, "titletime:10:70");
            MessageUtil.sendMessage(player, "titletime:a:b:c");
            MessageUtil.sendMessage(player, "bossbar:broken");
            MessageUtil.sendMessage(player, "bossbar:Text:not-a-number:RED:PROGRESS");
        });
        verifyNoInteractions(player);
    }

    @Test
    void bossBarShowsOnceForValidSpec() {
        Player player = mock(Player.class);

        MessageUtil.sendMessage(player, "bossbar:Fight:1:RED:PROGRESS");

        verify(player, times(1)).showBossBar(any());
    }

    @Test
    void unknownBossBarLiteralsFallBackToDefaults() {
        Player player = mock(Player.class);

        MessageUtil.sendMessage(player, "bossbar:Fight:1:NOPE:ALSO_NOPE");

        verify(player, times(1)).showBossBar(any());
    }

    @Test
    void consoleGetsChatTextWithoutPrefix() {
        CommandSender console = mock(CommandSender.class);
        org.mockito.ArgumentMatcher<Component> isHello = component ->
                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(component).equals("hello");

        MessageUtil.sendMessage(console, "message:hello", net.kyori.adventure.text.minimessage.tag.resolver.TagResolver.empty());

        verify(console, times(1)).sendMessage(argThat(isHello));
        verifyNoMoreInteractions(console);
    }

    @Test
    void consoleSkipsVisualOnlySegments() {
        CommandSender console = mock(CommandSender.class);

        MessageUtil.sendMessage(console, "title:Big;message:small", net.kyori.adventure.text.minimessage.tag.resolver.TagResolver.empty());

        verify(console, times(1)).sendMessage(any(Component.class));
        verifyNoMoreInteractions(console);
    }
}
