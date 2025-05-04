package org.telegram.bot.mapper.telegram.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.EditResponse;
import org.telegram.bot.domain.model.response.Keyboard;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditMessageMapperTest {

    @Mock
    private KeyboardMapper keyboardMapper;
    @Mock
    private ParseModeMapper parseModeMapper;

    @InjectMocks
    private EditMessageMapperText editMessageMapper;

    @Test
    void toSendDocumentWithoutResponseSettingsTest() {
        final Long chatId = 123L;
        final String text = "text";
        final Integer messageId = 12345;

        Message message = new Message().setMessageId(messageId).setChat(new Chat().setChatId(chatId));
        Keyboard keyboard = new Keyboard();
        EditResponse editResponse = new EditResponse(message)
                .setChatId(chatId)
                .setText(text)
                .setKeyboard(keyboard);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(List.of());
        when(keyboardMapper.toKeyboard(keyboard)).thenReturn(inlineKeyboardMarkup);

        EditMessageText editMessageText = editMessageMapper.map(editResponse);

        assertEquals(chatId.toString(), editMessageText.getChatId());
        assertEquals(messageId, editMessageText.getMessageId());
        assertEquals(text, editMessageText.getText());
        assertEquals(inlineKeyboardMarkup, editMessageText.getReplyMarkup());
        assertNull(editMessageText.getDisableWebPagePreview());
        assertNull(editMessageText.getParseMode());
    }

    @Test
    void toSendDocumentWithParseModeTest() {
        final Long chatId = 123L;
        final String text = "text";
        final Integer messageId = 12345;
        final String expectedParseMode = "HTML";

        Message message = new Message().setMessageId(messageId).setChat(new Chat().setChatId(chatId));
        ResponseSettings responseSettings = new ResponseSettings().setFormattingStyle(FormattingStyle.HTML);
        Keyboard keyboard = new Keyboard();
        EditResponse editResponse = new EditResponse(message)
                .setChatId(chatId)
                .setText(text)
                .setKeyboard(keyboard)
                .setResponseSettings(responseSettings);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(List.of());
        when(keyboardMapper.toKeyboard(keyboard)).thenReturn(inlineKeyboardMarkup);
        when(parseModeMapper.toParseMode(FormattingStyle.HTML)).thenReturn(expectedParseMode);

        EditMessageText editMessageText = editMessageMapper.map(editResponse);

        assertEquals(chatId.toString(), editMessageText.getChatId());
        assertEquals(messageId, editMessageText.getMessageId());
        assertEquals(text, editMessageText.getText());
        assertEquals(inlineKeyboardMarkup, editMessageText.getReplyMarkup());
        assertNull(editMessageText.getDisableWebPagePreview());
        assertEquals(expectedParseMode, editMessageText.getParseMode());
    }

    @Test
    void toSendDocumentWithResponseSettingsTest() {
        final Long chatId = 123L;
        final String text = "text";
        final Integer messageId = 12345;
        final String expectedParseMode = "HTML";

        Message message = new Message().setMessageId(messageId).setChat(new Chat().setChatId(chatId));
        ResponseSettings responseSettings = new ResponseSettings()
                .setWebPagePreview(false)
                .setFormattingStyle(FormattingStyle.HTML);
        Keyboard keyboard = new Keyboard();
        EditResponse editResponse = new EditResponse(message)
                .setChatId(chatId)
                .setText(text)
                .setKeyboard(keyboard)
                .setResponseSettings(responseSettings);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(List.of());
        when(keyboardMapper.toKeyboard(keyboard)).thenReturn(inlineKeyboardMarkup);
        when(parseModeMapper.toParseMode(FormattingStyle.HTML)).thenReturn(expectedParseMode);

        EditMessageText editMessageText = editMessageMapper.map(editResponse);

        assertEquals(chatId.toString(), editMessageText.getChatId());
        assertEquals(messageId, editMessageText.getMessageId());
        assertEquals(text, editMessageText.getText());
        assertEquals(inlineKeyboardMarkup, editMessageText.getReplyMarkup());
        assertTrue(editMessageText.getDisableWebPagePreview());
        assertEquals(expectedParseMode, editMessageText.getParseMode());
    }

}