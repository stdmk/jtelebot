package org.telegram.bot.commands;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.SpeechService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Disabled
class WordTest {

    @Mock
    private Bot bot;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private LanguageResolver languageResolver;
    @Mock
    private RestTemplate botRestTemplate;
    @Mock
    private SpeechService speechService;

    @Mock
    private ResponseEntity<Word.WiktionaryData> response;

    @InjectMocks
    private Word word;

    @Test
    void parseWithoutParamsTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        BotResponse response = word.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals("${command.word.commandwaitingstart}", textResponse.getText());
        verify(commandWaitingService).add(request.getMessage(), Word.class);
        verify(bot).sendTyping(TestUtils.DEFAULT_CHAT_ID);
    }

    @Test
    void parseWithoutResponse() {
        BotRequest request = TestUtils.getRequestFromGroup("word word");

        when(languageResolver.getChatLanguageCode(request)).thenReturn("en");
        when(botRestTemplate.getForEntity(anyString(), any())).thenThrow(new RestClientException("error"));

        assertThrows(BotException.class, () -> word.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
        verify(bot).sendTyping(TestUtils.DEFAULT_CHAT_ID);
    }

    @Test
    void parseWithEmptyResponseTest() {
        BotRequest request = TestUtils.getRequestFromGroup("word word");

        when(languageResolver.getChatLanguageCode(request)).thenReturn("en");
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Word.WiktionaryData>>any()))
                .thenReturn(response);

        assertThrows(BotException.class, () -> word.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
        verify(bot).sendTyping(TestUtils.DEFAULT_CHAT_ID);
    }

    @Test
    void parseTest() throws IOException {
        final String expectedResponseText1 = TestUtils.getResourceAsString("wiktionary/response_text1");
        final String expectedResponseText2 = TestUtils.getResourceAsString("wiktionary/response_text2");
        BotRequest request = TestUtils.getRequestFromGroup("word word");
        Word.WiktionaryData wiktionaryData = new Word.WiktionaryData()
                .setQuery(new Word.Query()
                        .setPages(Map.of(
                                "123", new Word.PageData()
                                        .setPageid(123)
                                        .setExtract(TestUtils.getResourceAsString("wiktionary/wiktionary_response")))));

        when(languageResolver.getChatLanguageCode(request)).thenReturn("en");
        when(response.getBody()).thenReturn(wiktionaryData);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Word.WiktionaryData>>any()))
                .thenReturn(response);

        List<BotResponse> methods = word.parse(request);
        assertEquals(2, methods.size());

        BotResponse response = methods.get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertThat(expectedResponseText1).isEqualToNormalizingNewlines(textResponse.getText());

        textResponse = TestUtils.checkDefaultTextResponseParams(methods.get(1));
        assertThat(expectedResponseText2).isEqualToNormalizingNewlines(textResponse.getText());

        verify(bot).sendTyping(TestUtils.DEFAULT_CHAT_ID);
    }

}