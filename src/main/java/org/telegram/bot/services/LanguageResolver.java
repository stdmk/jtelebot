package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Service Interface for resolving language code.
 */
public interface LanguageResolver {

    //TODO
    @NotNull
    Locale getLocale(Message message);

    @NotNull
    Locale getLocale(Chat chat);

    @Nullable
    String getChatLanguageCode(Message message);

    @Nullable
    String getChatLanguageCode(String chatId);

    @Nullable
    String getChatLanguageCode(Long chatId);

    @Nullable
    String getChatLanguageCode(Chat chat);
}
