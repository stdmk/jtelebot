package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.GoogleSearchResult;
import org.telegram.bot.domain.entities.ImageUrl;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.GoogleSearchResultService;
import org.telegram.bot.services.ImageUrlService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleTest {

    @Mock
    private Bot bot;
    @Mock
    private PropertiesConfig propertiesConfig;
    @Mock
    private SpeechService speechService;
    @Mock
    private ImageUrlService imageUrlService;
    @Mock
    private GoogleSearchResultService googleSearchResultService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private RestTemplate botRestTemplate;
    @Mock
    private BotStats botStats;
    @Mock
    private ResponseEntity<Google.GoogleSearchData> response;
    @Captor
    private ArgumentCaptor<List<GoogleSearchResult>> googleSearchResultCaptor;

    @InjectMocks
    private Google google;

    @Test
    void googleWithoutTokenTest() {
        Update update = TestUtils.getUpdateFromGroup("google test");

        when(propertiesConfig.getGoogleToken()).thenReturn(null);

        assertThrows(BotException.class, () -> google.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN);
    }

    @Test
    void googleWithEmptyTextMessage() {
        Update update = TestUtils.getUpdateFromGroup("google");

        when(propertiesConfig.getGoogleToken()).thenReturn("123");

        PartialBotApiMethod<?> method = google.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(method);
        verify(commandWaitingService).add(any(Message.class), any(Class.class));
    }

    @Test
    void getStoredGoogleSearchResultWithInvalidIdTest() {
        Update update = TestUtils.getUpdateFromGroup("google_a");

        when(propertiesConfig.getGoogleToken()).thenReturn("123");

        assertThrows(BotException.class, () -> google.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void getNotExistentStoredGoogleSearchResultTest() {
        Update update = TestUtils.getUpdateFromGroup("google_1");

        when(propertiesConfig.getGoogleToken()).thenReturn("123");
        when(googleSearchResultService.get(anyLong())).thenReturn(null);

        assertThrows(BotException.class, () -> google.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void getStoredGoogleSearchResultTest() {
        Update update = TestUtils.getUpdateFromGroup("google_1");
        GoogleSearchResult googleSearchResult = new GoogleSearchResult()
                .setId(1L)
                .setTitle("title")
                .setSnippet("snippet")
                .setLink("link")
                .setFormattedUrl("formattedUrl");

        when(propertiesConfig.getGoogleToken()).thenReturn("123");
        when(googleSearchResultService.get(anyLong())).thenReturn(googleSearchResult);

        PartialBotApiMethod<?> method = google.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        SendMessage sendMessage = TestUtils.checkDefaultSendMessageParams(method);
        String responseText = sendMessage.getText();

        assertNotNull(responseText);
        assertTrue(responseText.contains(googleSearchResult.getTitle()));
        assertTrue(responseText.contains(googleSearchResult.getSnippet()));
        assertTrue(responseText.contains(googleSearchResult.getLink()));
        assertTrue(responseText.contains(googleSearchResult.getFormattedUrl()));
    }

    @Test
    void getStoredGoogleSearchResultWithPictureTest() {
        Update update = TestUtils.getUpdateFromGroup("google_1");
        ImageUrl imageUrl = new ImageUrl()
                .setId(1L)
                .setTitle("imageTitle")
                .setUrl("url");
        GoogleSearchResult googleSearchResult = new GoogleSearchResult()
                .setId(1L)
                .setTitle("title")
                .setSnippet("snippet")
                .setLink("link")
                .setFormattedUrl("formattedUrl")
                .setImageUrl(imageUrl);

        when(propertiesConfig.getGoogleToken()).thenReturn("123");
        when(googleSearchResultService.get(anyLong())).thenReturn(googleSearchResult);

        PartialBotApiMethod<?> method = google.parse(update);


        verify(bot).sendTyping(update.getMessage().getChatId());
        SendPhoto sendPhoto = TestUtils.checkDefaultSendPhotoParams(method);
        InputFile photo = sendPhoto.getPhoto();

        assertNotNull(photo);
        assertEquals(imageUrl.getUrl(), photo.getAttachName());

        String responseText = sendPhoto.getCaption();

        assertNotNull(responseText);
        assertTrue(responseText.contains(googleSearchResult.getTitle()));
        assertTrue(responseText.contains(googleSearchResult.getSnippet()));
        assertTrue(responseText.contains(googleSearchResult.getLink()));
        assertTrue(responseText.contains(googleSearchResult.getFormattedUrl()));
    }

    @Test
    void googleTextWithUnavailableApiTest() {
        Update update = TestUtils.getUpdateFromGroup("google test");

        when(propertiesConfig.getGoogleToken()).thenReturn("123");
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Google.GoogleSearchData>>any()))
                .thenThrow(new RestClientException(""));

        assertThrows(BotException.class, () -> google.parse(update));

        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void googleWithNullResponseTest() {
        Update update = TestUtils.getUpdateFromGroup("google test");
        Google.GoogleSearchData googleSearchData = new Google.GoogleSearchData();

        when(propertiesConfig.getGoogleToken()).thenReturn("123");
        when(response.getBody()).thenReturn(googleSearchData);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Google.GoogleSearchData>>any()))
                .thenReturn(response);

        assertThrows(BotException.class, () -> google.parse(update));

        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
        verify(botStats).incrementGoogleRequests();
    }

    @Test
    void googleNotFoundedText() {
        Update update = TestUtils.getUpdateFromGroup("google test");

        Google.CseImage cseImage = new Google.CseImage().setSrc("src");
        Google.GoogleSearchItem googleSearchItem = new Google.GoogleSearchItem()
                .setTitle("title")
                .setLink("link")
                .setDisplayLink("displayLink")
                .setSnippet("snippet")
                .setFormattedUrl("formattedUrl")
                .setPagemap(
                        new Google.Pagemap()
                                .setCseImage(List.of(cseImage)));
        Google.GoogleSearchData googleSearchData = new Google.GoogleSearchData().setItems(List.of(googleSearchItem));

        GoogleSearchResult expectedGoogleSearchResult = new GoogleSearchResult()
                .setId(1L)
                .setLink(googleSearchItem.getLink())
                .setDisplayLink(googleSearchItem.getDisplayLink())
                .setTitle(googleSearchItem.getTitle());

        when(propertiesConfig.getGoogleToken()).thenReturn("123");
        when(response.getBody()).thenReturn(googleSearchData);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Google.GoogleSearchData>>any()))
                .thenReturn(response);
        when(imageUrlService.save(any(ImageUrl.class))).thenReturn(new ImageUrl().setUrl(cseImage.getSrc()).setTitle("imageTitle"));
        when(googleSearchResultService.save(anyList())).thenReturn(List.of(expectedGoogleSearchResult));

        PartialBotApiMethod<?> method = google.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        SendMessage sendMessage = TestUtils.checkDefaultSendMessageParams(method);
        String responseText = sendMessage.getText();

        assertNotNull(responseText);
        assertTrue(responseText.contains(googleSearchItem.getLink()));
        assertTrue(responseText.contains(googleSearchItem.getDisplayLink()));
        assertTrue(responseText.contains(googleSearchItem.getTitle()));

        verify(botStats).incrementGoogleRequests();

        ArgumentCaptor<ImageUrl> imageUrlCaptor = ArgumentCaptor.forClass(ImageUrl.class);
        verify(imageUrlService).save(imageUrlCaptor.capture());
        ImageUrl imageUrl = imageUrlCaptor.getValue();

        assertNotNull(imageUrl);
        assertEquals(cseImage.getSrc(), imageUrl.getUrl());

        verify(googleSearchResultService).save(googleSearchResultCaptor.capture());
        List<GoogleSearchResult> resultList = googleSearchResultCaptor.getValue();
        assertFalse(resultList.isEmpty());

        GoogleSearchResult actualGoogleSearchResult = resultList.get(0);
        assertNotNull(actualGoogleSearchResult.getImageUrl());
        assertEquals(googleSearchItem.getTitle(), actualGoogleSearchResult.getTitle());
        assertEquals(googleSearchItem.getLink(), actualGoogleSearchResult.getLink());
        assertEquals(googleSearchItem.getDisplayLink(), actualGoogleSearchResult.getDisplayLink());
        assertEquals(googleSearchItem.getSnippet(), actualGoogleSearchResult.getSnippet());
        assertEquals(googleSearchItem.getFormattedUrl(), actualGoogleSearchResult.getFormattedUrl());
    }

}