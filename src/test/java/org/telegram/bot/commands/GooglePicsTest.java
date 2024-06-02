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
import org.telegram.bot.domain.entities.ImageUrl;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.ImageUrlService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.utils.NetworkUtils;

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
        BotRequest request = TestUtils.getRequestFromGroup("picture");

        BotResponse response = googlePics.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TestUtils.checkDefaultTextResponseParams(response);
        verify(commandWaitingService).add(any(Message.class), any(Class.class));
    }

    @Test
    void googlePicsByInvalidImageUrlIdTest() {
        BotRequest request = TestUtils.getRequestFromGroup("picture_a");
        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());

        assertThrows(BotException.class, () -> googlePics.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void googlePicsByNotExistentImageUrlIdTest() {
        BotRequest request = TestUtils.getRequestFromGroup("picture_1");

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(imageUrlService.get(anyLong())).thenReturn(null);

        assertThrows(BotException.class, () -> googlePics.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void googlePicsWithExceptionDuringDownloadingTest() throws Exception {
        final String url = "url";
        BotRequest request = TestUtils.getRequestFromGroup("picture_1");

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(imageUrlService.get(anyLong())).thenReturn(new ImageUrl().setUrl(url));
        when(networkUtils.getFileFromUrlWithLimit(anyString())).thenThrow(new IOException());

        BotException botException = assertThrows(BotException.class, () -> googlePics.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        assertTrue(botException.getMessage().contains(url));
    }

    @Test
    void googlePicsByImageUrlIdTest() throws Exception {
        final String url = "url";
        BotRequest request = TestUtils.getRequestFromGroup("picture_1");

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(imageUrlService.get(anyLong())).thenReturn(new ImageUrl().setUrl(url).setTitle("title"));
        when(networkUtils.getFileFromUrlWithLimit(anyString())).thenReturn(Mockito.mock(InputStream.class));

        BotResponse response = googlePics.parse(request).get(0);
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        TestUtils.checkDefaultFileResponseImageParams(response);
    }

    @Test
    void googlePicsWithoutTokenTest() {
        BotRequest request = TestUtils.getRequestFromGroup("picture test");

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(propertiesConfig.getGoogleToken()).thenReturn(null);

        assertThrows(BotException.class, () -> googlePics.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN);
    }

    @Test
    void googlePicsWithUnavailableApiTest() {
        BotRequest request = TestUtils.getRequestFromGroup("picture test");

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(propertiesConfig.getGoogleToken()).thenReturn("123");
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Google.GoogleSearchData>>any()))
                .thenThrow(new RestClientException(""));

        assertThrows(BotException.class, () -> googlePics.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void googlePicsWithNullResponseTest() {
        BotRequest request = TestUtils.getRequestFromGroup("picture test");
        GooglePics.GooglePicsSearchData googlePicsSearchData = new GooglePics.GooglePicsSearchData();

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(propertiesConfig.getGoogleToken()).thenReturn("123");
        when(response.getBody()).thenReturn(googlePicsSearchData);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GooglePics.GooglePicsSearchData>>any()))
                .thenReturn(response);

        assertThrows(BotException.class, () -> googlePics.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(botStats).incrementGoogleRequests();
        verify(speechService).getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
    }

    @Test
    void googlePicsTest() {
        BotRequest request = TestUtils.getRequestFromGroup("picture test");

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
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
                        new ImageUrl().setUrl(googlePicsSearchItem.getLink()).setTitle(googlePicsSearchItem.getTitle()),
                        new ImageUrl().setUrl(googlePicsSearchItem.getLink()).setTitle(googlePicsSearchItem.getTitle())));

        BotResponse response = googlePics.parse(request).get(0);
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        FileResponse mediaGroup = TestUtils.checkDefaultResponseMultipleImagesParams(response);

        File file = mediaGroup.getFiles().get(0);
        assertEquals(googlePicsSearchItem.getLink(), file.getUrl());

        verify(botStats).incrementGoogleRequests();

        verify(imageUrlService).save(imageUrlListCaptor.capture());

        List<ImageUrl> imageUrlList = imageUrlListCaptor.getValue();
        assertFalse(imageUrlList.isEmpty());

        ImageUrl imageUrl = imageUrlList.get(0);
        assertEquals(googlePicsSearchItem.getTitle(), imageUrl.getTitle());
        assertEquals(googlePicsSearchItem.getLink(), imageUrl.getUrl());

    }

}