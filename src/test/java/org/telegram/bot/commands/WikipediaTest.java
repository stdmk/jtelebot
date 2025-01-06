package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.Wiki;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.WikiService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WikipediaTest {

    @Mock
    private Bot bot;
    @Mock
    private WikiService wikiService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private SpeechService speechService;
    @Mock
    private LanguageResolver languageResolver;
    @Mock
    private RestTemplate botRestTemplate;

    @Mock
    private ResponseEntity<Object[]> responseSearch;
    @Mock
    private ResponseEntity<Wikipedia.WikiData> responseWikiData;
    @Mock
    private ResponseEntity<Wikipedia.WikiData> afterSearchResponseWikiData;
    @Mock
    private ResponseEntity<Wikipedia.WikiData> afterSearchResponseWikiData2;

    @InjectMocks
    private Wikipedia wikipedia;

    @Test
    void parseWithoutArgumentTest() {
        final String expectedResponseText = "${command.wikipedia.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestFromGroup("wiki");
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());

        BotResponse botResponse = wikipedia.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(commandWaitingService).add(message, Wikipedia.class);
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithWrongWikiIdAsArgumentTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("wiki_a");
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> wikipedia.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithNotExistenceWikiIdAsArgumentTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("wiki_1");
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> wikipedia.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithWikiIdAsArgumentTest() {
        final String expectedResponseText = """
                <b>title</b>
                text
                <a href="https://ru.wikipedia.org/wiki/title">${command.wikipedia.articlelink}</a>
                """;
        final Integer wikiId = 1;
        BotRequest request = TestUtils.getRequestFromGroup("wiki_" + wikiId);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(wikiService.get(wikiId)).thenReturn(getSomeWiki());

        BotResponse botResponse = wikipedia.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithTitleAsArgumentRestClientExceptionTest() {
        final String expectedErrorText = "error";
        final String title = "test";
        final String expectedApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + title;
        final String expectedSearchApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=opensearch&search=" + title;
        BotRequest request = TestUtils.getRequestFromGroup("wiki " + title);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(request)).thenReturn("en");
        when(botRestTemplate.getForEntity(expectedApiUrl, Wikipedia.WikiData.class)).thenThrow(new RestClientException(""));
        when(botRestTemplate.getForEntity(expectedSearchApiUrl, Object[].class)).thenReturn(responseSearch);
        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn(expectedErrorText);

        BotResponse botResponse = wikipedia.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedErrorText, textResponse.getText());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithTitleAsArgumentEmptyResponseTest() {
        final String expectedErrorText = "error";
        final String title = "test";
        final String expectedApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + title;
        final String expectedSearchApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=opensearch&search=" + title;
        BotRequest request = TestUtils.getRequestFromGroup("wiki " + title);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(request)).thenReturn("en");
        when(botRestTemplate.getForEntity(expectedApiUrl, Wikipedia.WikiData.class)).thenReturn(responseWikiData);
        when(botRestTemplate.getForEntity(expectedSearchApiUrl, Object[].class)).thenReturn(responseSearch);
        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn(expectedErrorText);

        BotResponse botResponse = wikipedia.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedErrorText, textResponse.getText());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithTitleAsArgumentNullableResponseTextTest() {
        final String expectedErrorText = "error";
        final String title = "test";
        final String expectedApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + title;
        final String expectedSearchApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=opensearch&search=" + title;
        BotRequest request = TestUtils.getRequestFromGroup("wiki " + title);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(request)).thenReturn("en");
        Wikipedia.WikiData wikiData = new Wikipedia.WikiData().setQuery(new Wikipedia.WikiQuery());
        when(responseWikiData.getBody()).thenReturn(wikiData);
        when(botRestTemplate.getForEntity(expectedApiUrl, Wikipedia.WikiData.class)).thenReturn(responseWikiData);
        when(botRestTemplate.getForEntity(expectedSearchApiUrl, Object[].class)).thenReturn(responseSearch);
        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn(expectedErrorText);

        BotResponse botResponse = wikipedia.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedErrorText, textResponse.getText());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithTitleAsArgumentEmptyResponseTextTest() {
        final String expectedErrorText = "error";
        final String title = "test";
        final String expectedApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + title;
        final String expectedSearchApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=opensearch&search=" + title;
        BotRequest request = TestUtils.getRequestFromGroup("wiki " + title);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(request)).thenReturn("en");
        Wikipedia.WikiPage wikiPage = new Wikipedia.WikiPage()
                .setPageid(1)
                .setTitle("TEST")
                .setExtract("");
        Wikipedia.WikiData wikiData = new Wikipedia.WikiData()
                .setQuery(new Wikipedia.WikiQuery()
                        .setPages(new Wikipedia.WikiPages()
                                .setWikiPage(wikiPage)));
        when(responseWikiData.getBody()).thenReturn(wikiData);
        when(botRestTemplate.getForEntity(expectedApiUrl, Wikipedia.WikiData.class)).thenReturn(responseWikiData);
        when(botRestTemplate.getForEntity(expectedSearchApiUrl, Object[].class)).thenReturn(responseSearch);
        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn(expectedErrorText);

        BotResponse botResponse = wikipedia.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedErrorText, textResponse.getText());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithTitleAsArgumentTest() {
        final String expectedResponseText = """
                <b>TEST</b>
                extract
                <a href="https://ru.wikipedia.org/wiki/TEST">${command.wikipedia.articlelink}</a>
                """;
        final String title = "test";
        final String expectedApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + title;
        BotRequest request = TestUtils.getRequestFromGroup("wiki " + title);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(request)).thenReturn("en");
        Wikipedia.WikiPage wikiPage = new Wikipedia.WikiPage()
                .setPageid(1)
                .setTitle("TEST")
                .setExtract("extract");
        Wikipedia.WikiData wikiData = new Wikipedia.WikiData()
                .setQuery(new Wikipedia.WikiQuery()
                        .setPages(new Wikipedia.WikiPages()
                                .setWikiPage(wikiPage)));
        when(responseWikiData.getBody()).thenReturn(wikiData);
        when(botRestTemplate.getForEntity(expectedApiUrl, Wikipedia.WikiData.class)).thenReturn(responseWikiData);

        BotResponse botResponse = wikipedia.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithTitleAsArgumentSearchFoundStrangeTest() {
        final String expectedErrorText = "error";
        final String title = "test";
        final String expectedApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + title;
        final String expectedSearchApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=opensearch&search=" + title;
        BotRequest request = TestUtils.getRequestFromGroup("wiki " + title);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(request)).thenReturn("en");
        Wikipedia.WikiData wikiData = new Wikipedia.WikiData().setQuery(new Wikipedia.WikiQuery());
        when(responseWikiData.getBody()).thenReturn(wikiData);
        when(botRestTemplate.getForEntity(expectedApiUrl, Wikipedia.WikiData.class)).thenReturn(responseWikiData);
        Object[] response = new Object[2];
        response[0] = new Object();
        response[1] = new Object();
        when(responseSearch.getBody()).thenReturn(response);
        when(botRestTemplate.getForEntity(expectedSearchApiUrl, Object[].class)).thenReturn(responseSearch);
        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn(expectedErrorText);

        BotResponse botResponse = wikipedia.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedErrorText, textResponse.getText());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithTitleAsArgumentSearchFoundButWikiNotFoundTest() {
        final String expectedErrorText = "error";
        final String title = "test";
        final String foundTitle = "TITLE";
        final String expectedApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + title;
        final String expectedSearchApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=opensearch&search=" + title;
        final String afterSearchExpectedApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + foundTitle;
        BotRequest request = TestUtils.getRequestFromGroup("wiki " + title);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(request)).thenReturn("en");
        Wikipedia.WikiData wikiData = new Wikipedia.WikiData().setQuery(new Wikipedia.WikiQuery());
        when(responseWikiData.getBody()).thenReturn(wikiData);
        when(botRestTemplate.getForEntity(expectedApiUrl, Wikipedia.WikiData.class)).thenReturn(responseWikiData);
        Object[] response = new Object[2];
        response[0] = new Object();
        response[1] = List.of(foundTitle);
        when(responseSearch.getBody()).thenReturn(response);
        when(botRestTemplate.getForEntity(expectedSearchApiUrl, Object[].class)).thenReturn(responseSearch);
        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn(expectedErrorText);
        when(botRestTemplate.getForEntity(afterSearchExpectedApiUrl, Wikipedia.WikiData.class)).thenReturn(responseWikiData);

        BotResponse botResponse = wikipedia.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedErrorText, textResponse.getText());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithTitleAsArgumentSearchFoundOneWikiButEmptyTextTest() {
        final String expectedErrorText = "error";
        final String title = "test";
        final String foundTitle = "TITLE";
        final String expectedApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + title;
        final String expectedSearchApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=opensearch&search=" + title;
        final String afterSearchExpectedApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + foundTitle;
        BotRequest request = TestUtils.getRequestFromGroup("wiki " + title);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(request)).thenReturn("en");
        Wikipedia.WikiData wikiEmptyData = new Wikipedia.WikiData().setQuery(new Wikipedia.WikiQuery());
        when(responseWikiData.getBody()).thenReturn(wikiEmptyData);
        when(botRestTemplate.getForEntity(expectedApiUrl, Wikipedia.WikiData.class)).thenReturn(responseWikiData);
        Object[] response = new Object[2];
        response[0] = new Object();
        response[1] = List.of(foundTitle);
        when(responseSearch.getBody()).thenReturn(response);
        when(botRestTemplate.getForEntity(expectedSearchApiUrl, Object[].class)).thenReturn(responseSearch);
        Wikipedia.WikiPage wikiPage = new Wikipedia.WikiPage()
                .setPageid(1)
                .setTitle("TEST")
                .setExtract("");
        Wikipedia.WikiData wikiData = new Wikipedia.WikiData()
                .setQuery(new Wikipedia.WikiQuery()
                        .setPages(new Wikipedia.WikiPages()
                                .setWikiPage(wikiPage)));
        when(afterSearchResponseWikiData.getBody()).thenReturn(wikiData);
        when(botRestTemplate.getForEntity(afterSearchExpectedApiUrl, Wikipedia.WikiData.class)).thenReturn(afterSearchResponseWikiData);
        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn(expectedErrorText);

        BotResponse botResponse = wikipedia.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedErrorText, textResponse.getText());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithTitleAsArgumentSearchFoundOneWikiTest() {
        final String expectedResponseText = """
                <b>TEST</b>
                extract
                <a href="https://ru.wikipedia.org/wiki/TEST">${command.wikipedia.articlelink}</a>
                """;
        final String title = "test";
        final String foundTitle = "TITLE";
        final String expectedApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + title;
        final String expectedSearchApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=opensearch&search=" + title;
        final String afterSearchExpectedApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + foundTitle;
        BotRequest request = TestUtils.getRequestFromGroup("wiki " + title);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(request)).thenReturn("en");
        Wikipedia.WikiData wikiEmptyData = new Wikipedia.WikiData().setQuery(new Wikipedia.WikiQuery());
        when(responseWikiData.getBody()).thenReturn(wikiEmptyData);
        when(botRestTemplate.getForEntity(expectedApiUrl, Wikipedia.WikiData.class)).thenReturn(responseWikiData);
        Object[] response = new Object[2];
        response[0] = new Object();
        response[1] = List.of(foundTitle);
        when(responseSearch.getBody()).thenReturn(response);
        when(botRestTemplate.getForEntity(expectedSearchApiUrl, Object[].class)).thenReturn(responseSearch);
        Wikipedia.WikiPage wikiPage = new Wikipedia.WikiPage()
                .setPageid(1)
                .setTitle("TEST")
                .setExtract("extract");
        Wikipedia.WikiData wikiData = new Wikipedia.WikiData()
                .setQuery(new Wikipedia.WikiQuery()
                        .setPages(new Wikipedia.WikiPages()
                                .setWikiPage(wikiPage)));
        when(afterSearchResponseWikiData.getBody()).thenReturn(wikiData);
        when(botRestTemplate.getForEntity(afterSearchExpectedApiUrl, Wikipedia.WikiData.class)).thenReturn(afterSearchResponseWikiData);

        BotResponse botResponse = wikipedia.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithTitleAsArgumentSearchFoundTwoWikiTest() {
        final String expectedResponseText = """
                <b>${command.wikipedia.searchresults} test</b>
                TITLE
                /wiki_1
                TiTlE
                /wiki_1""";
        final String title = "test";
        final String foundTitle1 = "TITLE";
        final String foundTitle2 = "TiTlE";
        final String expectedApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + title;
        final String expectedSearchApiUrl = "https://en.wikipedia.org/w/api.php?format=json&action=opensearch&search=" + title;
        final String afterSearchExpectedApiUrlFirstTitle = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + foundTitle1;
        final String afterSearchExpectedApiUrlSecondTitle = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + foundTitle2;
        BotRequest request = TestUtils.getRequestFromGroup("wiki " + title);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(request)).thenReturn("en");
        Wikipedia.WikiData wikiEmptyData = new Wikipedia.WikiData().setQuery(new Wikipedia.WikiQuery());
        when(responseWikiData.getBody()).thenReturn(wikiEmptyData);
        when(botRestTemplate.getForEntity(expectedApiUrl, Wikipedia.WikiData.class)).thenReturn(responseWikiData);
        Object[] response = new Object[2];
        response[0] = new Object();
        response[1] = List.of(foundTitle1, foundTitle2);
        when(responseSearch.getBody()).thenReturn(response);
        when(botRestTemplate.getForEntity(expectedSearchApiUrl, Object[].class)).thenReturn(responseSearch);
        Wikipedia.WikiPage wikiPage = new Wikipedia.WikiPage()
                .setPageid(1)
                .setTitle(foundTitle1)
                .setExtract("extract");
        Wikipedia.WikiData wikiData = new Wikipedia.WikiData()
                .setQuery(new Wikipedia.WikiQuery()
                        .setPages(new Wikipedia.WikiPages()
                                .setWikiPage(wikiPage)));
        when(afterSearchResponseWikiData.getBody()).thenReturn(wikiData);
        when(botRestTemplate.getForEntity(afterSearchExpectedApiUrlFirstTitle, Wikipedia.WikiData.class)).thenReturn(afterSearchResponseWikiData);
        Wikipedia.WikiPage wikiPage2 = new Wikipedia.WikiPage()
                .setPageid(1)
                .setTitle(foundTitle2)
                .setExtract("extract");
        Wikipedia.WikiData wikiData2 = new Wikipedia.WikiData()
                .setQuery(new Wikipedia.WikiQuery()
                        .setPages(new Wikipedia.WikiPages()
                                .setWikiPage(wikiPage2)));
        when(afterSearchResponseWikiData2.getBody()).thenReturn(wikiData2);
        when(botRestTemplate.getForEntity(afterSearchExpectedApiUrlSecondTitle, Wikipedia.WikiData.class)).thenReturn(afterSearchResponseWikiData2);
        when(wikiService.save(any(Wiki.class))).thenAnswer(answer -> answer.getArgument(0));

        BotResponse botResponse = wikipedia.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(message.getChatId());
    }

    private Wiki getSomeWiki() {
        Wiki wiki = new Wiki();

        wiki.setPageId(123);
        wiki.setTitle("title");
        wiki.setText("text");

        return wiki;
    }

}