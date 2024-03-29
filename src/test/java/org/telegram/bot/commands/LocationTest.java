package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.objects.Update;

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
        Update update = TestUtils.getUpdateFromGroup("location");
        List<SendLocation> methods = location.parse(update);
        assertTrue(methods.isEmpty());
    }

    @Test
    void parseWithWrongInputTest() {
        Update update = TestUtils.getUpdateFromGroup("location 1234");
        assertThrows(BotException.class, () -> location.parse(update));
        verify(bot).sendLocation(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseTest() {
        final Double expectedLatitude = 56.83417;
        final Double expectedLongitude = 35.90604;

        Update update = TestUtils.getUpdateFromGroup("location 56.83417 35,90604");

        SendLocation sendLocation = location.parse(update).get(0);
        verify(bot).sendLocation(update.getMessage().getChatId());
        TestUtils.checkDefaultSendLocationParams(sendLocation);
        assertEquals(expectedLatitude, sendLocation.getLatitude());
        assertEquals(expectedLongitude, sendLocation.getLongitude());
    }

}