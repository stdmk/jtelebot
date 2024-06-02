package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.commands.convertors.UnitsConverter;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConverterTest {

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;

    @InjectMocks
    private Converter converter;

    @Test
    void parseWithWrongInputTest() {
        BotRequest requestFromGroup = TestUtils.getRequestFromGroup("конверт 123");
        assertThrows(BotException.class, () -> converter.parse(requestFromGroup));
        verify(bot).sendTyping(requestFromGroup.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseWithUnknownUnitsTest() {
        final String expectedInfo = "info";
        BotRequest requestFromGroup = TestUtils.getRequestFromGroup("конверт 1,23 с в");

        UnitsConverter unitsConverter = Mockito.mock(UnitsConverter.class);
        when(unitsConverter.getInfo()).thenReturn(expectedInfo);
        Converter converter2 = new Converter(List.of(unitsConverter), bot, speechService);

        BotResponse botResponse = converter2.parse(requestFromGroup).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        verify(bot).sendTyping(requestFromGroup.getMessage().getChatId());

        TestUtils.checkDefaultTextResponseParams(textResponse);
        assertEquals(expectedInfo, textResponse.getText());
    }

    @Test
    void parseWithMultipleResults() {
        final String expectedResult1 = "result1";
        final String expectedResult2 = "result2";
        BotRequest requestFromGroup = TestUtils.getRequestFromGroup("конверт 1,23 с в");

        UnitsConverter unitsConverter1 = Mockito.mock(UnitsConverter.class);
        when(unitsConverter1.getInfo()).thenReturn(expectedResult1);
        UnitsConverter unitsConverter2 = Mockito.mock(UnitsConverter.class);
        when(unitsConverter2.getInfo()).thenReturn(expectedResult2);
        Converter converter2 = new Converter(List.of(unitsConverter1, unitsConverter2), bot, speechService);

        BotResponse botResponse = converter2.parse(requestFromGroup).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        verify(bot).sendTyping(requestFromGroup.getMessage().getChatId());

        TestUtils.checkDefaultTextResponseParams(textResponse);
        assertEquals(expectedResult1 + "\n" + expectedResult2, textResponse.getText());
    }
}