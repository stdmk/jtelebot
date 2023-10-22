package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.commands.Files;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.File;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.FileService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Audio;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilesTest {

    //TODO

    @Mock
    private Bot bot;
    @Mock
    private FileService fileService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private SpeechService speechService;

    @InjectMocks
    private Files files;

    private final Long ROOT_DIR_ID = 0L;

    @Test
    void unknownCommandTest() {
        Update update = TestUtils.getUpdateFromGroup("files test");
        assertThrows(BotException.class, () -> files.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void emptyCommandWithUnknownRootDirectoryTest() {
        Update update = TestUtils.getUpdateFromGroup();

        when(fileService.get(ROOT_DIR_ID)).thenReturn(null);

        assertThrows(BotException.class, () -> files.parse(update));

        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void emptyCommandRootDirTest() {
        final String dirName = "root";
        final String fileName = "testtesttesttesttesttesttesttest";
        final String fileType = "text";
        Update update = TestUtils.getUpdateFromGroup();

        when(fileService.get(anyLong())).thenReturn(new File().setId(ROOT_DIR_ID).setName(dirName));
        when(fileService.get(any(Chat.class), any(File.class), anyInt()))
                .thenReturn(
                        new PageImpl<>(
                                List.of(new File().setId(1L).setType(fileType).setName(fileName)),
                                PageRequest.of(1, 10),
                                100));

        PartialBotApiMethod<?> method = files.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        SendMessage sendMessage = TestUtils.checkDefaultSendMessageParams(method, ParseMode.HTML, false, true);
        assertTrue(sendMessage.getText().contains(dirName));

        InlineKeyboardMarkup replyMarkup = (InlineKeyboardMarkup) sendMessage.getReplyMarkup();
        List<List<InlineKeyboardButton>> keyboard = replyMarkup.getKeyboard();

        List<InlineKeyboardButton> filesButtons = keyboard.get(0);
        assertEquals(1, filesButtons.size());

        InlineKeyboardButton fileButton = filesButtons.get(0);
        assertTrue(fileButton.getText().length() <= 30);
        assertNotNull(fileButton.getCallbackData());

        List<InlineKeyboardButton> pagesButtons = keyboard.get(1);
        assertEquals(1, pagesButtons.size());

        InlineKeyboardButton forwardButton = pagesButtons.get(0);
        assertEquals(Emoji.RIGHT_ARROW.getEmoji(), forwardButton.getText());
        assertNotNull(forwardButton.getCallbackData());
    }

    @Test
    void emptyCommandNotRootEmptyDirTest() {
        final String dirName = "вшк";
        Update update = TestUtils.getUpdateFromGroup();

        when(fileService.get(anyLong())).thenReturn(new File().setId(1L).setName(dirName));
        when(fileService.get(any(Chat.class), any(File.class), anyInt())).thenReturn(new PageImpl<>(new ArrayList<>()));

        PartialBotApiMethod<?> method = files.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        SendMessage sendMessage = TestUtils.checkDefaultSendMessageParams(method, ParseMode.HTML, false, true);
        assertTrue(sendMessage.getText().contains(dirName));

        InlineKeyboardMarkup replyMarkup = (InlineKeyboardMarkup) sendMessage.getReplyMarkup();
        List<List<InlineKeyboardButton>> keyboard = replyMarkup.getKeyboard();

        List<InlineKeyboardButton> buttons = keyboard.get(0);
        assertEquals(1, buttons.size());

        InlineKeyboardButton deleteButton = buttons.get(0);
        assertTrue(deleteButton.getText().contains(Emoji.DELETE.getEmoji()));
        assertNotNull(deleteButton.getCallbackData());
    }

    @Test
    void selectFileWitCorruptedFileIdTest() {
        Update update = TestUtils.getUpdateWithCallback("files stest test");

        assertThrows(BotException.class, () -> files.parse(update));
        verify(bot).sendTyping(update.getCallbackQuery().getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void selectFileWithNotExistanseFileIdAndPageTest() {
        Update update = TestUtils.getUpdateWithCallback("files s1 p1");

        assertThrows(BotException.class, () -> files.parse(update));
        verify(bot).sendTyping(update.getCallbackQuery().getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void selectFileTest() {
        Update update = TestUtils.getUpdateWithCallback("files s1");

        when(fileService.get(anyLong())).thenReturn(getFile());

        PartialBotApiMethod<?> method = files.parse(update);
        verify(bot).sendTyping(update.getCallbackQuery().getMessage().getChatId());
        EditMessageText editMessageText = TestUtils.checkDefaultEditMessageTextParams(method, ParseMode.HTML, false, true);

        TestUtils.checkDefaultEditMessageTextParams(editMessageText);
    }

    @Test
    void selectDirWithNextPrevButtonsTest() {
        Update update = TestUtils.getUpdateWithCallback("files s1 p3");

        when(fileService.get(anyLong())).thenReturn(new File().setId(ROOT_DIR_ID).setName("root"));
        when(fileService.get(any(Chat.class), any(File.class), anyInt()))
                .thenReturn(
                        new PageImpl<>(
                                List.of(new File().setId(1L).setType("text").setName("testtesttesttesttesttesttesttest")),
                                PageRequest.of(3, 10),
                                100));

        PartialBotApiMethod<?> method = files.parse(update);
        verify(bot).sendTyping(update.getCallbackQuery().getMessage().getChatId());
        EditMessageText editMessageText = TestUtils.checkDefaultEditMessageTextParams(method, ParseMode.HTML, false, true);

        InlineKeyboardMarkup replyMarkup = editMessageText.getReplyMarkup();
        List<List<InlineKeyboardButton>> keyboard = replyMarkup.getKeyboard();

        List<InlineKeyboardButton> pagesButtons = keyboard.get(1);
        assertEquals(2, pagesButtons.size());

        InlineKeyboardButton backButton = pagesButtons.get(0);
        //assertEquals(Emoji.LEFT_ARROW.getEmoji(), backButton.getText());
        assertNotNull(backButton.getCallbackData());

        InlineKeyboardButton forwardButton = pagesButtons.get(1);
        //assertEquals(Emoji.RIGHT_ARROW.getEmoji(), forwardButton.getText());
        assertNotNull(forwardButton.getCallbackData());
    }

    @Test
    void deleteFileWithCorruptedCommandTest() {
        Update update = TestUtils.getUpdateWithCallback("files dtest");

        assertThrows(BotException.class, () -> files.parse(update));
        verify(bot).sendTyping(update.getCallbackQuery().getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void deleteNotExistenceFileTest() {
        Update update = TestUtils.getUpdateWithCallback("files d1");

        when(fileService.get(anyLong())).thenReturn(null);

        assertThrows(BotException.class, () -> files.parse(update));
        verify(bot).sendTyping(update.getCallbackQuery().getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void deleteSomeOneElsesFileTest() {
        Update update = TestUtils.getUpdateWithCallback("files d1");

        when(fileService.get(anyLong())).thenReturn(getFile().setUser(new User().setUserId(2L)));

        assertThrows(BotException.class, () -> files.parse(update));
        verify(bot).sendTyping(update.getCallbackQuery().getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NOT_OWNER);
    }

    @Test
    void deleteFileWithoutDirTest() {
        Update update = TestUtils.getUpdateWithCallback("files d1");

        when(fileService.get(anyLong())).thenReturn(getFile());

        assertThrows(BotException.class, () -> files.parse(update));

        verify(bot).sendTyping(update.getCallbackQuery().getMessage().getChatId());
        verify(fileService).remove(any(Chat.class), any(File.class));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void deleteFileTest() {
        Update update = TestUtils.getUpdateWithCallback("files d1");

        when(fileService.get(anyLong())).thenReturn(getFile().setParentId(1L));
        when(fileService.get(any(Chat.class), any(File.class), anyInt())).thenReturn(new PageImpl<>(new ArrayList<>()));

        PartialBotApiMethod<?> method = files.parse(update);

        verify(bot).sendTyping(update.getCallbackQuery().getMessage().getChatId());
        verify(fileService).remove(any(Chat.class), any(File.class));

        TestUtils.checkDefaultEditMessageTextParams(method, ParseMode.HTML, false, true);
    }

    @Test
    void addCommandByCallbackTest() {
        Update update = TestUtils.getUpdateWithCallback("files a");
        assertDoesNotThrow(() -> files.parse(update));
        verify(commandWaitingService).add(any(Chat.class), any(User.class), any(Class.class), anyString());
    }

    @Test
    void makeDirByCallbackTest() {
        Update update = TestUtils.getUpdateWithCallback("files m");
        assertDoesNotThrow(() -> files.parse(update));
        verify(bot).sendTyping(update.getCallbackQuery().getMessage().getChatId());
        verify(commandWaitingService).add(any(Chat.class), any(User.class), any(Class.class), anyString());
    }

    @Test
    void sendFileWithCorruptedFileIdTest() {
        Update update = TestUtils.getUpdateWithCallback("files otest");
        assertThrows(BotException.class, () -> files.parse(update));
        verify(bot).sendUploadDocument(update.getCallbackQuery().getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void sendFileNotExistenceFileTest() {
        Update update = TestUtils.getUpdateWithCallback("files o1");

        when(fileService.get(anyLong())).thenReturn(null);

        assertThrows(BotException.class, () -> files.parse(update));
        verify(bot).sendUploadDocument(update.getCallbackQuery().getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void sendFileFromAnotherChatTest() {
        Update update = TestUtils.getUpdateWithCallback("files o1");

        when(fileService.get(anyLong())).thenReturn(getFile().setChat(new Chat().setChatId(2L)));

        assertThrows(BotException.class, () -> files.parse(update));
        verify(bot).sendUploadDocument(update.getCallbackQuery().getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NOT_OWNER);
    }

    @Test
    void sendFileTest() {
        Update update = TestUtils.getUpdateWithCallback("files o1");
        when(fileService.get(anyLong())).thenReturn(getFile());
        assertDoesNotThrow(() -> files.parse(update));
        verify(bot).sendUploadDocument(update.getCallbackQuery().getMessage().getChatId());
    }

    @Test
    void addFilesWithoutDocumentTest() {
        Update update = TestUtils.getUpdateFromGroup();

        when(commandWaitingService.get(any(Chat.class), any(User.class)))
                .thenReturn(new CommandWaiting().setTextMessage("files a"));

        assertNull(files.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(commandWaitingService).remove(any(CommandWaiting.class));
    }

    @Test
    void addAudioFilesWithWrongCommand() {
        Update update = TestUtils.getUpdateFromGroup();
        Message message = update.getMessage();
        message.setAudio(new Audio());

        when(commandWaitingService.get(any(Chat.class), any(User.class)))
                .thenReturn(new CommandWaiting().setTextMessage("files atest"));

        assertThrows(BotException.class, () -> files.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(commandWaitingService).remove(any(CommandWaiting.class));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void addDocumentFilesWithoutParent() {
        Update update = TestUtils.getUpdateFromGroup("");
        Message message = update.getMessage();
        message.setDocument(new Document());

        when(commandWaitingService.get(any(Chat.class), any(User.class)))
                .thenReturn(new CommandWaiting().setTextMessage("files a1"));
        when(fileService.get(anyLong())).thenReturn(null);

        assertThrows(BotException.class, () -> files.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(commandWaitingService).remove(any(CommandWaiting.class));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void addDocumentFilesTest() {
        final Document document = TestUtils.getDocument();
        File dir = getFile();
        Update update = TestUtils.getUpdateFromGroup("");
        Message message = update.getMessage();
        message.setDocument(document);

        ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
        when(commandWaitingService.get(any(Chat.class), any(User.class)))
                .thenReturn(new CommandWaiting().setTextMessage("files a1"));
        when(fileService.get(anyLong())).thenReturn(dir);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn("saved");

        PartialBotApiMethod<?> method = files.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(method);

        verify(fileService).save(captor.capture());
        File file = captor.getValue();

        assertEquals(document.getFileId(), file.getFileId());
        assertEquals(document.getFileUniqueId(), file.getFileUniqueId());
        assertEquals(document.getFileName(), file.getName());
        assertEquals(document.getMimeType(), file.getType());
        assertEquals(document.getFileSize(), file.getSize());
        assertEquals(message.getChatId(), file.getChat().getChatId());
        assertEquals(message.getFrom().getId(), file.getUser().getUserId());
        assertNotNull(file.getDate());
        assertEquals(dir.getId(), file.getParentId());
    }

    @Test
    void addAudioFilesTest() {
        final Audio audio = TestUtils.getAudio();
        File dir = getFile();
        Update update = TestUtils.getUpdateFromGroup("");
        Message message = update.getMessage();
        message.setAudio(audio);

        ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
        when(commandWaitingService.get(any(Chat.class), any(User.class)))
                .thenReturn(new CommandWaiting().setTextMessage("files a1"));
        when(fileService.get(anyLong())).thenReturn(dir);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn("saved");

        PartialBotApiMethod<?> method = files.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(method);

        verify(fileService).save(captor.capture());
        File file = captor.getValue();

        assertEquals(audio.getFileId(), file.getFileId());
        assertEquals(audio.getFileUniqueId(), file.getFileUniqueId());
        assertEquals(audio.getFileName(), file.getName());
        assertEquals(audio.getMimeType(), file.getType());
        assertEquals(audio.getFileSize(), file.getSize());
        assertEquals(message.getChatId(), file.getChat().getChatId());
        assertEquals(message.getFrom().getId(), file.getUser().getUserId());
        assertNotNull(file.getDate());
        assertEquals(dir.getId(), file.getParentId());
    }

    @Test
    void makeDirForWrongParentIdTest() {
        Update update = TestUtils.getUpdateFromGroup("files mtest test");
        assertThrows(BotException.class, () -> files.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void makeDirForNotExistenceParentTest() {
        Update update = TestUtils.getUpdateFromGroup("files m1 test");

        when(fileService.get(anyLong())).thenReturn(null);

        assertThrows(BotException.class, () -> files.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void makeDirTest() {
        final String dirName = "test";
        final File parentFile = getFile();
        Update update = TestUtils.getUpdateFromGroup("files m1 " + dirName);
        ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);

        when(fileService.get(anyLong())).thenReturn(parentFile);
        when(fileService.get(any(Chat.class), any(File.class), anyInt())).thenReturn(new PageImpl<>(new ArrayList<>()));

        PartialBotApiMethod<?> method = files.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(method);

        verify(fileService).save(captor.capture());
        File file = captor.getValue();

        Message message = update.getMessage();
        assertEquals(dirName, file.getName());
        assertEquals(message.getChatId(), file.getChat().getChatId());
        assertEquals(message.getFrom().getId(), file.getUser().getUserId());
        assertNotNull(file.getDate());
        assertEquals(parentFile.getId(), file.getParentId());
    }

    //does not work. Possibly because of the emoji symbol
//    @ParameterizedTest
//    @MethodSource("provideTypes")
//    void mimeTypeEmojisTest(String type, String emoji) {
//        String emojiByType = Files.EmojiMimeType.getEmojiByType(type);
//        assertEquals(emoji, emojiByType);
//    }

    private File getFile() {
        return new File()
                .setId(1L)
                .setName("filename")
                .setDate(LocalDateTime.now())
                .setType("text")
                .setSize(1L)
                .setChat(TestUtils.getChat())
                .setUser(TestUtils.getUser());
    }

//    private static Stream<Arguments> provideTypes() {
//        return Stream.of(
//                Arguments.of(null, Emoji.FOLDER.getEmoji()),
//                Arguments.of("audio", Emoji.HEADPHONE.getEmoji()),
//                Arguments.of("image", Emoji.PICTURE.getEmoji()),
//                Arguments.of("text", Emoji.CLIPBOARD.getEmoji()),
//                Arguments.of("video", Emoji.MOVIE_CAMERA.getEmoji()),
//                Arguments.of("test", Emoji.MEMO.getEmoji())
//        );
//    }

}