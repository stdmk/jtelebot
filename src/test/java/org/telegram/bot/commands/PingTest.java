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

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PingTest {

    private static final LocalDateTime CURRENT_DATE_TIME = LocalDateTime.of(2007, 1, 2, 0, 0, 0);

    @Mock
    private Clock clock;

    @InjectMocks
    private Ping ping;

    @Test
    void parseWithArgumentsTest() {
        BotRequest request = TestUtils.getRequestFromGroup("ping test");
        List<BotResponse> botResponses = ping.parse(request);
        assertTrue(botResponses.isEmpty());
    }

    @Test
    void parseTest() {
        final String expectedResponseText = "${command.ping.caption}: -7.0 ${command.ping.seconds}.";
        BotRequest request = TestUtils.getRequestFromGroup("ping");
        request.getMessage().setDateTime(CURRENT_DATE_TIME.plusSeconds(7));

        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        BotResponse botResponse = ping.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }

}