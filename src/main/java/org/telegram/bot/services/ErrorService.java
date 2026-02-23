package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Error;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;

import java.util.List;

public interface ErrorService {
    void saveRequest(Object request, PartialBotApiMethod<?> response, Throwable throwable, String comment);
    void saveResponse(PartialBotApiMethod<?> response, Throwable throwable, String comment);
    void save(Exception e, String comment);
    void saveRequest(Object request, String comment);
    void saveRequest(Object request, Throwable throwable, String comment);
    Error get(long id);
    List<Error> getAll();
    void clear();
}
