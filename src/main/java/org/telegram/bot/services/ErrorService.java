package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Error;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;

import java.util.List;

public interface ErrorService {
    void save(Object request, PartialBotApiMethod<?> response, Throwable throwable, String comment);
    void save(PartialBotApiMethod<?> response, Throwable throwable, String comment);
    void save(Object request, String comment);
    void save(Object request, Throwable throwable, String comment);
    Error get(long id);
    List<Error> getAll();
    void clear();
}
