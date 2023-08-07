package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.commands.convertors.UnitsConverter;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

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
        Update updateFromGroup = TestUtils.getUpdateFromGroup("конверт 123");
        assertThrows(BotException.class, () -> converter.parse(updateFromGroup));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseWithUnknownUnitsTest() {
        final String expectedInfo = "info";
        Update updateFromGroup = TestUtils.getUpdateFromGroup("конверт 1,23 с в");

        UnitsConverter unitsConverter = Mockito.mock(UnitsConverter.class);
        when(unitsConverter.getInfo()).thenReturn(expectedInfo);
        Converter converter2 = new Converter(List.of(unitsConverter), bot, speechService);

        SendMessage sendMessage = converter2.parse(updateFromGroup);

        TestUtils.checkDefaultSendMessageParams(sendMessage);
        assertEquals(expectedInfo, sendMessage.getText());
    }

}