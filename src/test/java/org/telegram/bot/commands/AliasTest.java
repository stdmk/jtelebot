package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.AliasService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.ObjectCopier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.telegram.bot.TestUtils.getRequestFromGroup;

@ExtendWith(MockitoExtension.class)
class AliasTest {

    @Mock
    private Bot bot;
    @Mock
    private ObjectCopier objectCopier;
    @Mock
    private AliasService aliasService;
    @Mock
    private SpeechService speechService;

    @InjectMocks
    private Alias alias;

    @Test
    void parseWithUnknownAliasNameTest() {
        BotRequest request = getRequestFromGroup("alias test");

        when(aliasService.get(any(org.telegram.bot.domain.entities.Chat.class), anyString()))
                .thenReturn(List.of());

        assertThrows((BotException.class), () -> alias.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
    }

    @Test
    void parseWithAliasNameTest() {
        final String expectedResponseText = "${command.alias.foundaliases}:\ntest1 — `echo1`\ntest2 — `echo2`";
        List<org.telegram.bot.domain.entities.Alias> aliasEntityList = getSomeAliasEntityList();
        BotRequest request = getRequestFromGroup("alias test");

        when(aliasService.get(any(org.telegram.bot.domain.entities.Chat.class), anyString())).thenReturn(aliasEntityList);

        BotResponse botResponse = alias.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse, true, FormattingStyle.MARKDOWN);

        String actualResponseText = textResponse.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void parseTest() {
        final String expectedResponseText = "*${command.alias.aliaslist}:*\ntest1 — `echo1`\ntest1 — `echo1`\ntest2 — `echo2`";
        List<org.telegram.bot.domain.entities.Alias> aliasEntityList = getSomeAliasEntityList();

        when(aliasService.getByChatAndUser(any(org.telegram.bot.domain.entities.Chat.class), any(org.telegram.bot.domain.entities.User.class)))
                .thenReturn(aliasEntityList);

        BotResponse botResponse = alias.parse(TestUtils.getRequestFromGroup()).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse, true, FormattingStyle.MARKDOWN);

        String actualResponseText = textResponse.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void analyzeWithoutTextTest() {
        BotRequest request = TestUtils.getRequestFromGroup("");

        List<BotResponse> botResponseList = alias.analyze(request);

        assertTrue(botResponseList.isEmpty());
        verify(bot, never()).processRequestWithoutAnalyze(any(BotRequest.class));
    }

    @Test
    void analyzeNotAliasTest() {
        BotRequest request = TestUtils.getRequestFromGroup("test");

        List<BotResponse> botResponseList = alias.analyze(request);

        assertTrue(botResponseList.isEmpty());
        verify(bot, never()).processRequestWithoutAnalyze(any(BotRequest.class));
    }

    @Test
    void analyzeCopyErrorTest() {
        BotRequest request = TestUtils.getRequestFromGroup("test");
        org.telegram.bot.domain.entities.Alias aliasEntity = getSomeAliasEntityList().get(0);

        when(aliasService.get(
                any(org.telegram.bot.domain.entities.Chat.class),
                any(org.telegram.bot.domain.entities.User.class),
                anyString()))
                .thenReturn(aliasEntity);
        when(objectCopier.copyObject(request, BotRequest.class)).thenReturn(null);

        List<BotResponse> botResponseList = alias.analyze(request);

        assertTrue(botResponseList.isEmpty());
        verify(bot, never()).processRequestWithoutAnalyze(any(BotRequest.class));
    }

    @Test
    void analyzeTest() {
        BotRequest request = TestUtils.getRequestFromGroup("test");
        org.telegram.bot.domain.entities.Alias aliasEntity = getSomeAliasEntityList().get(0);

        when(aliasService.get(
                        any(org.telegram.bot.domain.entities.Chat.class),
                        any(org.telegram.bot.domain.entities.User.class),
                        anyString()))
                .thenReturn(aliasEntity);
        when(objectCopier.copyObject(request, BotRequest.class)).thenReturn(request);

        List<BotResponse> botResponseList = alias.analyze(request);

        assertTrue(botResponseList.isEmpty());
        verify(bot, times(1)).processRequestWithoutAnalyze(any(BotRequest.class));
    }

    @Test
    void analyzeMultipleCommandsAliasTest() {
        BotRequest request = TestUtils.getRequestFromGroup("test");
        List<String> commands = List.of("say test1", "say test2", "say test3");
        org.telegram.bot.domain.entities.Alias aliasEntity = new org.telegram.bot.domain.entities.Alias()
                .setId(1L)
                .setChat(new Chat().setChatId(-1L))
                .setUser(new User().setUserId(1L))
                .setName("test")
                .setValue("{" + String.join(";", commands) + "}");

        when(aliasService.get(
                any(org.telegram.bot.domain.entities.Chat.class),
                any(org.telegram.bot.domain.entities.User.class),
                anyString()))
                .thenReturn(aliasEntity);
        when(objectCopier.copyObject(request, BotRequest.class)).thenReturn(request);

        List<BotResponse> botResponseList = alias.analyze(request);

        assertTrue(botResponseList.isEmpty());
        verify(bot, times(3)).processRequestWithoutAnalyze(any(BotRequest.class));
    }

    @Test
    void analyzeMultipleCommandsAliasWithArgumentTest() {
        BotRequest request = getRequestFromGroup("test hello");
        org.telegram.bot.domain.entities.Alias aliasEntity = new org.telegram.bot.domain.entities.Alias()
                .setId(1L)
                .setChat(new Chat().setChatId(-1L))
                .setUser(new User().setUserId(1L))
                .setName("test")
                .setValue("{say;say;say}");

        when(aliasService.get(
                any(org.telegram.bot.domain.entities.Chat.class),
                any(org.telegram.bot.domain.entities.User.class),
                anyString()))
                .thenReturn(aliasEntity);
        when(objectCopier.copyObject(request, BotRequest.class)).thenReturn(request);

        List<BotResponse> botResponseList = alias.analyze(request);

        assertTrue(botResponseList.isEmpty());
        verify(bot, times(3)).processRequestWithoutAnalyze(any(BotRequest.class));
    }

    private List<org.telegram.bot.domain.entities.Alias> getSomeAliasEntityList() {
        return List.of(
                new org.telegram.bot.domain.entities.Alias()
                    .setId(1L)
                    .setChat(new Chat().setChatId(-1L))
                    .setUser(new User().setUserId(1L))
                    .setName("test1")
                    .setValue("echo1"),
                new org.telegram.bot.domain.entities.Alias()
                        .setId(2L)
                        .setChat(new Chat().setChatId(-1L))
                        .setUser(new User().setUserId(2L))
                        .setName("test1")
                        .setValue("echo1"),
                new org.telegram.bot.domain.entities.Alias()
                        .setId(3L)
                        .setChat(new Chat().setChatId(-1L))
                        .setUser(new User().setUserId(1L))
                        .setName("test2")
                        .setValue("echo2"));
    }

}