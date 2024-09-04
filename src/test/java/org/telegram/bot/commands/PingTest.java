package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.utils.NetworkUtils;

import java.net.UnknownHostException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PingTest {

    private static final LocalDateTime CURRENT_DATE_TIME = LocalDateTime.of(2007, 1, 2, 0, 0, 0);

    @Mock
    private Clock clock;
    @Mock
    private NetworkUtils networkUtils;

    @InjectMocks
    private Ping ping;

    @Test
    void parseWithArgumentUnknownHostExceptionTest() throws UnknownHostException {
        final String expectedResponseText = "${command.ping.ipnotfound}";
        final String host = "host";
        BotRequest request = TestUtils.getRequestFromGroup("ping " + host);
        when(networkUtils.pingHost(host)).thenThrow(new UnknownHostException());

        BotResponse botResponse = ping.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseWithArgumentUnreachableHostTest() throws UnknownHostException {
        final String expectedResponseText = """
                ${command.ping.host} host (127.0.0.1):
                ❌* ${command.ping.unreachable}*
                """;
        final String host = "host";
        BotRequest request = TestUtils.getRequestFromGroup("ping " + host);
        when(networkUtils.pingHost(host))
                .thenReturn(new NetworkUtils.PingResult("host", "127.0.0.1", false, 6));

        BotResponse botResponse = ping.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseWithArgumentReachableHostTest() throws UnknownHostException {
        final String expectedResponseText = """
                ${command.ping.host} 127.0.0.1:
                ✅* ${command.ping.reachable}*
                ${command.ping.caption}: *6* ${command.ping.ms}.""";
        final String host = "host";
        BotRequest request = TestUtils.getRequestFromGroup("ping " + host);
        when(networkUtils.pingHost(host))
                .thenReturn(new NetworkUtils.PingResult("127.0.0.1", "127.0.0.1", true, 6));

        BotResponse botResponse = ping.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseTest() {
        final String expectedResponseText = "${command.ping.caption}: 7000 ${command.ping.ms}.";
        BotRequest request = TestUtils.getRequestFromGroup("ping");
        request.getMessage().setDateTime(CURRENT_DATE_TIME.minusSeconds(7));

        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        BotResponse botResponse = ping.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }

}