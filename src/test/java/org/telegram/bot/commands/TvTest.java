package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TvTest {

    private static final int HOURS_NUMBER_SHORT = 3;
    private static final int HOURS_NUMBER_DEFAULT = 6;
    private static final int HOURS_NUMBER_LONG = 12;
    private static final LocalDateTime DATE_TIME_NOW = LocalDateTime.of(2000, 1, 1, 0, 0);

    @Mock
    private Bot bot;
    @Mock
    private TvChannelService tvChannelService;
    @Mock
    private TvProgramService tvProgramService;
    @Mock
    private UserTvService userTvService;
    @Mock
    private UserCityService userCityService;
    @Mock
    private SpeechService speechService;
    @Mock
    private Clock clock;

    @InjectMocks
    private Tv tv;

    @Test
    void parseWithoutArgumentsAndSettingsTest() {
        final String expectedErrorMessage = "error";
        BotRequest request = TestUtils.getRequestFromGroup("tv");

        when(speechService.getRandomMessageByTag(BotSpeechTag.SETTING_REQUIRED)).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> tv.parse(request));

        assertEquals(expectedErrorMessage, botException.getMessage());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithoutArgumentsTest() {
        final String expectedResponseText = """
                <u>channel1</u> /tv_ch1
                <b>[23:30]</b> title1 (100%)
                /tv_pr1
                <b>[00:30]</b> title2
                /tv_pr2
                
                <u>channel2</u> /tv_ch2
                <b>[23:30]</b> title3 (100%)
                /tv_pr3
                <b>[00:30]</b> title4
                /tv_pr4

                """;
        BotRequest request = TestUtils.getRequestFromGroup("tv");
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        TvChannel channel1 = getTvChannel(1);
        TvChannel channel2 = getTvChannel(2);
        List<UserTv> userTvList = List.of(
                new UserTv().setId(1L).setChat(chat).setUser(user).setTvChannel(channel1),
                new UserTv().setId(2L).setChat(chat).setUser(user).setTvChannel(channel2));

        List<TvProgram> channel1Programs = List.of(
                getTvProgram(channel1, DATE_TIME_NOW.minusHours(1), 1),
                getTvProgram(channel1, DATE_TIME_NOW, 2));
        List<TvProgram> channel2Programs = List.of(
                getTvProgram(channel2, DATE_TIME_NOW.minusHours(1), 3),
                getTvProgram(channel2, DATE_TIME_NOW, 4));
        when(clock.instant()).thenReturn(DATE_TIME_NOW.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(userCityService.getZoneIdOfUserOrDefault(message)).thenReturn(ZoneId.systemDefault());
        when(userTvService.get(chat, user)).thenReturn(userTvList);
        when(tvProgramService.get(channel1, DATE_TIME_NOW, HOURS_NUMBER_SHORT)).thenReturn(channel1Programs);
        when(tvProgramService.get(channel2, DATE_TIME_NOW, HOURS_NUMBER_SHORT)).thenReturn(channel2Programs);

        BotResponse botResponse = tv.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithCorruptedChannelCommandAsArgumentTest() {
        final String expectedErrorMessage = "error";
        BotRequest request = TestUtils.getRequestFromGroup("tv_cha");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> tv.parse(request));

        assertEquals(expectedErrorMessage, botException.getMessage());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithUnknownChannelCommandAsArgumentTest() {
        final String expectedErrorMessage = "error";
        BotRequest request = TestUtils.getRequestFromGroup("tv_ch1");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> tv.parse(request));

        assertEquals(expectedErrorMessage, botException.getMessage());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithChannelCommandAsArgumentProgramsNotFoundTest() {
        final String expectedErrorMessage = "error";
        final int channelId = 1;
        BotRequest request = TestUtils.getRequestFromGroup("tv_ch" + channelId);

        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn(expectedErrorMessage);
        when(tvChannelService.get(channelId)).thenReturn(getTvChannel(channelId));
        when(userCityService.getZoneIdOfUserOrDefault(request.getMessage())).thenReturn(ZoneId.systemDefault());
        when(clock.instant()).thenReturn(DATE_TIME_NOW.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        BotException botException = assertThrows(BotException.class, () -> tv.parse(request));

        assertEquals(expectedErrorMessage, botException.getMessage());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithChannelCommandAsArgumentTest() {
        final String expectedResponseText = """
                <u>channel1</u> /tv_ch1
                <b>[23:30]</b> title1 (100%)
                /tv_pr1
                <b>[00:30]</b> title2
                /tv_pr2

                """;
        final int channelId = 1;
        BotRequest request = TestUtils.getRequestFromGroup("tv_ch" + channelId);
        Message message = request.getMessage();

        TvChannel channel1 = getTvChannel(1);
        List<TvProgram> channel1Programs = List.of(
                getTvProgram(channel1, DATE_TIME_NOW.minusHours(1), 1),
                getTvProgram(channel1, DATE_TIME_NOW, 2));

        when(tvChannelService.get(channelId)).thenReturn(channel1);
        when(clock.instant()).thenReturn(DATE_TIME_NOW.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(userCityService.getZoneIdOfUserOrDefault(message)).thenReturn(ZoneId.systemDefault());
        when(tvProgramService.get(channel1, DATE_TIME_NOW, HOURS_NUMBER_LONG)).thenReturn(channel1Programs);

        BotResponse botResponse = tv.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithCorruptedProgramCommandAsArgumentTest() {
        final String expectedErrorMessage = "error";
        BotRequest request = TestUtils.getRequestFromGroup("tv_pra");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> tv.parse(request));

        assertEquals(expectedErrorMessage, botException.getMessage());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithUnknownProgramCommandAsArgumentTest() {
        final String expectedErrorMessage = "error";
        BotRequest request = TestUtils.getRequestFromGroup("tv_pr1");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> tv.parse(request));

        assertEquals(expectedErrorMessage, botException.getMessage());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithProgramCommandAsArgumentTest() {
        final String expectedResponseText = """
                <u>channel1</u> /tv_ch1
                <b>title1</b>\s
                <i>category1</i>
                ${command.tv.start}: 00:30
                ${command.tv.stop}: 01:00
                (30 ${utils.date.m}. )

                <i>desc1</i>""";
        final int tvProgramId = 1;
        BotRequest request = TestUtils.getRequestFromGroup("tv_pr" + tvProgramId);

        TvChannel channel = getTvChannel(1);
        TvProgram tvProgram = getTvProgram(channel, DATE_TIME_NOW, 1);
        when(tvProgramService.get(tvProgramId)).thenReturn(tvProgram);
        when(clock.instant()).thenReturn(DATE_TIME_NOW.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(userCityService.getZoneIdOfUserOrDefault(request.getMessage())).thenReturn(ZoneId.systemDefault());

        BotResponse botResponse = tv.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithSearchOneChannelTest() {
        final String expectedResponseText = """
                <u>channel1</u> /tv_ch1
                <b>[23:30]</b> title1 (100%)
                /tv_pr1
                <b>[00:30]</b> title2
                /tv_pr2

                """;
        final String channelName = "channel";
        BotRequest request = TestUtils.getRequestFromGroup("tv " + channelName);
        Message message = request.getMessage();

        TvChannel channel1 = getTvChannel(1);
        List<TvProgram> channel1Programs = List.of(
                getTvProgram(channel1, DATE_TIME_NOW.minusHours(1), 1),
                getTvProgram(channel1, DATE_TIME_NOW, 2));

        when(tvChannelService.get(channelName)).thenReturn(List.of(channel1));
        when(clock.instant()).thenReturn(DATE_TIME_NOW.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(userCityService.getZoneIdOfUserOrDefault(message)).thenReturn(ZoneId.systemDefault());
        when(tvProgramService.get(channel1, DATE_TIME_NOW, HOURS_NUMBER_LONG)).thenReturn(channel1Programs);

        BotResponse botResponse = tv.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithSearchChannelsTest() {
        final String expectedResponseText = """
                <u>channel1</u> /tv_ch1
                <b>[23:30]</b> title1 (100%)
                /tv_pr1
                <b>[00:30]</b> title2
                /tv_pr2

                """;
        final String channelName = "channel1";
        BotRequest request = TestUtils.getRequestFromGroup("tv " + channelName);
        Message message = request.getMessage();

        TvChannel channel1 = getTvChannel(1);
        TvChannel channel2 = getTvChannel(2);
        List<TvProgram> channel1Programs = List.of(
                getTvProgram(channel1, DATE_TIME_NOW.minusHours(1), 1),
                getTvProgram(channel1, DATE_TIME_NOW, 2));

        when(tvChannelService.get(channelName)).thenReturn(List.of(channel1, channel2));
        when(clock.instant()).thenReturn(DATE_TIME_NOW.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(userCityService.getZoneIdOfUserOrDefault(message)).thenReturn(ZoneId.systemDefault());
        when(tvProgramService.get(channel1, DATE_TIME_NOW, HOURS_NUMBER_LONG)).thenReturn(channel1Programs);

        BotResponse botResponse = tv.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithSearchNotFoundTest() {
        final String expectedErrorMessage = "error";
        BotRequest request = TestUtils.getRequestFromGroup("tv test");

        when(clock.instant()).thenReturn(DATE_TIME_NOW.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn(expectedErrorMessage);
        when(userCityService.getZoneIdOfUserOrDefault(request.getMessage())).thenReturn(ZoneId.systemDefault());

        BotException botException = assertThrows(BotException.class, () -> tv.parse(request));

        assertEquals(expectedErrorMessage, botException.getMessage());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithSearchChannelsAndProgramsTest() {
        final String searchText = "test";
        final String expectedResponseText = """
                <u>${command.tv.foundchannels}:</u>
                channel1 - /tv_ch1
                channel2 - /tv_ch2

                <u>${command.tv.foundprograms}:</u>
                title1 (100%)
                (<b>channel1</b>)
                31.12.1999 23:30
                /tv_pr1

                title2\s
                (<b>channel2</b>)
                01.01.2000 00:30
                /tv_pr2

                """;
        BotRequest request = TestUtils.getRequestFromGroup("tv " + searchText);
        Message message = request.getMessage();

        TvChannel channel1 = getTvChannel(1);
        TvChannel channel2 = getTvChannel(2);
        List<TvProgram> programList = List.of(
                getTvProgram(channel1, DATE_TIME_NOW.minusHours(1), 1),
                getTvProgram(channel2, DATE_TIME_NOW, 2));

        when(tvChannelService.get(searchText)).thenReturn(List.of(channel1, channel2));
        when(clock.instant()).thenReturn(DATE_TIME_NOW.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(userCityService.getZoneIdOfUserOrDefault(message)).thenReturn(ZoneId.systemDefault());
        when(tvProgramService.get(searchText, DATE_TIME_NOW, HOURS_NUMBER_DEFAULT)).thenReturn(programList);

        BotResponse botResponse = tv.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(message.getChatId());
    }

    private TvChannel getTvChannel(int number) {
        return new TvChannel().setId(number).setName("channel" + number);
    }

    private TvProgram getTvProgram(TvChannel channel, LocalDateTime dateTime, int number) {
        return new TvProgram()
                .setId(number)
                .setChannel(channel)
                .setTitle("title" + number)
                .setCategory("category" + number)
                .setDesc("desc" + number)
                .setStart(dateTime.plusMinutes(30))
                .setStop(dateTime.plusMinutes(60));
    }

}