package org.telegram.bot.mapper.email.response;

import org.junit.jupiter.api.Test;
import org.telegram.bot.domain.model.response.Keyboard;
import org.telegram.bot.domain.model.response.KeyboardButton;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyboardEmailMapperTest {

    private final KeyboardEmailMapper keyboardEmailMapper = new KeyboardEmailMapper();

    @Test
    void keyboardToStringTest() {
        final String expected = "name1 — callback1<br><br>name2 — callback2<br>name3 — callback3";
        Keyboard keyboard = new Keyboard(
                List.of(
                        List.of(new KeyboardButton().setName("name1").setCallback("callback1")),
                        List.of(
                                new KeyboardButton().setName("name2").setCallback("callback2"),
                                new KeyboardButton().setName("name3").setCallback("callback3")
                        )
                ));

        String actual = keyboardEmailMapper.keyboardToString(keyboard);

        assertEquals(expected, actual);
    }

}