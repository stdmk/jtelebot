package org.telegram.bot.domain.commands;

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
import org.telegram.bot.domain.entities.ImageUrl;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.ImageUrlService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GooglePicsTest {

    @Mock
    private Bot bot;
    @Mock
    private PropertiesConfig propertiesConfig;
    @Mock
    private SpeechService speechService;
    @Mock
    private ImageUrlService imageUrlService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private RestTemplate botRestTemplate;
    @Mock
    private BotStats botStats;
    @Mock
    private NetworkUtils networkUtils;
    @Mock
    private ResponseEntity<GooglePics.GooglePicsSearchData> response;
    @Captor
    private ArgumentCaptor<List<ImageUrl>> imageUrlListCaptor;

    @InjectMocks
    private GooglePics googlePics;

    @Test
    void googlePicsWithEmptyTextMessageTest() {
        Update update = TestUtils.getUpdateFromGroup("picture");

        PartialBotApiMethod<?> method = googlePics.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(method);
        verify(commandWaitingService).add(any(Message.class), any(Class.class));
    }

    @Test
    void googlePicsByInvalidImageUrlIdTest() {
        Update update = TestUtils.getUpdateFromGroup("picture_a");
        assertThrows(BotException.class, () -> googlePics.parse(update));
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void googlePicsByNotExistentImageUrlIdTest() {
        Update update = TestUtils.getUpdateFromGroup("picture_1");

        when(imageUrlService.get(anyLong())).thenReturn(null);

        assertThrows(BotException.class, () -> googlePics.parse(update));
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void googlePicsWithExceptionDuringDownloadingTest() throws Exception {
        final String url = "url";
        Update update = TestUtils.getUpdateFromGroup("picture_1");

        when(imageUrlService.get(anyLong())).thenReturn(new ImageUrl().setUrl(url));
        when(networkUtils.getFileFromUrlWithLimit(anyString())).thenThrow(new IOException());

        BotException botException = assertThrows(BotException.class, () -> googlePics.parse(update));
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        assertTrue(botException.getMessage().contains(url));
    }

    @Test
    void googlePicsByImageUrlIdTest() throws Exception {
        final String url = "url";
        Update update = TestUtils.getUpdateFromGroup("picture_1");

        when(imageUrlService.get(anyLong())).thenReturn(new ImageUrl().setUrl(url).setTitle("title"));
        when(networkUtils.getFileFromUrlWithLimit(anyString())).thenReturn(Mockito.mock(InputStream.class));

        PartialBotApiMethod<?> method = googlePics.parse(update);
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        TestUtils.checkDefaultSendPhotoParams(method);
    }

    @Test
    void googlePicsWithoutTokenTest() {
        Update update = TestUtils.getUpdateFromGroup("picture test");

        when(propertiesConfig.getGoogleToken()).thenReturn(null);

        assertThrows(BotException.class, () -> googlePics.parse(update));
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN);
    }

    @Test
    void googlePicsWithUnavailableApiTest() {
        Update update = TestUtils.getUpdateFromGroup("picture test");

        when(propertiesConfig.getGoogleToken()).thenReturn("123");
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Google.GoogleSearchData>>any()))
                .thenThrow(new RestClientException(""));

        assertThrows(BotException.class, () -> googlePics.parse(update));
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void googlePicsWithNullResponseTest() {
        Update update = TestUtils.getUpdateFromGroup("picture test");
        GooglePics.GooglePicsSearchData googlePicsSearchData = new GooglePics.GooglePicsSearchData();

        when(propertiesConfig.getGoogleToken()).thenReturn("123");
        when(response.getBody()).thenReturn(googlePicsSearchData);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GooglePics.GooglePicsSearchData>>any()))
                .thenReturn(response);

        assertThrows(BotException.class, () -> googlePics.parse(update));
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        verify(botStats).incrementGoogleRequests();
        verify(speechService).getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
    }

    @Test
    void googlePicsTest() {
        Update update = TestUtils.getUpdateFromGroup("picture test");

        GooglePics.GooglePicsSearchItem googlePicsSearchItem = new GooglePics.GooglePicsSearchItem()
                .setTitle("title")
                .setLink("link");
        GooglePics.GooglePicsSearchData googlePicsSearchData = new GooglePics.GooglePicsSearchData()
                .setItems(List.of(googlePicsSearchItem));

        when(propertiesConfig.getGoogleToken()).thenReturn("123");
        when(response.getBody()).thenReturn(googlePicsSearchData);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GooglePics.GooglePicsSearchData>>any()))
                .thenReturn(response);
        when(imageUrlService.save(anyList())).thenReturn(
                List.of(
                        new ImageUrl().setUrl(googlePicsSearchItem.getLink()).setTitle(googlePicsSearchItem.getTitle())));

        PartialBotApiMethod<?> method = googlePics.parse(update);
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        SendMediaGroup sendMediaGroup = TestUtils.checkDefaultSendMediaGroupParams(method);

        InputMedia inputMedia = sendMediaGroup.getMedias().get(0);
        assertEquals(googlePicsSearchItem.getLink(), inputMedia.getMedia());
        assertNotNull(inputMedia.getCaption());

        verify(botStats).incrementGoogleRequests();

        verify(imageUrlService).save(imageUrlListCaptor.capture());

        List<ImageUrl> imageUrlList = imageUrlListCaptor.getValue();
        assertFalse(imageUrlList.isEmpty());

        ImageUrl imageUrl = imageUrlList.get(0);
        assertEquals(googlePicsSearchItem.getTitle(), imageUrl.getTitle());
        assertEquals(googlePicsSearchItem.getLink(), imageUrl.getUrl());

    }

}