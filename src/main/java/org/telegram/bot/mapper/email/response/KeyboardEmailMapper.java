package org.telegram.bot.mapper.email.response;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.Keyboard;
import org.telegram.bot.domain.model.response.KeyboardButton;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class KeyboardEmailMapper {

    public String keyboardToString(Keyboard keyboard) {
        return keyboard.getKeyboardButtonsList()
                .stream()
                .map(this::keyboardRowToString)
                .collect(Collectors.joining("<br><br>"));
    }

    private String keyboardRowToString(List<KeyboardButton> keyboardButtons) {
        return keyboardButtons
                .stream()
                .map(this::buttonToString)
                .collect(Collectors.joining("<br>"));
    }

    private String buttonToString(KeyboardButton button) {
        return button.getName() + " — " + button.getCallback();
    }

}
