package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;

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
     * Get locale from Chat.
     *
     * @param message Telegram Message.
     * @param user Chat entity.
     * @return locale.
     */
    @NotNull
    Locale getLocale(Message message, User user);

    /**
     * Get language code from Update.
     *
     * @param botRequest request to bot.
     * @return language code.
     */
    @Nullable
    String getChatLanguageCode(BotRequest botRequest);

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
