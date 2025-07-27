package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.SpeechService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpStatusCodeTest {

    @Mock
    private SpeechService speechService;
    @Mock
    private LanguageResolver languageResolver;
    @Mock
    private Environment environment;

    @InjectMocks
    private HttpStatusCode httpStatusCode;

    @Test
    void parseWithoutArgumentsTest() {
        BotRequest request = TestUtils.getRequestFromGroup("http");
        List<BotResponse> botResponses = httpStatusCode.parse(request);
        assertTrue(botResponses.isEmpty());
    }

    @Test
    void parseWithUrlArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("http://example.com");
        List<BotResponse> botResponses = httpStatusCode.parse(request);
        assertTrue(botResponses.isEmpty());
    }

    @Test
    void parseWithWrongArgumentTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("http test");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> httpStatusCode.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithUnknownCodeAsArgumentTest() {
        final String expectedErrorText = "error";
        final String lang = "en";
        BotRequest request = TestUtils.getRequestFromGroup("http 0");
        Message message = request.getMessage();

        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(lang);
        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> httpStatusCode.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseTest() {
        final String expectedResponseText = "<b>123 caption</b>\n" +
                "description";
        final String lang = "en";
        final String code = "123";
        BotRequest request = TestUtils.getRequestFromGroup("http " + code);
        Message message = request.getMessage();

        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(lang);
        when(environment.getProperty(lang + ".http.code." + code)).thenReturn("caption");
        when(environment.getProperty(lang + ".http.status." + code)).thenReturn("description");

        BotResponse botResponse = httpStatusCode.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }

}