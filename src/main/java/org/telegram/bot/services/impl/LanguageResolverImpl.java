package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatLanguage;
import org.telegram.bot.services.ChatLanguageService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.telegrambots.meta.api.objects.Message;

import javax.annotation.Nullable;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class LanguageResolverImpl implements LanguageResolver {

    private final ChatLanguageService chatLanguageService;
    private final PropertiesConfig propertiesConfig;

    @Override
    public Locale getLocale(Message message) {
        String lang = this.getChatLanguageCode(message);
        if (lang != null) {
            return Locale.forLanguageTag(lang);
        }

        return Locale.getDefault();
    }

    @Override
    public Locale getLocale(Chat chat) {
        String lang = this.getChatLanguageCode(chat);
        if (lang != null) {
            return Locale.forLanguageTag(lang);
        }

        return Locale.getDefault();
    }

    @Override
    public String getChatLanguageCode(Message message) {
        Chat chat = new Chat().setChatId(message.getChatId());

        String lang;
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
