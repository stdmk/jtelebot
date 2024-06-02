package org.telegram.bot.commands;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

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
    @ValueSource(strings = {" a", " -1", " 0", " 3", " 4097", " a /", " 16 /", " 4097 !@$", " 3 !@$"})
    void parseWrongInputTest(String input) {
        final String expectedErrorText = "wrong input";
        BotRequest request = TestUtils.getRequestFromGroup("password" + input);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> password.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", " 4", " 4096", "!@$", " 16 !@$"})
    void parseTest(String input) {
        BotRequest request = TestUtils.getRequestFromGroup("password" + input);
        BotResponse botResponse = password.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertNotNull(textResponse);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

}