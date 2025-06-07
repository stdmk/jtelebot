package org.telegram.bot.commands;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.NewsMessage;
import org.telegram.bot.domain.entities.NewsSource;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.bot.utils.RssMapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsTest {

    private static final String NEWS_SOURCE1_NAME = "newsSource1";
    private static final String RSS_FEED_URL = "http://example.com/rss1";

    @Mock
    private Bot bot;
    @Mock
    private NewsService newsService;
    @Mock
    private NewsSourceService newsSourceService;
    @Mock
    private NewsMessageService newsMessageService;
    @Mock
    private RssMapper rssMapper;
    @Mock
    private SpeechService speechService;
    @Mock
    private NetworkUtils networkUtils;
    @Mock
    private BotStats botStats;

    @InjectMocks
    private News news;

    @Test
    void parseWithoutArgumentsTest() {
        final String firstNewsMessageText = "firstNewsMessageText";
        final String secondNewsMessageText = "secondNewsMessageText";
        final String expectedResponseText = "<b>${command.news.lastnews}:</b>\n\n" + firstNewsMessageText + secondNewsMessageText;
        BotRequest request = TestUtils.getRequestFromGroup("news");

        when(newsService.getAll(any(Chat.class))).thenReturn(getSomeNews());
        when(rssMapper.toShortNewsMessageText(any(NewsMessage.class), anyString()))
                .thenReturn(firstNewsMessageText)
                .thenReturn(secondNewsMessageText);

        BotResponse response = news.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseWithWrongNumberOfNewsEntityAsArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("news_test");

        assertThrows(BotException.class, () -> news.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithNumberOfNonExistenceNewsEntityAsArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("news_1");

        assertThrows(BotException.class, () -> news.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithNumberOfNewsEntityAsArgumentWithoutAttachInEntityTest() {
        final String expectedResponseText = "expectedResponseText";
        final long newsId = 1;
        BotRequest request = TestUtils.getRequestFromGroup("news_" + newsId);

        when(newsMessageService.get(newsId)).thenReturn(getSomeNews().get(0).getNewsSource().getNewsMessage());
        when(rssMapper.toFullNewsMessageText(any(NewsMessage.class))).thenReturn(expectedResponseText);

        BotResponse response = news.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithNumberOfNewsEntityAsArgumentWithAttachInEntityTest() {
        final String expectedResponseText = "expectedResponseText";
        final long newsId = 1;
        BotRequest request = TestUtils.getRequestFromGroup("news_" + newsId);

        when(newsMessageService.get(newsId)).thenReturn(getSomeNews().get(1).getNewsSource().getNewsMessage());
        when(rssMapper.toFullNewsMessageText(any(NewsMessage.class))).thenReturn(expectedResponseText);

        BotResponse response = news.parse(request).get(0);
        FileResponse photo = TestUtils.checkDefaultFileResponseImageParams(response);

        assertEquals(expectedResponseText, photo.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"21_22", "19_18", "1_19"})
    void parseWithNewsArrayAsArgumentWrongInputTest(String argument) {
        BotRequest request = TestUtils.getRequestFromGroup("news_" + argument);

        when(newsMessageService.getLastNewsMessage()).thenReturn(new NewsMessage().setId(20L));

        assertThrows(BotException.class, () -> news.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithNewsArrayAsArgumentNotExistenceTest() {
        BotRequest request = TestUtils.getRequestFromGroup("news_10_11");

        when(newsMessageService.getLastNewsMessage()).thenReturn(new NewsMessage().setId(20L));
        when(newsMessageService.getAll(anyList())).thenReturn(Collections.emptyList());

        assertThrows(BotException.class, () -> news.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithNewsArrayAsArgumentTest() {
        final String expectedResponse = "news message1news message2news message3news message4news message5news message6news message7news message8news message9news message10${command.news.morenews}: /news_12_20";
        BotRequest request = TestUtils.getRequestFromGroup("news_10_11");
        List<NewsMessage> newsMessages = LongStream.range(1, 11).mapToObj(this::getNewsMessage).toList();

        when(newsMessageService.getLastNewsMessage()).thenReturn(new NewsMessage().setId(20L));
        when(newsMessageService.getAll(anyList())).thenReturn(newsMessages);
        when(rssMapper.toShortNewsMessageText(any(NewsMessage.class))).thenAnswer(answer -> "news message" + ((NewsMessage) answer.getArgument(0)).getId());

        BotResponse botResponse = news.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponse, textResponse.getText());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    private NewsMessage getNewsMessage(long id) {
        NewsMessage newsMessage = new NewsMessage();
        newsMessage.setId(id);
        return newsMessage;
    }

    @Test
    void parseWithNewsSourceNameAsArgumentWithFeedExceptionTest() throws FeedException, IOException {
        BotRequest request = TestUtils.getRequestFromGroup("news " + NEWS_SOURCE1_NAME);
        org.telegram.bot.domain.entities.News newsEntity = getSomeNews().get(0);

        when(newsService.get(any(Chat.class), anyString())).thenReturn(newsEntity);
        when(networkUtils.getRssFeedFromUrl(RSS_FEED_URL)).thenThrow(new FeedException("error"));

        assertThrows(BotException.class, () -> news.parse(request));

        verify(botStats).incrementErrors(anyString(), any(Throwable.class), anyString());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithNewsSourceNameAsArgumentWithMalformedUrlExceptionTest() throws FeedException, IOException {
        BotRequest request = TestUtils.getRequestFromGroup("news " + NEWS_SOURCE1_NAME);
        org.telegram.bot.domain.entities.News newsEntity = getSomeNews().get(0);

        when(newsService.get(any(Chat.class), anyString())).thenReturn(newsEntity);
        when(networkUtils.getRssFeedFromUrl(RSS_FEED_URL)).thenThrow(new MalformedURLException("error"));

        assertThrows(BotException.class, () -> news.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithNewsSourceNameAsArgumentWithIOExceptionTest() throws FeedException, IOException {
        BotRequest request = TestUtils.getRequestFromGroup("news " + NEWS_SOURCE1_NAME);
        org.telegram.bot.domain.entities.News newsEntity = getSomeNews().get(0);

        when(newsService.get(any(Chat.class), anyString())).thenReturn(newsEntity);
        when(networkUtils.getRssFeedFromUrl(RSS_FEED_URL)).thenThrow(new IOException("error"));

        assertThrows(BotException.class, () -> news.parse(request));

        verify(botStats).incrementErrors(anyString(), any(Throwable.class), anyString());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithNewsSourceNameAsArgumentTest() throws FeedException, IOException {
        final String firstNewsMessageText = "firstNewsMessageText";
        final String secondNewsMessageText = "secondNewsMessageText";
        final String expectedResponseText = firstNewsMessageText + secondNewsMessageText;
        BotRequest request = TestUtils.getRequestFromGroup("news " + NEWS_SOURCE1_NAME);
        org.telegram.bot.domain.entities.News newsEntity = getSomeNews().get(0);
        List<NewsMessage> newsMessageList = getSomeNews()
                .stream()
                .map(org.telegram.bot.domain.entities.News::getNewsSource)
                .map(NewsSource::getNewsMessage)
                .toList();

        when(newsService.get(any(Chat.class), anyString())).thenReturn(newsEntity);
        when(networkUtils.getRssFeedFromUrl(RSS_FEED_URL)).thenReturn(mock(SyndFeed.class));
        when(rssMapper.toNewsMessage(anyList())).thenReturn(newsMessageList);
        when(newsMessageService.save(anyList()))
                .thenReturn(newsMessageList);
        when(rssMapper.toShortNewsMessageText(any(NewsMessage.class)))
                .thenReturn(firstNewsMessageText)
                .thenReturn(secondNewsMessageText);

        BotResponse response = news.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithUrlAsArgumentTest() throws FeedException, IOException {
        final String firstNewsMessageText = "firstNewsMessageText";
        final String secondNewsMessageText = "secondNewsMessageText";
        final String expectedResponseText = firstNewsMessageText + secondNewsMessageText;
        BotRequest request = TestUtils.getRequestFromGroup("news " + RSS_FEED_URL);
        List<NewsMessage> newsMessageList = getSomeNews()
                .stream()
                .map(org.telegram.bot.domain.entities.News::getNewsSource)
                .map(NewsSource::getNewsMessage)
                .toList();

        when(networkUtils.getRssFeedFromUrl(RSS_FEED_URL)).thenReturn(mock(SyndFeed.class));
        when(rssMapper.toNewsMessage(anyList())).thenReturn(newsMessageList);
        when(newsMessageService.save(anyList())).thenReturn(newsMessageList);
        when(rssMapper.toShortNewsMessageText(any(NewsMessage.class)))
                .thenReturn(firstNewsMessageText)
                .thenReturn(secondNewsMessageText);

        BotResponse response = news.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithUrlAndWrongNewsCountAsArgumentsTest() {
        BotRequest request = TestUtils.getRequestFromGroup("news " + RSS_FEED_URL + " 0");

        assertThrows(BotException.class, () -> news.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithUrlAndNewsCountAsArgumentsTest() throws FeedException, IOException {
        final String fullNewsMessageText = "fullNewsMessageText";
        BotRequest request = TestUtils.getRequestFromGroup("news " + RSS_FEED_URL + " 1");

        SyndFeed syndFeed = mock(SyndFeed.class);
        when(syndFeed.getEntries()).thenReturn(List.of(mock(SyndEntry.class)));
        when(networkUtils.getRssFeedFromUrl(RSS_FEED_URL)).thenReturn(syndFeed);
        when(rssMapper.toFullNewsMessageText(any(SyndEntry.class))).thenReturn(fullNewsMessageText);

        BotResponse response = news.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(fullNewsMessageText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithSearchingNewsFoundNothingTest() throws FeedException, IOException {
        BotRequest request = TestUtils.getRequestFromGroup("news test \"testych test\"");
        List<org.telegram.bot.domain.entities.News> newsList = getSomeNews();
        List<NewsSource> sourceList = newsList
                .stream()
                .map(org.telegram.bot.domain.entities.News::getNewsSource)
                .toList();

        when(newsSourceService.getAll()).thenReturn(sourceList);

        SyndFeed syndFeed = mock(SyndFeed.class);
        when(syndFeed.getEntries()).thenReturn(List.of());
        when(networkUtils.getRssFeedFromUrl(RSS_FEED_URL)).thenThrow(new BotException("")).thenReturn(syndFeed);
        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn("not found");

        assertThrows(BotException.class, () -> news.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithSearchingNewsTest() throws FeedException, IOException {
        final String firstNewsMessageText = "firstNewsMessageText";
        final String secondNewsMessageText = "secondNewsMessageText";
        final String expectedResponseText = firstNewsMessageText + secondNewsMessageText;
        BotRequest request = TestUtils.getRequestFromGroup("news test \"testych test\"");
        List<org.telegram.bot.domain.entities.News> newsList = getSomeNews();
        List<NewsSource> sourceList = newsList
                .stream()
                .map(org.telegram.bot.domain.entities.News::getNewsSource)
                .toList();
        List<NewsMessage> newsMessageList = newsList
                .stream()
                .map(org.telegram.bot.domain.entities.News::getNewsSource)
                .map(NewsSource::getNewsMessage)
                .toList();

        when(newsSourceService.getAll()).thenReturn(sourceList);

        SyndEntry firstMatchesSyndEntry = mock(SyndEntry.class);
        when(firstMatchesSyndEntry.getTitle()).thenReturn("test");

        SyndEntry secondMatchesSyndEntry = mock(SyndEntry.class);
        when(secondMatchesSyndEntry.getTitle()).thenReturn("title");
        SyndContent syndContent = mock(SyndContent.class);
        when(syndContent.getValue()).thenReturn("tratatam testych test tratatam");
        when(secondMatchesSyndEntry.getDescription()).thenReturn(syndContent);

        SyndEntry notMatchesSyndEntry = mock(SyndEntry.class);
        SyndContent notMatchesSyndContent = mock(SyndContent.class);
        when(notMatchesSyndContent.getValue()).thenReturn("tratatam tratatam");
        when(notMatchesSyndEntry.getDescription()).thenReturn(notMatchesSyndContent);

        SyndFeed syndFeed = mock(SyndFeed.class);
        when(syndFeed.getEntries()).thenReturn(List.of(firstMatchesSyndEntry, secondMatchesSyndEntry, notMatchesSyndEntry));
        SyndFeed syndFeed2 = mock(SyndFeed.class);
        when(syndFeed2.getEntries()).thenReturn(List.of(secondMatchesSyndEntry, firstMatchesSyndEntry, notMatchesSyndEntry));
        when(networkUtils.getRssFeedFromUrl(RSS_FEED_URL)).thenReturn(syndFeed).thenReturn(syndFeed2);
        when(rssMapper.toNewsMessage(firstMatchesSyndEntry)).thenReturn(newsMessageList.get(0));
        when(rssMapper.toNewsMessage(secondMatchesSyndEntry)).thenReturn(newsMessageList.get(1));
        when(newsMessageService.save(anyList())).thenReturn(newsMessageList);
        when(rssMapper.toShortNewsMessageText(any(NewsMessage.class), anyString(), anyString()))
                .thenReturn(firstNewsMessageText)
                .thenReturn(secondNewsMessageText);

        BotResponse response = news.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(rssMapper, never()).toNewsMessage(notMatchesSyndEntry);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    private List<org.telegram.bot.domain.entities.News> getSomeNews() {
        return List.of(
                new org.telegram.bot.domain.entities.News()
                        .setNewsSource(new NewsSource()
                                .setName(NEWS_SOURCE1_NAME)
                                .setUrl(RSS_FEED_URL)
                                .setNewsMessage(new NewsMessage())),
                new org.telegram.bot.domain.entities.News()
                        .setNewsSource(new NewsSource()
                                .setName(NEWS_SOURCE1_NAME)
                                .setUrl(RSS_FEED_URL)
                                .setNewsMessage(new NewsMessage()
                                        .setAttachUrl("http://example.com/2.jpg"))));
    }

}