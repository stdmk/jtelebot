package org.telegram.bot.mapper.telegram.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.domain.model.response.Keyboard;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendMessageMapperTest {

    @Mock
    private KeyboardMapper keyboardMapper;
    @Mock
    private ParseModeMapper parseModeMapper;

    @InjectMocks
    private SendMessageMapperText sendMessageMapper;

    @Test
    void mapWithoutResponseSettingsTest() {
        final Long chatId = 123L;
        final String text = "text";
        final Integer replyToMessageId = 12345;

        Keyboard keyboard = new Keyboard();
        TextResponse textResponse = new TextResponse()
                .setChatId(chatId)
                .setReplyToMessageId(replyToMessageId)
                .setText(text)
                .setKeyboard(keyboard);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(List.of());
        when(keyboardMapper.toKeyboard(keyboard)).thenReturn(inlineKeyboardMarkup);

        SendMessage sendMessage = sendMessageMapper.map(textResponse);

        assertNotNull(sendMessage);

        assertEquals(chatId.toString(), sendMessage.getChatId());
        assertEquals(replyToMessageId, sendMessage.getReplyToMessageId());
        assertEquals(text, sendMessage.getText());
        assertEquals(inlineKeyboardMarkup, sendMessage.getReplyMarkup());
        assertNull(sendMessage.getDisableNotification());
        assertNull(sendMessage.getDisableWebPagePreview());
        assertNull(sendMessage.getParseMode());
    }

    @Test
    void mapWithParseModeTest() {
        final Long chatId = 123L;
        final String text = "text";
        final Integer replyToMessageId = 12345;
        final String expectedParseMode = "html";

        Keyboard keyboard = new Keyboard();
        ResponseSettings responseSettings = new ResponseSettings()
                .setFormattingStyle(FormattingStyle.HTML);
        TextResponse textResponse = new TextResponse()
                .setChatId(chatId)
                .setReplyToMessageId(replyToMessageId)
                .setText(text)
                .setKeyboard(keyboard)
                .setResponseSettings(responseSettings);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(List.of());
        when(keyboardMapper.toKeyboard(keyboard)).thenReturn(inlineKeyboardMarkup);
        when(parseModeMapper.toParseMode(FormattingStyle.HTML)).thenReturn(expectedParseMode);

        SendMessage sendMessage = sendMessageMapper.map(textResponse);

        assertNotNull(sendMessage);

        assertEquals(chatId.toString(), sendMessage.getChatId());
        assertEquals(replyToMessageId, sendMessage.getReplyToMessageId());
        assertEquals(text, sendMessage.getText());
        assertEquals(inlineKeyboardMarkup, sendMessage.getReplyMarkup());
        assertNull(sendMessage.getDisableNotification());
        assertNull(sendMessage.getDisableWebPagePreview());
        assertEquals(expectedParseMode, sendMessage.getParseMode());
    }

    @Test
    void mapWithResponseSettingsTest() {
        final Long chatId = 123L;
        final String text = "text";
        final Integer replyToMessageId = 12345;
        final String expectedParseMode = "html";

        Keyboard keyboard = new Keyboard();
        ResponseSettings responseSettings = new ResponseSettings()
                .setFormattingStyle(FormattingStyle.HTML)
                .setNotification(false)
                .setWebPagePreview(false);
        TextResponse textResponse = new TextResponse()
                .setChatId(chatId)
                .setReplyToMessageId(replyToMessageId)
                .setText(text)
                .setKeyboard(keyboard)
                .setResponseSettings(responseSettings);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(List.of());
        when(keyboardMapper.toKeyboard(keyboard)).thenReturn(inlineKeyboardMarkup);
        when(parseModeMapper.toParseMode(FormattingStyle.HTML)).thenReturn(expectedParseMode);

        SendMessage sendMessage = sendMessageMapper.map(textResponse);

        assertNotNull(sendMessage);

        assertEquals(chatId.toString(), sendMessage.getChatId());
        assertEquals(replyToMessageId, sendMessage.getReplyToMessageId());
        assertEquals(text, sendMessage.getText());
        assertEquals(inlineKeyboardMarkup, sendMessage.getReplyMarkup());
        assertTrue(sendMessage.getDisableNotification());
        assertTrue(sendMessage.getDisableWebPagePreview());
        assertEquals(expectedParseMode, sendMessage.getParseMode());
    }

}