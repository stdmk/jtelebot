package org.telegram.bot.domain;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.telegram.bot.utils.TextUtils.getPotentialCommandInText;

public interface CommandParent<T extends PartialBotApiMethod<?>> {

    T parse(Update update) throws Exception;

    default Message getMessageFromUpdate(Update update) {
        if (update.hasMessage()) {
            return update.getMessage();
        } else if (update.hasEditedMessage()) {
            return update.getEditedMessage();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage();
        }

        return null;
    }

    default String getTextMessage(Update update) {
        if (update.hasCallbackQuery()) {
            return cutCommandInText(update.getCallbackQuery().getData());
        }
        return cutCommandInText(getMessageFromUpdate(update).getText());
    }

    default String cutCommandInText(String text) {
        if (text == null) {
            return null;
        }
        if (text.charAt(0) == '/') {
            text = text.substring(1);
        }
        String cuttedText = getPotentialCommandInText(text);
        if (cuttedText != null) {
            if (text.toLowerCase().equals(cuttedText)) {
                return null;
            }
            int i = text.indexOf("@");
            if (i > 0 && text.endsWith("bot")) {
                text = text.substring(0, i);
            }
            text = text.substring(cuttedText.length());
            if (text.equals("")) {
                return null;
            }
            if (text.startsWith("_")) {
                return text;
            }

            return text.substring(1);
        }

        return null;
    }
}
