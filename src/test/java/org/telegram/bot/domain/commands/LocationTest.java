package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocationTest {

    @Mock
    private SpeechService speechService;

    @InjectMocks
    private Location location;

    @Test
    void parseWithoutCoordinatesTest() {
        Update update = TestUtils.getUpdateFromGroup("location");
        SendLocation sendLocation = location.parse(update);
        assertNull(sendLocation);
    }

    @Test
    void parseWithWrongInputTest() {
        Update update = TestUtils.getUpdateFromGroup("location 1234");
        assertThrows(BotException.class, () -> location.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseTest() {
        final Double expectedLatitude = 56.83417;
        final Double expectedLongitude = 35.90604;

        Update update = TestUtils.getUpdateFromGroup("location 56.83417 35,90604");

        SendLocation sendLocation = location.parse(update);
        TestUtils.checkDefaultSendLocationParams(sendLocation);
        assertEquals(expectedLatitude, sendLocation.getLatitude());
        assertEquals(expectedLongitude, sendLocation.getLongitude());
    }

}