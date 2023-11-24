package org.telegram.bot.commands;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordTest {

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;

    @InjectMocks
    private Password password;

    @ParameterizedTest
    @ValueSource(strings = {"a", "-1", "0", "3", "4097"})
    void parseWrongInputTest(String input) {
        final String expectedErrorText = "wrong input";
        Update update = TestUtils.getUpdateFromGroup("password " + input);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> password.parse(update));
        verify(bot).sendTyping(update);
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "4", "4096"})
    void parseTest(String input) {
        Update update = TestUtils.getUpdateFromGroup("password" + input);
        SendMessage sendMessage = password.parse(update);
        verify(bot).sendTyping(update);
        TestUtils.checkDefaultSendMessageParams(sendMessage);
    }

}