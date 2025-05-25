package org.telegram.bot.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.SpeechService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SteamTest {

    private static final String APP_ID = "123";
    private static final String IMAGE_URL = "imageUrl";

    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private LanguageResolver languageResolver;
    @Mock
    private RestTemplate botRestTemplate;
    @Mock
    private SpeechService speechService;
    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<HttpEntity<Void>> entityCaptor;
    @Mock
    private ResponseEntity<Steam.SearchResult> searchResponse;
    @Mock
    private ResponseEntity<Map<String, Object>> detailsResponse;

    @InjectMocks
    private Steam steam;

    @Test
    void parseWithoutArgumentsTest() {
        final String expectedResponseText = "${command.steam.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestFromGroup("steam");
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());

        BotResponse response = steam.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(commandWaitingService).add(message, Steam.class);
    }

    @Test
    void parseWithHttpErrorExceptionTest() {
        final String expectedErrorText = "error";
        String commandArgument = "test";
        String lang = "en";
        final String expectedApiUrl = "https://store.steampowered.com/api/storesearch?cc=" + lang + "&term=" + commandArgument;
        BotRequest request = TestUtils.getRequestFromGroup("steam " + commandArgument);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(lang);
        when(botRestTemplate.exchange(eq(expectedApiUrl), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.CONFLICT));
        when(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> steam.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());

        verify(botRestTemplate).exchange(eq(expectedApiUrl), eq(HttpMethod.GET), entityCaptor.capture(), any(ParameterizedTypeReference.class));
        HttpEntity<Void> httpEntity = entityCaptor.getValue();
        List<String> langHeader = httpEntity.getHeaders().get("Accept-Language");
        assertNotNull(langHeader);
        assertEquals(1, langHeader.size());
        assertEquals(lang, langHeader.get(0));
    }

    @ParameterizedTest
    @MethodSource("provideHasNoResultSearchResult")
    void parseWithNoResponseSearchTest(Steam.SearchResult searchResult) {
        final String expectedErrorText = "error";
        String commandArgument = "test";
        String lang = "en";
        final String expectedApiUrl = "https://store.steampowered.com/api/storesearch?cc=" + lang + "&term=" + commandArgument;
        BotRequest request = TestUtils.getRequestFromGroup("steam test");
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(lang);
        when(searchResponse.getBody()).thenReturn(searchResult);
        when(botRestTemplate.exchange(eq(expectedApiUrl), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(searchResponse);
        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> steam.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    private static Stream<Steam.SearchResult> provideHasNoResultSearchResult() {
        return Stream.of(
            null,
                new Steam.SearchResult(),
                new Steam.SearchResult().setTotal(0)
        );
    }

    @Test
    void parseSearchTest() {
        final String expectedMessageText = """
                <b>game1</b>
                /steam_1
                
                <b>game2</b>
                /steam_2
                
                <b>game3</b>
                /steam_3""";
        String commandArgument = "test";
        String lang = "en";
        final String expectedApiUrl = "https://store.steampowered.com/api/storesearch?cc=" + lang + "&term=" + commandArgument;
        BotRequest request = TestUtils.getRequestFromGroup("steam test");
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(lang);
        when(searchResponse.getBody()).thenReturn(getSomeSearchResult());
        when(botRestTemplate.exchange(eq(expectedApiUrl), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(searchResponse);

        BotResponse response = steam.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedMessageText, textResponse.getText());
    }

    private Steam.SearchResult getSomeSearchResult() {
        return new Steam.SearchResult()
                .setTotal(3)
                .setItems(List.of(
                        new Steam.Item().setId(1L).setName("game1"),
                        new Steam.Item().setId(2L).setName("game2"),
                        new Steam.Item().setId(3L).setName("game3")));
    }

    @ParameterizedTest
    @MethodSource("provideHasNoResultDetailResult")
    void parseWithNoResponseDetailsTest(Map<String, Object> steamResponse) {
        final String expectedErrorText = "error";
        String lang = "en";
        final String expectedApiUrl = "https://store.steampowered.com/api/appdetails?cc=" + lang + "&l=" + lang + "&appids=" + APP_ID;
        BotRequest request = TestUtils.getRequestFromGroup("steam " + APP_ID);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(lang);
        when(detailsResponse.getBody()).thenReturn(steamResponse);
        when(botRestTemplate.exchange(eq(expectedApiUrl), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(detailsResponse);
        when(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE)).thenReturn(expectedErrorText);
        Steam.DetailResult detailResult = (Steam.DetailResult) steamResponse.get(APP_ID);
        when(objectMapper.convertValue(detailResult, Steam.DetailResult.class)).thenReturn(detailResult);

        BotException botException = assertThrows(BotException.class, () -> steam.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    private static Stream<Map<String, Object>> provideHasNoResultDetailResult() {
        Map<String, Object> nullableResponse = new HashMap<>(1);
        nullableResponse.put(APP_ID, null);

        return Stream.of(
                nullableResponse,
                Map.of(APP_ID, new Steam.DetailResult()),
                Map.of(APP_ID, new Steam.DetailResult().setSuccess(true))
        );
    }

    @Test
    void parseDetailsTest() {
        final String expectedMessageText = """
                <b><u>name</u></b>
                ID: <code>123</code>
                <b>18+</b>
                ${command.steam.releasedate}: <b>1 january 2000</b>
                ${command.steam.developers}: <b>developer1,developer2</b>
                ${command.steam.price}: <b>$256</b>
                description
                <a href="https://store.steampowered.com/app/123">${command.steam.store}</a>""";
        String lang = "en";
        final String expectedApiUrl = "https://store.steampowered.com/api/appdetails?cc=" + lang + "&l=" + lang + "&appids=" + APP_ID;
        BotRequest request = TestUtils.getRequestFromGroup("steam " + APP_ID);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(lang);
        Map<String, Object> steamResponse = getSomeDetailsResponse();
        when(detailsResponse.getBody()).thenReturn(steamResponse);
        when(botRestTemplate.exchange(eq(expectedApiUrl), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(detailsResponse);
        Steam.DetailResult detailResult = (Steam.DetailResult) steamResponse.get(APP_ID);
        when(objectMapper.convertValue(detailResult, Steam.DetailResult.class)).thenReturn(detailResult);

        BotResponse response = steam.parse(request).get(0);

        FileResponse fileResponse = TestUtils.checkDefaultFileResponseImageParams(response);
        assertEquals(expectedMessageText, fileResponse.getText());
        assertEquals(1, fileResponse.getFiles().size());
        File file = fileResponse.getFiles().get(0);
        assertEquals(IMAGE_URL, file.getUrl());
    }

    @Test
    void parseWithNotAvailableRegionTest() {
        final String expectedMessageText = """
                <b><u>name</u></b>
                ID: <code>123</code>
                <b>18+</b>
                ${command.steam.releasedate}: <b>1 january 2000</b>
                ${command.steam.developers}: <b>developer1,developer2</b>
                ${command.steam.price}: <b>$256</b>
                description
                <a href="https://store.steampowered.com/app/123">${command.steam.store}</a>""";
        String lang = "ru";
        BotRequest request = TestUtils.getRequestFromGroup("steam " + APP_ID);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(lang);
        Map<String, Object> steamResponse = getSomeDetailsResponse();
        when(detailsResponse.getBody()).thenReturn(steamResponse);
        when(botRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(detailsResponse);
        Steam.DetailResult detailResult = (Steam.DetailResult) steamResponse.get(APP_ID);
        when(objectMapper.convertValue(detailResult, Steam.DetailResult.class))
                .thenReturn(new Steam.DetailResult().setSuccess(false))
                .thenReturn(detailResult);

        BotResponse response = steam.parse(request).get(0);

        FileResponse fileResponse = TestUtils.checkDefaultFileResponseImageParams(response);
        assertEquals(expectedMessageText, fileResponse.getText());
        assertEquals(1, fileResponse.getFiles().size());
        File file = fileResponse.getFiles().get(0);
        assertEquals(IMAGE_URL, file.getUrl());
    }

    private Map<String, Object> getSomeDetailsResponse() {
        return Map.of(APP_ID, new Steam.DetailResult()
                .setSuccess(true)
                .setData(new Steam.AppData()
                        .setName("name")
                        .setAppId(123L)
                        .setRequiredAge(18)
                        .setDescription("description")
                        .setImageUrl(IMAGE_URL)
                        .setDevelopers(List.of("developer1", "developer2"))
                        .setPrice(new Steam.Price().setFinalFormatted("$256"))
                        .setReleaseDate(new Steam.ReleaseDate().setDate("1 january 2000"))));
    }

}