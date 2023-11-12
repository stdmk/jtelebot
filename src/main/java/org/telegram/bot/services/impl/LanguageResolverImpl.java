package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatLanguage;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserLanguage;
import org.telegram.bot.services.ChatLanguageService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.services.UserLanguageService;
import org.telegram.bot.utils.TelegramUtils;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.annotation.Nullable;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class LanguageResolverImpl implements LanguageResolver {

    private final ChatLanguageService chatLanguageService;
    private final UserLanguageService userLanguageService;
    private final PropertiesConfig propertiesConfig;

    @Override
    public Locale getLocale(Chat chat) {
        String lang = this.getChatLanguageCode(chat);
        if (lang != null) {
            return Locale.forLanguageTag(lang);
        }

        return Locale.getDefault();
    }

    @Override
    public Locale getLocale(Message message, User user) {
        String lang = this.getChatLanguageCode(message, user);

        if (lang != null) {
            return Locale.forLanguageTag(lang);
        }

        return Locale.getDefault();
    }

    @Override
    public String getChatLanguageCode(Update update) {
        Message message = TelegramUtils.getMessage(update);
        if (message == null) {
            return null;
        }

        Long userId;
        if (update.hasCallbackQuery()) {
            userId = update.getCallbackQuery().getFrom().getId();
        } else {
            userId = message.getFrom().getId();
        }

        return getChatLanguageCode(message, new User().setUserId(userId));
    }

    @Nullable
    @Override
    public String getChatLanguageCode(Message message, User user) {
        Chat chat = new Chat().setChatId(message.getChatId());

        String lang;
        UserLanguage userLanguage = userLanguageService.get(chat, user);
        if (userLanguage != null) {
            return userLanguage.getLang();
        }

        ChatLanguage chatLanguage = chatLanguageService.get(chat);
        if (chatLanguage != null) {
            return chatLanguage.getLang();
        }

        lang = message.getFrom().getLanguageCode();
        if (lang != null) {
            return lang;
        }

        return propertiesConfig.getDefaultLanguage();
    }

    @Nullable
    @Override
    public String getChatLanguageCode(String chatId) {
        return getChatLanguageCode(Long.valueOf(chatId));
    }

    @Nullable
    @Override
    public String getChatLanguageCode(Long chatId) {
        return getChatLanguageCode(new Chat().setChatId(chatId));
    }

    @Nullable
    @Override
    public String getChatLanguageCode(Chat chat) {
        ChatLanguage chatLanguage = chatLanguageService.get(chat);
        if (chatLanguage == null) {
            return null;
        }

        return chatLanguage.getLang();
    }

}
