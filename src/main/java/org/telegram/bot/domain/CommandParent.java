package org.telegram.bot.domain;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

public abstract class CommandParent<T extends PartialBotApiMethod> {
    public abstract T parse(Update update) throws Exception;
}
