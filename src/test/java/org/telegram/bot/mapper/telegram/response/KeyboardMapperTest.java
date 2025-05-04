package org.telegram.bot.mapper.telegram.response;

import org.junit.jupiter.api.Test;
import org.telegram.bot.domain.model.response.Keyboard;
import org.telegram.bot.domain.model.response.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KeyboardMapperTest {

    private final KeyboardMapper keyboardMapper = new KeyboardMapper();

    @Test
    void toKeyboardNullTest() {
        assertNull(keyboardMapper.toKeyboard(null));
    }

    @Test
    void toKeyboardEmptyTest() {
        Keyboard keyboard = new Keyboard();
        InlineKeyboardMarkup actual = keyboardMapper.toKeyboard(keyboard);
        assertTrue(actual.getKeyboard().isEmpty());
    }

    @Test
    void toKeyboardTest() {
        KeyboardButton button1 = new KeyboardButton("name1", "callback1");
        KeyboardButton button2 = new KeyboardButton("name2", "callback2");
        KeyboardButton button3 = new KeyboardButton("name3", "callback3");
        KeyboardButton button4 = new KeyboardButton("name4", "callback4");
        List<KeyboardButton> row1 = List.of(button1, button2);
        List<KeyboardButton> row2 = List.of(button3, button4);
        Keyboard keyboard = new Keyboard(List.of(row1, row2));

        InlineKeyboardMarkup actual = keyboardMapper.toKeyboard(keyboard);

        assertNotNull(actual);

        List<InlineKeyboardRow> mappedKeyboard = actual.getKeyboard();
        assertEquals(2, mappedKeyboard.size());

        InlineKeyboardRow inlineKeyboardButtons1 = mappedKeyboard.get(0);
        assertEquals(2, inlineKeyboardButtons1.size());

        InlineKeyboardButton inlineKeyboardButton1 = inlineKeyboardButtons1.get(0);
        assertEquals(button1.getName(), inlineKeyboardButton1.getText());
        assertEquals(button1.getCallback(), inlineKeyboardButton1.getCallbackData());

        InlineKeyboardButton inlineKeyboardButton2 = inlineKeyboardButtons1.get(1);
        assertEquals(button2.getName(), inlineKeyboardButton2.getText());
        assertEquals(button2.getCallback(), inlineKeyboardButton2.getCallbackData());

        InlineKeyboardRow inlineKeyboardButtons2 = mappedKeyboard.get(1);
        assertEquals(2, inlineKeyboardButtons2.size());

        InlineKeyboardButton inlineKeyboardButton3 = inlineKeyboardButtons2.get(0);
        assertEquals(button3.getName(), inlineKeyboardButton3.getText());
        assertEquals(button3.getCallback(), inlineKeyboardButton3.getCallbackData());

        InlineKeyboardButton inlineKeyboardButton4 = inlineKeyboardButtons2.get(1);
        assertEquals(button4.getName(), inlineKeyboardButton4.getText());
        assertEquals(button4.getCallback(), inlineKeyboardButton4.getCallbackData());
    }

}