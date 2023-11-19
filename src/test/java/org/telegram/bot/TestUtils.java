package org.telegram.bot;

import org.apache.commons.io.IOUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class TestUtils {

    public static final String BOT_USERNAME = "jtelebot";
    public static final Long DEFAULT_CHAT_ID = -1L;
    public static final Long DEFAULT_USER_ID = 1L;
    public static final Long ANOTHER_USER_ID = 2L;
    public static final Integer DEFAULT_MESSAGE_ID = 1;
    public static final Integer ANOTHER_MESSAGE_ID = 2;
    public static final String DEFAULT_MESSAGE_TEXT = "test";

    public static Update getUpdateWithRepliedMessage(String textMessage) {
        Chat chat = new Chat();
        chat.setId(DEFAULT_CHAT_ID);

        User user = getTelegramUser(ANOTHER_USER_ID);

        Message repliedMessage = getMessage(ANOTHER_MESSAGE_ID, chat, user, textMessage);

        return getUpdateWithRepliedMessage(repliedMessage);
    }

    public static Update getUpdateWithRepliedMessage(Message message) {
        Update update = getUpdateFromGroup();
        update.getMessage().setReplyToMessage(message);

        return update;
    }

    public static Update getUpdateFromGroup() {
        return getUpdateFromGroup(DEFAULT_MESSAGE_TEXT);
    }

    public static Update getUpdateWithCallback(String callback) {
        Chat chat = new Chat();
        chat.setId(DEFAULT_CHAT_ID);

        User user = getTelegramUser();

        Message message = getMessage(chat, user);

        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setFrom(user);
        callbackQuery.setMessage(message);
        callbackQuery.setData(callback);

        Update update = new Update();
        update.setCallbackQuery(callbackQuery);

        return update;
    }

    public static Update getUpdateFromGroup(String textMessage) {
        Chat chat = new Chat();
        chat.setId(DEFAULT_CHAT_ID);

        User user = getTelegramUser();

        Message message = getMessage(chat, user, textMessage);

        Update update = new Update();
        update.setMessage(message);

        return update;
    }

    public static Update getUpdateFromPrivate(String textMessage) {
        Chat chat = new Chat();
        chat.setId(DEFAULT_USER_ID);

        User user = getTelegramUser();

        Message message = getMessage(chat, user, textMessage);

        Update update = new Update();
        update.setMessage(message);

        return update;
    }

    public static Message getMessage() {
        Chat chat = new Chat();
        chat.setId(DEFAULT_CHAT_ID);

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
        message.setFrom(user);
        message.setText(textMessage);

        return message;
    }

    public static org.telegram.bot.domain.entities.Chat getChat() {
        return getChat(DEFAULT_CHAT_ID);
    }

    public static org.telegram.bot.domain.entities.Chat getChat(Long chatId) {
        return new org.telegram.bot.domain.entities.Chat().setChatId(chatId);
    }

    public static User getTelegramUser() {
        return getTelegramUser(DEFAULT_USER_ID);
    }

    public static User getTelegramUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setLanguageCode("en");

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

    public static Document getDocument() {
        Document document = new Document();

        document.setFileId("fileId");
        document.setFileUniqueId("fileUniqueId");
        document.setMimeType("mimeType");
        document.setFileSize(1000L);

        return document;
    }

    public static Audio getAudio() {
        Audio audio = new Audio();

        audio.setFileId("fileId");
        audio.setFileUniqueId("fileUniqueId");
        audio.setMimeType("mimeType");
        audio.setFileSize(1000L);

        return audio;
    }

    public static SendMessage checkDefaultSendMessageParams(SendMessage sendMessage, boolean disableWebPagePreview, String parseMode) {
        assertEquals(disableWebPagePreview, sendMessage.getDisableWebPagePreview());
        assertEquals(parseMode, sendMessage.getParseMode());
        return checkDefaultSendMessageParams(sendMessage);
    }

    public static SendMessage checkDefaultSendMessageParams(PartialBotApiMethod<?> method, String parseMode, boolean disableWebPagePreview, boolean hasKeyboard) {
        assertTrue(method instanceof SendMessage);
        return checkDefaultSendMessageParams((SendMessage) method, parseMode, disableWebPagePreview, hasKeyboard);
    }

    public static SendMessage checkDefaultSendMessageParams(SendMessage sendMessage, boolean disableWebPagePreview) {
        assertEquals(disableWebPagePreview, sendMessage.getDisableWebPagePreview());
        return checkDefaultSendMessageParams(sendMessage);
    }

    public static SendMessage checkDefaultSendMessageParams(SendMessage sendMessage, String parseMode, boolean disableWebPagePreview, boolean hasKeyboard) {
        assertEquals(parseMode, sendMessage.getParseMode());
        if (disableWebPagePreview) {
            assertEquals(Boolean.TRUE, sendMessage.getDisableWebPagePreview());
        }
        if (hasKeyboard) {
            assertNotNull(sendMessage.getReplyMarkup());
        }
        return checkDefaultSendMessageParams(sendMessage);
    }

    public static SendMessage checkDefaultSendMessageParams(SendMessage sendMessage, String parseMode) {
        assertEquals(parseMode, sendMessage.getParseMode());
        return checkDefaultSendMessageParams(sendMessage);
    }

    public static SendMessage checkDefaultSendMessageParams(PartialBotApiMethod<?> method, String parseMode) {
        assertTrue(method instanceof SendMessage);
        return checkDefaultSendMessageParams((SendMessage) method, parseMode);
    }

    public static EditMessageText checkDefaultEditMessageTextParams(PartialBotApiMethod<?> method, String parseMode, boolean disableWebPagePreview, boolean hasKeyboard) {
        assertTrue(method instanceof EditMessageText);
        return checkDefaultEditMessageTextParams((EditMessageText) method, parseMode, disableWebPagePreview, hasKeyboard);
    }

    public static EditMessageText checkDefaultEditMessageTextParams(EditMessageText editMessageText, String parseMode, boolean disableWebPagePreview, boolean hasKeyboard) {
        assertEquals(parseMode, editMessageText.getParseMode());
        if (disableWebPagePreview) {
            assertEquals(Boolean.TRUE, editMessageText.getDisableWebPagePreview());
        }
        if (hasKeyboard) {
            assertNotNull(editMessageText.getReplyMarkup());
        }
        return checkDefaultEditMessageTextParams(editMessageText);
    }

    public static EditMessageText checkDefaultEditMessageTextParams(EditMessageText editMessageText) {
        assertNotNull(editMessageText);
        assertNotNull(editMessageText.getChatId());
        assertNotNull(editMessageText.getMessageId());
        assertNotNull(editMessageText.getText());

        return editMessageText;
    }

    public static SendMessage checkDefaultSendMessageParams(PartialBotApiMethod<?> method) {
        assertTrue(method instanceof SendMessage);
        return checkDefaultSendMessageParams((SendMessage) method);
    }

    public static SendMessage checkDefaultSendMessageParams(SendMessage sendMessage) {
        assertNotNull(sendMessage);
        assertNotNull(sendMessage.getChatId());
        assertNotNull(sendMessage.getReplyToMessageId());
        assertNotNull(sendMessage.getText());

        return sendMessage;
    }

    public static SendDocument checkDefaultSendDocumentParams(PartialBotApiMethod<?> method) {
        assertTrue(method instanceof SendDocument);
        return checkDefaultSendDocumentParams((SendDocument) method);
    }

    public static SendDocument checkDefaultSendDocumentParams(SendDocument sendDocument) {
        assertNotNull(sendDocument);
        assertNotNull(sendDocument.getChatId());
        assertNotNull(sendDocument.getReplyToMessageId());
        assertNotNull(sendDocument.getDocument());

        return sendDocument;
    }

    public static SendPhoto checkDefaultSendPhotoParams(PartialBotApiMethod<?> method) {
        assertTrue(method instanceof SendPhoto);
        return checkDefaultSendPhotoParams((SendPhoto) method);
    }

    public static SendPhoto checkDefaultSendPhotoParams(SendPhoto sendPhoto, boolean hasSpoiler) {
        assertEquals(hasSpoiler, sendPhoto.getHasSpoiler());
        return checkDefaultSendPhotoParams(sendPhoto);
    }

    public static SendPhoto checkDefaultSendPhotoParams(SendPhoto sendPhoto) {
        assertNotNull(sendPhoto);
        assertNotNull(sendPhoto.getChatId());
        assertNotNull(sendPhoto.getReplyToMessageId());
        assertNotNull(sendPhoto.getPhoto());
        assertNotNull(sendPhoto.getCaption());

        return sendPhoto;
    }

    public static SendVideo checkDefaultSendVideoParams(PartialBotApiMethod<?> method) {
        assertTrue(method instanceof SendVideo);
        return checkDefaultSendVideoParams((SendVideo) method);
    }

    public static SendVideo checkDefaultSendVideoParams(SendVideo sendVideo) {
        assertNotNull(sendVideo);
        assertNotNull(sendVideo.getChatId());
        assertNotNull(sendVideo.getReplyToMessageId());
        assertNotNull(sendVideo.getVideo());
        assertNotNull(sendVideo.getCaption());

        return sendVideo;
    }

    public static SendMediaGroup checkDefaultSendMediaGroupParams(PartialBotApiMethod<?> method) {
        assertTrue(method instanceof SendMediaGroup);
        return checkDefaultSendMediaGroupParams((SendMediaGroup) method);
    }

    public static SendMediaGroup checkDefaultSendMediaGroupParams(SendMediaGroup sendMediaGroup) {
        assertNotNull(sendMediaGroup);
        assertFalse(sendMediaGroup.getMedias().isEmpty());
        assertNotNull(sendMediaGroup.getReplyToMessageId());
        assertNotNull(sendMediaGroup.getChatId());

        return sendMediaGroup;
    }

    public static SendLocation checkDefaultSendLocationParams(SendLocation sendLocation) {
        assertNotNull(sendLocation);
        assertNotNull(sendLocation.getChatId());
        assertNotNull(sendLocation.getReplyToMessageId());
        assertNotNull(sendLocation.getLatitude());
        assertNotNull(sendLocation.getLongitude());

        return sendLocation;
    }

    public static String getResourceAsString(String path) throws IOException {
        return IOUtils.toString(new FileInputStream("src/test/resources/" + path), StandardCharsets.UTF_8);
    }

}
