package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.providers.system.SystemInfoProvider;
import org.telegram.bot.repositories.TalkerPhraseRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UptimeTest {

    private static final LocalDateTime DATE_TIME_NOW = LocalDateTime.of(2000, 1, 1, 0, 0);

    @Mock
    private Bot bot;
    @Mock
    private BotStats botStats;
    @Mock
    private TalkerPhraseRepository talkerPhraseRepository;
    @Mock
    private SystemInfoProvider systemInfoProvider;
    @Mock
    private Clock clock;

    @InjectMocks
    private Uptime uptime;

    @Test
    void parseWithArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("uptime test");
        List<BotResponse> botResponses = uptime.parse(request);
        assertTrue(botResponses.isEmpty());
        verify(bot, never()).sendTyping(anyLong());
    }

    @Test
    void parseTest() {
        final String expectedResponseText = """
                <b>${command.uptime.launch}:</b>
                01.12.1999 00:00:00
                <b>${command.uptime.uptime}:</b>
                31 ${utils.date.d}.\s
                <b>${command.uptime.totaltime}:</b>
                4 ${utils.date.d}. 5 ${utils.date.h}. 23 ${utils.date.m}. 20 ${utils.date.s}.\s
                <b>Heap:</b>
                47/1024/2048 мб.
                <b><u>${command.uptime.statistic}:</u></b>
                ${command.uptime.incomingmessages}: <b>100</b> (1000)
                ${command.uptime.talkerphrases}: <b>107</b> (0)
                ${command.uptime.commandsprocessed}: <b>101</b> (1K)
                ${command.uptime.googlerequests}: <b>102</b>
                ${command.uptime.postrequests}: <b>103</b>
                ${command.uptime.wolframrequests}: <b>104</b>
                ${command.uptime.movierequests}: <b>105</b>
                ${command.uptime.unexpectederrors}: <b>106</b>
                ${command.uptime.tvupdate}: <b>31.12 23:30</b>
                ${command.uptime.trackcodeupdate}: <b>31.12 23:20</b>
                ${command.uptime.dbsize}: <b>30.00 Mb </b>
                ${command.uptime.freeondisk}: <b>5.00 Gb </b>
                """;
        BotRequest request = TestUtils.getRequestFromGroup("uptime");
        Message message = request.getMessage();

        when(clock.instant()).thenReturn(DATE_TIME_NOW.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(systemInfoProvider.getTotalMemory()).thenReturn(1073741824L);
        when(systemInfoProvider.getMaxMemory()).thenReturn(2147483648L);
        when(systemInfoProvider.getFreeMemory()).thenReturn(1024000000L);
        when(systemInfoProvider.getDbFileSize()).thenReturn(31457280L);
        when(systemInfoProvider.getFreeSystemSpace()).thenReturn(5368709120L);
        when(botStats.getBotStartDateTime()).thenReturn(DATE_TIME_NOW.minusMonths(1));
        when(botStats.getTotalRunningTime()).thenReturn(365000000L);
        when(botStats.getReceivedMessages()).thenReturn(100);
        when(botStats.getTotalReceivedMessages()).thenReturn(1000L);
        when(botStats.getCommandsProcessed()).thenReturn(101);
        when(botStats.getTotalCommandsProcessed()).thenReturn(1001L);
        when(botStats.getGoogleRequests()).thenReturn(102);
        when(botStats.getRussianPostRequests()).thenReturn(103);
        when(botStats.getWolframRequests()).thenReturn(104);
        when(botStats.getKinopoiskRequests()).thenReturn(105);
        when(botStats.getErrors()).thenReturn(106);
        when(botStats.getLastTvUpdate()).thenReturn(DATE_TIME_NOW.minusMinutes(30).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000);
        when(botStats.getLastTracksUpdate()).thenReturn(DATE_TIME_NOW.minusMinutes(40).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000);
        when(talkerPhraseRepository.countByChat(message.getChat())).thenReturn(107L);

        BotResponse botResponse = uptime.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(message.getChatId());
    }

}