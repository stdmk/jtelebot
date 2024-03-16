package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.ImageUrl;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ImageUrlService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageTest {

    @Mock
    private Bot bot;
    @Mock
    private ImageUrlService imageUrlService;
    @Mock
    private SpeechService speechService;
    @Mock
    private NetworkUtils networkUtils;
    @Mock
    private GooglePics googlePics;

    @InjectMocks
    private Image image;

    @Test
    void getRandomImageWithoutImagesIndbTest() {
        Update update = TestUtils.getUpdateFromGroup("image");

        when(imageUrlService.getRandom()).thenReturn(null);

        assertThrows(BotException.class, () -> image.parse(update));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
    }

    @Test
    void getRandomImageWithFailedDownloadingTest() throws IOException {
        ImageUrl imageUrl = getSomeImageUrl();
        final String expectedResponseText = "${command.image.failedtodownload}: " + imageUrl.getUrl() + "\n"
                + Emoji.LEFT_ARROW.getSymbol() + " /image_" + (imageUrl.getId() - 1) + "\n\n";
        Update update = TestUtils.getUpdateFromGroup("image");

        when(imageUrlService.getRandom()).thenReturn(imageUrl);
        when(networkUtils.getFileFromUrlWithLimit(imageUrl.getUrl())).thenThrow(new RuntimeException());

        PartialBotApiMethod<?> method = image.parse(update).get(0);

        SendMessage sendMessage = TestUtils.checkDefaultSendMessageParams(method);
        assertEquals(expectedResponseText, sendMessage.getText());
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
    }

    @Test
    void getRandomImageTest() throws IOException {
        ImageUrl imageUrl = getSomeImageUrl();
        Long imageUrlId = imageUrl.getId();
        final String expectedResponseText = "\n" + Emoji.LEFT_ARROW.getSymbol() + " /image_" + (imageUrlId - 1) + "\n\n"
                + "/image_" + imageUrlId + "\n\n"
                + Emoji.RIGHT_ARROW.getSymbol() + " /image_" + (imageUrlId + 1);
        Update update = TestUtils.getUpdateFromGroup("image");

        when(imageUrlService.getRandom()).thenReturn(imageUrl);
        when(networkUtils.getFileFromUrlWithLimit(imageUrl.getUrl())).thenReturn(mock(InputStream.class));
        when(imageUrlService.isImageUrlExists(imageUrlId + 1)).thenReturn(true);

        PartialBotApiMethod<?> method = image.parse(update).get(0);

        SendPhoto sendPhoto = TestUtils.checkDefaultSendPhotoParams(method);
        assertEquals(expectedResponseText, sendPhoto.getCaption());
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
    }

    @Test
    void getImageFromMalformedUrlTest() {
        Update update = TestUtils.getUpdateFromGroup("image http://example.com:-80");

        assertThrows(BotException.class, () -> image.parse(update));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
    }

    @Test
    void getAlreadyStoredImageFromUrlTest() throws IOException {
        ImageUrl imageUrl = getSomeImageUrl();
        Long imageUrlId = imageUrl.getId();
        final String expectedResponseText = "\n" + Emoji.LEFT_ARROW.getSymbol() + " /image_" + (imageUrlId - 1) + "\n\n"
                + "/image_" + imageUrlId + "\n\n"
                + Emoji.RIGHT_ARROW.getSymbol() + " /image_" + (imageUrlId + 1);
        Update update = TestUtils.getUpdateFromGroup("image " + imageUrl.getUrl());

        when(imageUrlService.get(imageUrl.getUrl())).thenReturn(imageUrl);
        when(networkUtils.getFileFromUrlWithLimit(imageUrl.getUrl())).thenReturn(mock(InputStream.class));
        when(imageUrlService.isImageUrlExists(imageUrlId + 1)).thenReturn(true);

        PartialBotApiMethod<?> method = image.parse(update).get(0);

        SendPhoto sendPhoto = TestUtils.checkDefaultSendPhotoParams(method);
        assertEquals(expectedResponseText, sendPhoto.getCaption());

        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
    }

    @Test
    void getImageFromUrlTest() throws IOException {
        ImageUrl imageUrl = getSomeImageUrl();
        Long imageUrlId = imageUrl.getId();
        final String expectedResponseText = "\n" + Emoji.LEFT_ARROW.getSymbol() + " /image_" + (imageUrlId - 1) + "\n\n"
                + "/image_" + imageUrlId + "\n\n"
                + Emoji.RIGHT_ARROW.getSymbol() + " /image_" + (imageUrlId + 1);
        Update update = TestUtils.getUpdateFromGroup("image " + imageUrl.getUrl());

        when(imageUrlService.get(imageUrl.getUrl())).thenReturn(null);
        when(imageUrlService.save(any(ImageUrl.class))).thenReturn(imageUrl);
        when(networkUtils.getFileFromUrlWithLimit(imageUrl.getUrl())).thenReturn(mock(InputStream.class));
        when(imageUrlService.isImageUrlExists(imageUrlId + 1)).thenReturn(true);

        PartialBotApiMethod<?> method = image.parse(update).get(0);

        SendPhoto sendPhoto = TestUtils.checkDefaultSendPhotoParams(method);
        assertEquals(expectedResponseText, sendPhoto.getCaption());

        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
    }

    @Test
    void getImageByCorruptedIdTest() {
        Update update = TestUtils.getUpdateFromGroup("image_a");

        assertThrows(BotException.class, () -> image.parse(update));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
    }

    @Test
    void getNotExistanceImageByIdTest() {
        Update update = TestUtils.getUpdateFromGroup("image_1");

        assertThrows(BotException.class, () -> image.parse(update));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
    }

    @Test
    void getImageByIdTest() throws IOException {
        ImageUrl imageUrl = getSomeImageUrl();
        Long imageUrlId = imageUrl.getId();
        final String expectedResponseText = "\n" + Emoji.LEFT_ARROW.getSymbol() + " /image_" + (imageUrlId - 1) + "\n\n"
                + "/image_" + imageUrlId + "\n\n"
                + Emoji.RIGHT_ARROW.getSymbol() + " /image_" + (imageUrlId + 1);
        Update update = TestUtils.getUpdateFromGroup("image_" + imageUrlId);

        when(imageUrlService.get(imageUrlId)).thenReturn(imageUrl);
        when(networkUtils.getFileFromUrlWithLimit(imageUrl.getUrl())).thenReturn(mock(InputStream.class));
        when(imageUrlService.isImageUrlExists(imageUrlId + 1)).thenReturn(true);

        PartialBotApiMethod<?> method = image.parse(update).get(0);

        SendPhoto sendPhoto = TestUtils.checkDefaultSendPhotoParams(method);
        assertEquals(expectedResponseText, sendPhoto.getCaption());

        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
    }

    @Test
    void searchImageOnGoogleTest() throws IOException {
        String searchingText = "test";
        ImageUrl imageUrl = getSomeImageUrl();
        Long imageUrlId = imageUrl.getId();
        final String expectedResponseText = "\n" + Emoji.LEFT_ARROW.getSymbol() + " /image_" + (imageUrlId - 1) + "\n\n"
                + "/image_" + imageUrlId + "\n\n"
                + Emoji.RIGHT_ARROW.getSymbol() + " /image_" + (imageUrlId + 1);
        Update update = TestUtils.getUpdateFromGroup("image " + searchingText);

        when(googlePics.searchImagesOnGoogle(searchingText)).thenReturn(List.of(imageUrl));
        when(networkUtils.getFileFromUrlWithLimit(imageUrl.getUrl())).thenReturn(mock(InputStream.class));
        when(imageUrlService.isImageUrlExists(imageUrlId + 1)).thenReturn(true);

        PartialBotApiMethod<?> method = image.parse(update).get(0);

        SendPhoto sendPhoto = TestUtils.checkDefaultSendPhotoParams(method);
        assertEquals(expectedResponseText, sendPhoto.getCaption());

        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
    }

    private ImageUrl getSomeImageUrl() {
        return new ImageUrl()
                .setId(5L)
                .setUrl("http://example.com")
                .setTitle("title");
    }

}