package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetadataTest {

    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private SpeechService speechService;
    @Mock
    private Bot bot;
    @Mock
    private NetworkUtils networkUtils;
    @Mock
    private BotStats botStats;

    @InjectMocks
    private Metadata metadata;

    @Test
    void parseWithParamsTest() {
        Update update = TestUtils.getUpdateFromGroup("metadata test");
        SendMessage sendMessage = metadata.parse(update);
        assertNull(sendMessage);
    }

    @Test
    void parseWithoutFilesTest() {
        Update update = TestUtils.getUpdateFromGroup("metadata");

        SendMessage sendMessage = metadata.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(sendMessage, ParseMode.HTML);
        assertEquals("теперь пришли мне файл", sendMessage.getText());
        verify(commandWaitingService).add(any(Message.class), any(Class.class));
    }

    @Test
    void parseFileWithOverLimitFileSizeTest() {
        Update update = TestUtils.getUpdateFromGroup("metadata");
        Document document = new Document();
        document.setFileSize(20971521L);
        document.setFileId("fileId");

        update.getMessage().setDocument(document);

        assertThrows(BotException.class, () -> metadata.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseFileWithApiExceptionTest() throws TelegramApiException {
        Update update = TestUtils.getUpdateFromGroup("metadata");
        Video video = new Video();
        video.setFileSize(1L);
        video.setFileId("fileId");

        when(networkUtils.getFileFromTelegram(bot, video.getFileId())).thenThrow(new TelegramApiException());

        update.getMessage().setVideo(video);

        assertThrows(BotException.class, () -> metadata.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(botStats).incrementErrors(any(Update.class), any(Throwable.class), anyString());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void parseFileWithLibExceptionTest() throws TelegramApiException {
        Update update = TestUtils.getUpdateFromGroup("metadata");
        Audio audio = new Audio();
        audio.setFileSize(1L);
        audio.setFileId("fileId");

        update.getMessage().setAudio(audio);

        InputStream inputStream = mock(InputStream.class);
        when(networkUtils.getFileFromTelegram(bot, audio.getFileId())).thenReturn(inputStream);

        assertThrows(BotException.class, () -> metadata.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(botStats).incrementErrors(any(Update.class), any(Throwable.class), anyString());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseTest() throws FileNotFoundException, TelegramApiException {
        PhotoSize photoSize = new PhotoSize();
        photoSize.setFileSize(1);
        photoSize.setFileId("fileId");
        Message message = TestUtils.getMessage();
        message.setPhoto(List.of(photoSize));
        Update update = TestUtils.getUpdateWithRepliedMessage(message);
        InputStream file = new FileInputStream("src/test/resources/png.png");

        when(networkUtils.getFileFromTelegram(bot, photoSize.getFileId())).thenReturn(file);

        SendMessage sendMessage = metadata.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(sendMessage, ParseMode.HTML);
        assertNotNull(sendMessage.getText());
    }

}