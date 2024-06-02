package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.File;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageContentType;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.FileService;
import org.telegram.bot.services.SpeechService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilesTest {

    @Mock
    private Bot bot;
    @Mock
    private BotStats botStats;
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
        BotRequest request = TestUtils.getRequestFromGroup("files test");
        assertThrows(BotException.class, () -> files.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void emptyCommandWithUnknownRootDirectoryTest() {
        BotRequest request = TestUtils.getRequestFromGroup();

        when(fileService.get(ROOT_DIR_ID)).thenReturn(null);

        assertThrows(BotException.class, () -> files.parse(request));

        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void emptyCommandRootDirTest() {
        final String dirName = "root";
        final String fileName = "testtesttesttesttesttesttesttest";
        final String fileType = "text";
        BotRequest request = TestUtils.getRequestFromGroup();

        when(fileService.get(anyLong())).thenReturn(new File().setId(ROOT_DIR_ID).setName(dirName));
        when(fileService.get(any(Chat.class), any(File.class), anyInt()))
                .thenReturn(
                        new PageImpl<>(
                                List.of(new File().setId(1L).setType(fileType).setName(fileName)),
                                PageRequest.of(1, 10),
                                100));

        BotResponse response = files.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response, FormattingStyle.HTML, false, true);
        assertTrue(textResponse.getText().contains(dirName));

        Keyboard keyboard = textResponse.getKeyboard();
        List<List<KeyboardButton>> buttons = keyboard.getKeyboardButtonsList();

        List<KeyboardButton> filesButtons = buttons.get(0);
        assertEquals(1, filesButtons.size());

        KeyboardButton fileButton = filesButtons.get(0);
        assertTrue(fileButton.getName().length() <= 30);
        assertNotNull(fileButton.getCallback());

        List<KeyboardButton> pagesButtons = buttons.get(1);
        assertEquals(1, pagesButtons.size());

        KeyboardButton forwardButton = pagesButtons.get(0);
        assertEquals(Emoji.RIGHT_ARROW.getSymbol(), forwardButton.getName());
        assertNotNull(forwardButton.getCallback());
    }

    @Test
    void emptyCallbackCommandTest() {
        final String dirName = "root";
        final String fileName = "testtesttesttesttesttesttesttest";
        final String fileType = "text";
        BotRequest request = TestUtils.getRequestWithCallback("files files");

        when(fileService.get(anyLong())).thenReturn(new File().setId(ROOT_DIR_ID).setName(dirName));

        when(fileService.get(anyLong())).thenReturn(new File().setId(ROOT_DIR_ID).setName(dirName));
        when(fileService.get(any(Chat.class), any(File.class), anyInt()))
                .thenReturn(
                        new PageImpl<>(
                                List.of(new File().setId(1L).setType(fileType).setName(fileName)),
                                PageRequest.of(1, 10),
                                100));

        BotResponse response = files.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response, FormattingStyle.HTML, false, true);
        assertTrue(editResponse.getText().contains(dirName));

        Keyboard keyboard = editResponse.getKeyboard();
        List<List<KeyboardButton>> buttons = keyboard.getKeyboardButtonsList();

        List<KeyboardButton> filesButtons = buttons.get(0);
        assertEquals(1, filesButtons.size());

        KeyboardButton fileButton = filesButtons.get(0);
        assertTrue(fileButton.getName().length() <= 30);
        assertNotNull(fileButton.getCallback());

        List<KeyboardButton> pagesButtons = buttons.get(1);
        assertEquals(1, pagesButtons.size());

        KeyboardButton forwardButton = pagesButtons.get(0);
        assertEquals(Emoji.RIGHT_ARROW.getSymbol(), forwardButton.getName());
        assertNotNull(forwardButton.getCallback());
    }

    @Test
    void unknownCalllBackCommandTest() {
        final String unknownCallbackCommand = "tratatam-tratatam";
        BotRequest request = TestUtils.getRequestWithCallback("files " + unknownCallbackCommand);

        assertThrows(BotException.class, () -> files.parse(request));

        verify(botStats).incrementErrors(request, "Unexpected callback request " + unknownCallbackCommand);
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void emptyCommandNotRootEmptyDirTest() {
        final String dirName = "вшк";
        BotRequest request = TestUtils.getRequestFromGroup();

        when(fileService.get(anyLong())).thenReturn(new File().setId(1L).setName(dirName));
        when(fileService.get(any(Chat.class), any(File.class), anyInt())).thenReturn(new PageImpl<>(new ArrayList<>()));

        BotResponse response = files.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response, FormattingStyle.HTML, false, true);
        assertTrue(textResponse.getText().contains(dirName));
        
        List<List<KeyboardButton>> keyboard = textResponse.getKeyboard().getKeyboardButtonsList();

        List<KeyboardButton> buttons = keyboard.get(0);
        assertEquals(1, buttons.size());

        KeyboardButton deleteButton = buttons.get(0);
        assertTrue(deleteButton.getName().contains(Emoji.DELETE.getSymbol()));
        assertNotNull(deleteButton.getCallback());
    }

    @Test
    void selectFileWitCorruptedFileIdTest() {
        BotRequest request = TestUtils.getRequestWithCallback("files stest test");

        assertThrows(BotException.class, () -> files.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void selectFileWithNotExistanseFileIdAndPageTest() {
        BotRequest request = TestUtils.getRequestWithCallback("files s1 p1");

        assertThrows(BotException.class, () -> files.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void selectFileTest() {
        BotRequest request = TestUtils.getRequestWithCallback("files s1");

        when(fileService.get(anyLong())).thenReturn(getFile());

        BotResponse response = files.parse(request).get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response, FormattingStyle.HTML, false, true);

        TestUtils.checkDefaultEditResponseParams(editResponse);
    }

    @Test
    void selectDirWithNextPrevButtonsTest() {
        BotRequest request = TestUtils.getRequestWithCallback("files s1 p3");

        when(fileService.get(anyLong())).thenReturn(new File().setId(ROOT_DIR_ID).setName("root"));
        when(fileService.get(any(Chat.class), any(File.class), anyInt()))
                .thenReturn(
                        new PageImpl<>(
                                List.of(new File().setId(1L).setType("text").setName("testtesttesttesttesttesttesttest")),
                                PageRequest.of(3, 10),
                                100));

        BotResponse response = files.parse(request).get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response, FormattingStyle.HTML, false, true);

        List<List<KeyboardButton>> buttons = editResponse.getKeyboard().getKeyboardButtonsList();

        List<KeyboardButton> pagesButtons = buttons.get(1);
        assertEquals(2, pagesButtons.size());

        KeyboardButton backButton = pagesButtons.get(0);
        //assertEquals(Emoji.LEFT_ARROW.getEmoji(), backButton.getText());
        assertNotNull(backButton.getCallback());

        KeyboardButton forwardButton = pagesButtons.get(1);
        //assertEquals(Emoji.RIGHT_ARROW.getEmoji(), forwardButton.getText());
        assertNotNull(forwardButton.getCallback());
    }

    @Test
    void deleteFileWithCorruptedCommandTest() {
        BotRequest request = TestUtils.getRequestWithCallback("files dtest");

        assertThrows(BotException.class, () -> files.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void deleteNotExistenceFileTest() {
        BotRequest request = TestUtils.getRequestWithCallback("files d1");

        when(fileService.get(anyLong())).thenReturn(null);

        assertThrows(BotException.class, () -> files.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void deleteSomeOneElsesFileTest() {
        BotRequest request = TestUtils.getRequestWithCallback("files d1");

        when(fileService.get(anyLong())).thenReturn(getFile().setUser(new User().setUserId(2L)));

        assertThrows(BotException.class, () -> files.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NOT_OWNER);
    }

    @Test
    void deleteFileWithoutDirTest() {
        BotRequest request = TestUtils.getRequestWithCallback("files d1");

        when(fileService.get(anyLong())).thenReturn(getFile());

        assertThrows(BotException.class, () -> files.parse(request));

        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(fileService).remove(any(Chat.class), any(File.class));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void deleteFileTest() {
        BotRequest request = TestUtils.getRequestWithCallback("files d1");

        when(fileService.get(anyLong())).thenReturn(getFile().setParentId(1L));
        when(fileService.get(any(Chat.class), any(File.class), anyInt())).thenReturn(new PageImpl<>(new ArrayList<>()));

        BotResponse response = files.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(fileService).remove(any(Chat.class), any(File.class));

        TestUtils.checkDefaultEditResponseParams(response, FormattingStyle.HTML, false, true);
    }

    @Test
    void addCommandByCallbackTest() {
        BotRequest request = TestUtils.getRequestWithCallback("files a");
        assertDoesNotThrow(() -> files.parse(request));
        verify(commandWaitingService).add(any(Chat.class), any(User.class), any(Class.class), anyString());
    }

    @Test
    void makeDirByCallbackTest() {
        BotRequest request = TestUtils.getRequestWithCallback("files m");
        assertDoesNotThrow(() -> files.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(commandWaitingService).add(any(Chat.class), any(User.class), any(Class.class), anyString());
    }

    @Test
    void sendFileWithCorruptedFileIdTest() {
        BotRequest request = TestUtils.getRequestWithCallback("files otest");
        assertThrows(BotException.class, () -> files.parse(request));
        verify(bot).sendUploadDocument(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void sendFileNotExistenceFileTest() {
        BotRequest request = TestUtils.getRequestWithCallback("files o1");

        when(fileService.get(anyLong())).thenReturn(null);

        assertThrows(BotException.class, () -> files.parse(request));
        verify(bot).sendUploadDocument(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void sendFileFromAnotherChatTest() {
        BotRequest request = TestUtils.getRequestWithCallback("files o1");

        when(fileService.get(anyLong())).thenReturn(getFile().setChat(new Chat().setChatId(2L)));

        assertThrows(BotException.class, () -> files.parse(request));
        verify(bot).sendUploadDocument(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NOT_OWNER);
    }

    @Test
    void sendFileTest() {
        BotRequest request = TestUtils.getRequestWithCallback("files o1");
        when(fileService.get(anyLong())).thenReturn(getFile());
        assertDoesNotThrow(() -> files.parse(request));
        verify(bot).sendUploadDocument(request.getMessage().getChatId());
    }

    @Test
    void addFilesWithoutDocumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup();

        when(commandWaitingService.get(any(Chat.class), any(User.class)))
                .thenReturn(new CommandWaiting().setTextMessage("files a"));

        List<BotResponse> botResponses = files.parse(request);
        assertTrue(botResponses.isEmpty());
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(commandWaitingService).remove(any(CommandWaiting.class));
    }

    @Test
    void addAudioFilesWithWrongCommand() {
        BotRequest request = TestUtils.getRequestFromGroup();
        Message message = request.getMessage();
        message.setMessageContentType(MessageContentType.AUDIO);

        when(commandWaitingService.get(any(Chat.class), any(User.class)))
                .thenReturn(new CommandWaiting().setTextMessage("files atest"));

        assertThrows(BotException.class, () -> files.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(commandWaitingService).remove(any(CommandWaiting.class));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void addDocumentFilesWithoutParent() {
        BotRequest request = TestUtils.getRequestFromGroup("");
        Message message = request.getMessage();
        message.setMessageContentType(MessageContentType.FILE);

        when(commandWaitingService.get(any(Chat.class), any(User.class)))
                .thenReturn(new CommandWaiting().setTextMessage("files a1"));
        when(fileService.get(anyLong())).thenReturn(null);

        assertThrows(BotException.class, () -> files.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(commandWaitingService).remove(any(CommandWaiting.class));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void addDocumentFilesTest() {
        final Attachment attachment = TestUtils.getDocument();
        File dir = getFile();
        BotRequest request = TestUtils.getRequestFromGroup("");
        Message message = request.getMessage();
        message.setAttachments(List.of(attachment));
        message.setMessageContentType(MessageContentType.FILE);

        ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
        when(commandWaitingService.get(any(Chat.class), any(User.class)))
                .thenReturn(new CommandWaiting().setTextMessage("files a1"));
        when(fileService.get(anyLong())).thenReturn(dir);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn("saved");

        BotResponse response = files.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TestUtils.checkDefaultTextResponseParams(response);

        verify(fileService).save(captor.capture());
        File file = captor.getValue();

        assertEquals(attachment.getFileId(), file.getFileId());
        assertEquals(attachment.getFileUniqueId(), file.getFileUniqueId());
        assertEquals(attachment.getName(), file.getName());
        assertEquals(attachment.getMimeType(), file.getType());
        assertEquals(attachment.getSize(), file.getSize());
        assertEquals(message.getChatId(), file.getChat().getChatId());
        assertEquals(message.getUser().getUserId(), file.getUser().getUserId());
        assertNotNull(file.getDate());
        assertEquals(dir.getId(), file.getParentId());
    }

    @Test
    void addAudioFilesTest() {
        final Attachment audio = TestUtils.getAudio();
        File dir = getFile();
        BotRequest request = TestUtils.getRequestFromGroup("");
        Message message = request.getMessage();
        message.setAttachments(List.of(audio));
        message.setMessageContentType(MessageContentType.AUDIO);

        ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
        when(commandWaitingService.get(any(Chat.class), any(User.class)))
                .thenReturn(new CommandWaiting().setTextMessage("files a1"));
        when(fileService.get(anyLong())).thenReturn(dir);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn("saved");

        BotResponse response = files.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TestUtils.checkDefaultTextResponseParams(response);

        verify(fileService).save(captor.capture());
        File file = captor.getValue();

        assertEquals(audio.getFileId(), file.getFileId());
        assertEquals(audio.getFileUniqueId(), file.getFileUniqueId());
        assertEquals(audio.getName(), file.getName());
        assertEquals(audio.getMimeType(), file.getType());
        assertEquals(audio.getSize(), file.getSize());
        assertEquals(message.getChatId(), file.getChat().getChatId());
        assertEquals(message.getUser().getUserId(), file.getUser().getUserId());
        assertNotNull(file.getDate());
        assertEquals(dir.getId(), file.getParentId());
    }

    @Test
    void makeDirForWrongParentIdTest() {
        BotRequest request = TestUtils.getRequestFromGroup("files mtest test");
        assertThrows(BotException.class, () -> files.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void makeDirForNotExistenceParentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("files m1 test");

        when(fileService.get(anyLong())).thenReturn(null);

        assertThrows(BotException.class, () -> files.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void makeDirTest() {
        final String dirName = "test";
        final File parentFile = getFile();
        BotRequest request = TestUtils.getRequestFromGroup("files m1 " + dirName);
        ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);

        when(fileService.get(anyLong())).thenReturn(parentFile);
        when(fileService.get(any(Chat.class), any(File.class), anyInt())).thenReturn(new PageImpl<>(new ArrayList<>()));

        BotResponse response = files.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TestUtils.checkDefaultTextResponseParams(response);

        verify(fileService).save(captor.capture());
        File file = captor.getValue();

        Message message = request.getMessage();
        assertEquals(dirName, file.getName());
        assertEquals(message.getChatId(), file.getChat().getChatId());
        assertEquals(message.getUser().getUserId(), file.getUser().getUserId());
        assertNotNull(file.getDate());
        assertEquals(parentFile.getId(), file.getParentId());
    }

    //does not work. Possibly because of the emoji symbol
    @ParameterizedTest
    @MethodSource("provideTypes")
    void mimeTypeEmojisTest(String type, String emoji) {
        String emojiByType = Files.EmojiMimeType.getEmojiByType(type);
        assertEquals(emoji, emojiByType);
    }

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

    private static Stream<Arguments> provideTypes() {
        return Stream.of(
                Arguments.of(null, Emoji.FOLDER.getSymbol()),
                Arguments.of("audio", Emoji.HEADPHONE.getSymbol()),
                Arguments.of("image", Emoji.PICTURE.getSymbol()),
                Arguments.of("text", Emoji.CLIPBOARD.getSymbol()),
                Arguments.of("video", Emoji.MOVIE_CAMERA.getSymbol()),
                Arguments.of("test", Emoji.MEMO.getSymbol())
        );
    }

}