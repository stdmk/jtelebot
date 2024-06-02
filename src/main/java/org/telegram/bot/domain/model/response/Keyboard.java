package org.telegram.bot.domain.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class Keyboard {

    public Keyboard(KeyboardButton... buttons) {
        List<List<KeyboardButton>> buttonsRows = new ArrayList<>(buttons.length);

        for (KeyboardButton button : buttons) {
            buttonsRows.add(List.of(button));
        }

        this.keyboardButtonsList = buttonsRows;
    }

    public Keyboard setKeyboardButtonsList(List<KeyboardButton> buttons) {
        this.keyboardButtonsList = List.of(buttons);
        return this;
    }

    private List<List<KeyboardButton>> keyboardButtonsList;

}
