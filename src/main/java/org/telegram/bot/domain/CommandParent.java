package org.telegram.bot.domain;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.telegram.bot.utils.TextUtils.getPotentialCommandInText;

public interface CommandParent<T extends PartialBotApiMethod> {

    T parse(Update update) throws Exception;

    default String cutCommandInText(String text) {
        String cuttedText = getPotentialCommandInText(text);
        if (cuttedText != null) {
            if (text.toLowerCase().equals(cuttedText)) {
                return null;
            }
            return text.replace(cuttedText, "").substring(1).toLowerCase();
        }

        return null;
    }
}
