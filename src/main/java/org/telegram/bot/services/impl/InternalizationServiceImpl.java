package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.telegram.bot.services.InternalizationService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.MathUtils.getRandomInRange;

@RequiredArgsConstructor
@Service
@Slf4j
public class InternalizationServiceImpl implements InternalizationService {

    private static final String CSV_SEPARATOR = "|";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z.]+)}");

    private final LanguageResolver languageResolver;
    private final MessageSource messageSource;

    @Override
    public SendMessage internalize(SendMessage sendMessage, @Nullable String lang) {
        sendMessage.setText(this.internalize(sendMessage.getText(), lang));
        this.internalize(sendMessage.getReplyMarkup(), lang);

        return sendMessage;
    }

    @Override
    public EditMessageText internalize(EditMessageText editMessageText, @Nullable String lang) {
        editMessageText.setText(this.internalize(editMessageText.getText(), lang));
        this.internalize(editMessageText.getReplyMarkup(), lang);

        return editMessageText;
    }

    private void internalize(ReplyKeyboard replyMarkup, String lang) {
        if (replyMarkup != null) {
            if (replyMarkup instanceof InlineKeyboardMarkup) {
                ((InlineKeyboardMarkup) replyMarkup).getKeyboard().forEach(buttons ->
                        buttons.forEach(button -> {
                            button.setText(this.internalize(button.getText(), lang));
                            button.setCallbackData(this.internalize(button.getCallbackData(), lang));
                        }));
            }
        }
    }

    @Override
    public SendDocument internalize(SendDocument sendDocument, @Nullable String lang) {
        sendDocument.setCaption(this.internalize(sendDocument.getCaption(), lang));
        return sendDocument;
    }

    @Override
    public SendPhoto internalize(SendPhoto sendPhoto, @Nullable String lang) {
        sendPhoto.setCaption(this.internalize(sendPhoto.getCaption(), lang));
        return sendPhoto;
    }

    @Override
    public SendVideo internalize(SendVideo sendVideo, @Nullable String lang) {
        sendVideo.setCaption(this.internalize(sendVideo.getCaption(), lang));
        return sendVideo;
    }

    @Override
    public String internalize(String text, @Nullable String lang) {
        if (text == null) {
            return null;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);

        List<MatchResult> results = matcher.results().collect(Collectors.toList());
        if (results.isEmpty()) {
            return text;
        }

        Locale locale = resolveLocale(lang);

        Map<String, String> valuesMap = new HashMap<>();
        results.forEach(matchResult -> {
            String code = matchResult.group(1);
            String message = messageSource.getMessage(code, null, locale);

            if (message.contains(CSV_SEPARATOR)) {
                message = getRandomMessageFromCsvString(message);
            }

            valuesMap.put(code, message);
        });

        StringSubstitutor substitutor = new StringSubstitutor(valuesMap);

        return substitutor.replace(text);
    }

    private String getRandomMessageFromCsvString(String data) {
        String[] messages = data.split("\\" + CSV_SEPARATOR);
        int randomIndex = getRandomInRange(0, messages.length) - 1;

        return messages[randomIndex];
    }

    @Override
    public Set<String> getAllTranslations(String code) {
        Set<String> translations = new HashSet<>();

        for (Locale locale : Locale.getAvailableLocales()) {
            String translation = messageSource.getMessage(code, null, locale);
            translations.add(translation);
        }

        return translations;
    }

    @Override
    public Set<String> getAvailableLocales() {
        return getAllTranslations("language");
    }

    private Locale resolveLocale(String lang) {
        if (lang != null) {
            return Locale.forLanguageTag(lang);
        }

        return Locale.getDefault();
    }

}
