package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.LocationResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocationTest {

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;

    @InjectMocks
    private Location location;

    @Test
    void parseWithoutCoordinatesTest() {
        BotRequest request = TestUtils.getRequestFromGroup("location");
        List<BotResponse> botResponses = location.parse(request);
        assertTrue(botResponses.isEmpty());
    }

    @Test
    void parseWithWrongInputTest() {
        BotRequest request = TestUtils.getRequestFromGroup("location 1234");
        assertThrows(BotException.class, () -> location.parse(request));
        verify(bot).sendLocation(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseTest() {
        final Double expectedLatitude = 56.83417;
        final Double expectedLongitude = 35.90604;

        BotRequest request = TestUtils.getRequestFromGroup("location 56.83417 35,90604");

        BotResponse botResponse = location.parse(request).get(0);

        LocationResponse locationResponse = TestUtils.checkDefaultLocationResponseParams(botResponse);

        verify(bot).sendLocation(request.getMessage().getChatId());
        assertEquals(expectedLatitude, locationResponse.getLatitude());
        assertEquals(expectedLongitude, locationResponse.getLongitude());
    }

}