package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Locale;

/**
 * Service Interface for resolving language code.
 */
public interface LanguageResolver {

    /**
     * Get locale from Chat.
     *
     * @param chat Chat entity.
     * @return locale.
     */
    @NotNull
    Locale getLocale(Chat chat);

    /**
     * Get language code from Update.
     *
     * @param update Telegram Update.
     * @return language code.
     */
    @Nullable
    String getChatLanguageCode(Update update);

    /**
     * Get language code by Chat and User.
     *
     * @param message Telegram Message.
     * @param user User entity.
     * @return language code.
     */
    @Nullable
    String getChatLanguageCode(Message message, User user);

    /**
     * Get language code by Chat id.
     *
     * @param chatId id of Chat entity.
     * @return language code.
     */
    @Nullable
    String getChatLanguageCode(String chatId);

    /**
     * Get language code by Chat id.
     *
     * @param chatId id of Chat entity.
     * @return language code.
     */
    @Nullable
    String getChatLanguageCode(Long chatId);

    /**
     * Get language code from.
     *
     * @param chat Chat entity.
     * @return language code.
     */
    @Nullable
    String getChatLanguageCode(Chat chat);
}
