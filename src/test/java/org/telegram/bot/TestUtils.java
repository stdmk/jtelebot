package org.telegram.bot;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestUtils {

    public static final String BOT_USERNAME = "jtelebot";
    public static final Long DEFAULT_CHAT_ID = -1L;
    public static final String DEFAULT_MESSAGE_TEXT = "test";

    public static Update getUpdate() {
        return getUpdate(DEFAULT_CHAT_ID, DEFAULT_MESSAGE_TEXT);
    }

    public static Update getUpdateWithCallback(String callback) {
        Chat chat = new Chat();
        chat.setId(DEFAULT_CHAT_ID);

        User user = new User();
        user.setId(1L);

        Message message = new Message();
        message.setMessageId(1);
        message.setChat(chat);
        message.setFrom(user);

        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setFrom(user);
        callbackQuery.setMessage(message);
        callbackQuery.setData(callback);

        Update update = new Update();
        update.setCallbackQuery(callbackQuery);

        return update;
    }

    public static Update getUpdate(String textMessage) {
        return getUpdate(DEFAULT_CHAT_ID, textMessage);
    }

    public static Update getUpdate(Long chatId, String textMessage) {
        Chat chat = new Chat();
        chat.setId(chatId);

        User user = new User();
        user.setId(1L);

        Message message = new Message();
        message.setMessageId(1);
        message.setChat(chat);
        message.setFrom(user);
        message.setText(textMessage);

        Update update = new Update();
        update.setMessage(message);

        return update;
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
}
