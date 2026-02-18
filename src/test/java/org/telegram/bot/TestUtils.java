package org.telegram.bot;

import org.apache.commons.io.IOUtils;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.*;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.enums.RequestSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestUtils {

    public static final String BOT_USERNAME = "jtelebot";
    public static final Long DEFAULT_CHAT_ID = -1L;
    public static final Long DEFAULT_USER_ID = 1L;
    public static final Long ANOTHER_USER_ID = 2L;
    public static final Integer DEFAULT_MESSAGE_ID = 1;
    public static final Integer ANOTHER_MESSAGE_ID = 2;
    public static final String DEFAULT_MESSAGE_TEXT = "test";
    public static final String DEFAULT_FILE_ID = "fileId";
    public static final Integer DEFAULT_VOICE_DURATION = 17;

    public static BotRequest getRequestWithRepliedMessage(String textMessage) {
        Chat chat = new Chat();
        chat.setChatId(DEFAULT_CHAT_ID);

        User user = getTelegramUser(ANOTHER_USER_ID);

        Message repliedMessage = getMessage(ANOTHER_MESSAGE_ID, chat, user, textMessage);

        return getRequestWithRepliedMessage(repliedMessage);
    }

    public static BotRequest getRequestWithRepliedMessage(Message message) {
        BotRequest request = getRequestFromGroup();
        request.getMessage().setReplyToMessage(message);

        return request;
    }

    public static BotRequest getRequestWithVoice() {
        Attachment attachment = getAudio();

        BotRequest requestFromGroup = getRequestFromGroup();
        requestFromGroup.getMessage().setMessageContentType(MessageContentType.VOICE);
        requestFromGroup.getMessage().setAttachments(List.of(attachment));

        return requestFromGroup;
    }

    public static BotRequest getRequestFromGroup() {
        return getRequestFromGroup(DEFAULT_MESSAGE_TEXT);
    }

    public static BotRequest getRequestWithCallback(String callback) {
        Chat chat = new Chat();
        chat.setChatId(DEFAULT_CHAT_ID);

        User user = getTelegramUser();

        Message message = getMessage(chat, user);
        message.setMessageKind(MessageKind.CALLBACK);
        message.setText(callback);

        return new BotRequest().setMessage(message).setSource(RequestSource.TELEGRAM);
    }

    public static BotRequest getRequestFromGroup(String textMessage) {
        Chat chat = new Chat();
        chat.setChatId(DEFAULT_CHAT_ID);

        User user = getTelegramUser();

        Message message = getMessage(chat, user, textMessage);

        BotRequest botRequest = new BotRequest();
        botRequest.setMessage(message).setSource(RequestSource.TELEGRAM);

        return botRequest;
    }

    public static BotRequest getRequestFromPrivate(String textMessage) {
        Chat chat = new Chat();
        chat.setChatId(DEFAULT_USER_ID);

        User user = getTelegramUser();

        Message message = getMessage(chat, user, textMessage);

        BotRequest botRequest = new BotRequest();
        botRequest.setMessage(message).setSource(RequestSource.TELEGRAM);

        return botRequest;
    }

    public static Message getMessage() {
        Chat chat = new Chat();
        chat.setChatId(DEFAULT_CHAT_ID);

        User user = getTelegramUser();

        return getMessage(chat, user);
    }

    public static Message getMessage(Chat chat, User user) {
        return getMessage(chat, user, null);
    }

    public static Message getMessage(Chat chat, User user, String textMessage) {
        return getMessage(DEFAULT_MESSAGE_ID, chat, user, textMessage);
    }

    public static Message getMessage(Integer messageId, Chat chat, User user, String textMessage) {
        Message message = new Message();
        message.setMessageId(messageId);
        message.setChat(chat);
        message.setUser(user);
        message.setText(textMessage);

        return message;
    }

    public static org.telegram.bot.domain.entities.Chat getChat() {
        return getChat(DEFAULT_CHAT_ID);
    }

    public static org.telegram.bot.domain.entities.Chat getChat(Long chatId) {
        return new org.telegram.bot.domain.entities.Chat().setChatId(chatId).setAccessLevel(1);
    }

    public static User getTelegramUser() {
        return getTelegramUser(DEFAULT_USER_ID);
    }

    public static User getTelegramUser(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername("username");
        user.setLang("en");

        return user;
    }

    public static org.telegram.bot.domain.entities.User getUser() {
        return getUser(DEFAULT_USER_ID);
    }

    public static org.telegram.bot.domain.entities.User getUser(Long userId) {
        return new org.telegram.bot.domain.entities.User()
                .setUserId(userId)
                .setUsername("username")
                .setAccessLevel(1);
    }

    public static Attachment getDocument() {
        return new Attachment("mimeType", "uniqueId", DEFAULT_FILE_ID, null, "name", 1000L, null, null);
    }

    public static Attachment getDocument(String mimeType) {
        return new Attachment(mimeType, "uniqueId", DEFAULT_FILE_ID, null, "name", 1000L, null, null);
    }

    public static Attachment getDocument(String mimeType, Long size) {
        return new Attachment(mimeType, "uniqueId", DEFAULT_FILE_ID, null, "name", size, null, null);
    }

    public static Attachment getAudio() {
        return new Attachment("audio/ogg", "uniqueId", DEFAULT_FILE_ID, null, null, 20000L, DEFAULT_VOICE_DURATION, null);
    }

    public static TextResponse checkDefaultTextResponseParams(BotResponse botResponse, boolean disableWebPagePreview, FormattingStyle formattingStyle) {
        assertInstanceOf(TextResponse.class, botResponse);
        return checkDefaultTextResponseParams((TextResponse) botResponse, disableWebPagePreview, formattingStyle);
    }

    public static TextResponse checkDefaultTextResponseParams(TextResponse textResponse, boolean disableWebPagePreview, FormattingStyle formattingStyle) {
        ResponseSettings responseSettings = textResponse.getResponseSettings();
        assertNotEquals(disableWebPagePreview, responseSettings.getWebPagePreview());
        assertEquals(formattingStyle, responseSettings.getFormattingStyle());
        return checkDefaultTextResponseParams(textResponse);
    }

    public static TextResponse checkDefaultTextResponseParams(BotResponse response, FormattingStyle formattingStyle, boolean disableWebPagePreview, boolean hasKeyboard) {
        assertInstanceOf(TextResponse.class, response);
        return checkDefaultTextResponseParams((TextResponse) response, formattingStyle, disableWebPagePreview, hasKeyboard);
    }

    public static TextResponse checkDefaultTextResponseParams(TextResponse textResponse, boolean disableWebPagePreview) {
        assertEquals(disableWebPagePreview, textResponse.getResponseSettings().getWebPagePreview());
        return checkDefaultTextResponseParams(textResponse);
    }

    public static TextResponse checkDefaultTextResponseParams(TextResponse textResponse, FormattingStyle formattingStyle, boolean disableWebPagePreview, boolean hasKeyboard) {
        ResponseSettings responseSettings = textResponse.getResponseSettings();

        assertEquals(formattingStyle, responseSettings.getFormattingStyle());
        if (disableWebPagePreview) {
            assertEquals(Boolean.TRUE, responseSettings.getWebPagePreview());
        }
        if (hasKeyboard) {
            assertNotNull(textResponse.getKeyboard());
        }

        return checkDefaultTextResponseParams(textResponse);
    }

    public static TextResponse checkDefaultTextResponseParams(TextResponse textResponse, FormattingStyle formattingStyle) {
        assertEquals(formattingStyle, textResponse.getResponseSettings().getFormattingStyle());
        return checkDefaultTextResponseParams(textResponse);
    }

    public static TextResponse checkDefaultTextResponseParams(BotResponse response, FormattingStyle formattingStyle) {
        assertInstanceOf(TextResponse.class, response);
        return TestUtils.checkDefaultTextResponseParams((TextResponse) response, formattingStyle);
    }

    public static EditResponse checkDefaultEditResponseParams(BotResponse response) {
        assertInstanceOf(EditResponse.class, response);
        return checkDefaultEditResponseParams((EditResponse) response);
    }

    public static EditResponse checkDefaultEditResponseParams(BotResponse response, FormattingStyle formattingStyle, boolean disableWebPagePreview, boolean hasKeyboard) {
        assertInstanceOf(EditResponse.class, response);
        return checkDefaultEditResponseParams((EditResponse) response, formattingStyle, disableWebPagePreview, hasKeyboard);
    }

    public static EditResponse checkDefaultEditResponseParams(EditResponse editResponse, FormattingStyle formattingStyle, boolean disableWebPagePreview, boolean hasKeyboard) {
        ResponseSettings responseSettings = editResponse.getResponseSettings();

        assertEquals(formattingStyle, responseSettings.getFormattingStyle());
        if (disableWebPagePreview) {
            assertEquals(Boolean.TRUE, responseSettings.getWebPagePreview());
        }
        if (hasKeyboard) {
            assertNotNull(editResponse.getKeyboard());
        }
        return checkDefaultEditResponseParams(editResponse);
    }

    public static EditResponse checkDefaultEditResponseParams(EditResponse editResponse) {
        assertNotNull(editResponse);
        assertNotNull(editResponse.getChatId());
        assertNotNull(editResponse.getEditableMessageId());
        assertNotNull(editResponse.getText());

        return editResponse;
    }

    public static TextResponse checkDefaultTextResponseParams(BotResponse response) {
        assertInstanceOf(TextResponse.class, response);
        return checkDefaultTextResponseParams((TextResponse) response);
    }

    public static TextResponse checkDefaultTextResponseParams(TextResponse textResponse) {
        assertNotNull(textResponse);
        assertNotNull(textResponse.getChatId());
        assertNotNull(textResponse.getText());

        return textResponse;
    }

    public static FileResponse checkDefaultFileResponseParams(BotResponse response) {
        return checkDefaultFileResponseParams(response, FileType.FILE);
    }

    public static FileResponse checkDefaultFileResponseParams(BotResponse response, FileType fileType) {
        assertInstanceOf(FileResponse.class, response);

        FileResponse fileResponse = (FileResponse) response;
        assertFalse(fileResponse.getFiles().isEmpty());
        assertEquals(fileType, fileResponse.getFiles().getFirst().getFileType());

        return checkDefaultFileResponseParams(fileResponse);
    }

    public static FileResponse checkDefaultFileResponseParams(FileResponse fileResponse) {
        assertNotNull(fileResponse);
        assertNotNull(fileResponse.getChatId());

        return fileResponse;
    }

    public static FileResponse checkDefaultFileResponseImageParams(BotResponse response) {
        assertInstanceOf(FileResponse.class, response);

        FileResponse fileResponse = (FileResponse) response;
        assertFalse(fileResponse.getFiles().isEmpty());
        assertEquals(FileType.IMAGE, fileResponse.getFiles().getFirst().getFileType());
        return checkDefaultFileResponseImageParams(fileResponse);
    }

    public static FileResponse checkDefaultFileResponseImageParams(FileResponse fileResponse, boolean hasSpoiler) {
        assertEquals(hasSpoiler, fileResponse.getFiles().getFirst().getFileSettings().isSpoiler());
        return checkDefaultFileResponseImageParams(fileResponse);
    }

    public static FileResponse checkDefaultFileResponseImageParams(FileResponse fileResponse) {
        assertNotNull(fileResponse);
        assertNotNull(fileResponse.getChatId());
        assertNotNull(fileResponse.getReplyToMessageId());
        assertNotNull(fileResponse.getText());

        return fileResponse;
    }

    public static FileResponse checkDefaultFileResponseVideoParams(BotResponse response) {
        assertInstanceOf(FileResponse.class, response);

        FileResponse fileResponse = (FileResponse) response;
        assertFalse(fileResponse.getFiles().isEmpty());
        assertEquals(FileType.VIDEO, fileResponse.getFiles().getFirst().getFileType());

        return checkDefaultFileResponseVideoParams(fileResponse);
    }

    public static FileResponse checkDefaultFileResponseVideoParams(FileResponse fileResponse) {
        assertNotNull(fileResponse);
        assertNotNull(fileResponse.getChatId());
        assertNotNull(fileResponse.getReplyToMessageId());
        assertNotNull(fileResponse.getText());

        return fileResponse;
    }

    public static FileResponse checkDefaultResponseMultipleImagesParams(BotResponse response) {
        assertInstanceOf(FileResponse.class, response);

        FileResponse fileResponse = (FileResponse) response;
        assertFalse(fileResponse.getFiles().isEmpty());
        assertTrue(fileResponse.getFiles().size() > 1);
        assertEquals(FileType.IMAGE, fileResponse.getFiles().getFirst().getFileType());

        return checkDefaultResponseMultipleImagesParams(fileResponse);
    }

    public static FileResponse checkDefaultResponseMultipleImagesParams(FileResponse fileResponse) {
        assertNotNull(fileResponse);
        assertNotNull(fileResponse.getReplyToMessageId());
        assertNotNull(fileResponse.getChatId());

        return fileResponse;
    }

    public static LocationResponse checkDefaultLocationResponseParams(BotResponse botResponse) {
        assertInstanceOf(LocationResponse.class, botResponse);
        return (checkDefaultLocationResponseParams((LocationResponse) botResponse));
    }

    public static LocationResponse checkDefaultLocationResponseParams(LocationResponse locationResponse) {
        assertNotNull(locationResponse);
        assertNotNull(locationResponse.getChatId());
        assertNotNull(locationResponse.getReplyToMessageId());
        assertNotNull(locationResponse.getLatitude());
        assertNotNull(locationResponse.getLongitude());

        return locationResponse;
    }

    public static FileResponse checkDefaultFileResponseVoiceParams(BotResponse botResponse) {
        assertInstanceOf(FileResponse.class, botResponse);
        return checkDefaultFileResponseVoiceParams((FileResponse) botResponse);
    }

    public static FileResponse checkDefaultFileResponseVoiceParams(FileResponse fileResponse) {
        assertFalse(fileResponse.getFiles().isEmpty());
        assertEquals(FileType.VOICE, fileResponse.getFiles().getFirst().getFileType());

        assertNotNull(fileResponse);
        assertNotNull(fileResponse.getChatId());
        assertNotNull(fileResponse.getReplyToMessageId());

        return fileResponse;
    }

    public static DeleteResponse checkDefaultDeleteResponseParams(BotResponse botResponse) {
        assertInstanceOf(DeleteResponse.class, botResponse);
        return checkDefaultDeleteResponseParams((DeleteResponse) botResponse);
    }

    public static DeleteResponse checkDefaultDeleteResponseParams(DeleteResponse deleteResponse) {
        assertNotNull(deleteResponse.getChatId());
        assertNotNull(deleteResponse.getMessageId());

        return deleteResponse;
    }

    public static String getResourceAsString(String path) throws IOException {
        return IOUtils.toString(new FileInputStream("src/test/resources/" + path), StandardCharsets.UTF_8);
    }

    public static byte[] getResourceAsBytes(String path) throws IOException {
        return IOUtils.toByteArray(new FileInputStream("src/test/resources/" + path));
    }

}
