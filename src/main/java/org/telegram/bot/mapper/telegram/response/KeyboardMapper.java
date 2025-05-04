package org.telegram.bot.mapper.telegram.response;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.Keyboard;
import org.telegram.bot.domain.model.response.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

@Component
public class KeyboardMapper {

    public InlineKeyboardMarkup toKeyboard(Keyboard keyboard) {
        if (keyboard == null) {
            return null;
        }

        return new InlineKeyboardMarkup(keyboard.getKeyboardButtonsList().stream().map(this::toInlineKeyboardRow).toList());
    }

    private InlineKeyboardRow toInlineKeyboardRow(List<KeyboardButton> keyboardButtonList) {
        InlineKeyboardRow inlineKeyboardRow = new InlineKeyboardRow(keyboardButtonList.size());
        inlineKeyboardRow.addAll(keyboardButtonList.stream().map(this::toInlineKeyboardButton).toList());
        return inlineKeyboardRow;
    }

    private InlineKeyboardButton toInlineKeyboardButton(KeyboardButton keyboardButton) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(keyboardButton.getName());
        inlineKeyboardButton.setCallbackData(keyboardButton.getCallback());

        return inlineKeyboardButton;
    }

}
