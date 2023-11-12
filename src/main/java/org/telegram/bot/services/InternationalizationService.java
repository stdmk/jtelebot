package org.telegram.bot.services;

import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * i18n service.
 */
public interface InternationalizationService {

    /**
     * Internationalize outgoing message.
     *
     * @param sendMessage message being sent.
     * @param lang target language
     * @return internationalized message.
     */
    SendMessage internationalize(SendMessage sendMessage, @Nullable String lang);

    /**
     * Internationalize outgoing edit-message.
     *
     * @param editMessageText message being edit.
     * @param lang target language
     * @return internationalized message.
     */
    EditMessageText internationalize(EditMessageText editMessageText, @Nullable String lang);

    /**
     * Internationalize outgoing message (caption).
     *
     * @param sendDocument message being sent.
     * @param lang target language
     * @return internationalized message.
     */
    SendDocument internationalize(SendDocument sendDocument, @Nullable String lang);

    /**
     * Internationalize outgoing photo (caption).
     *
     * @param sendPhoto photo being sent.
     * @param lang target language
     * @return internationalized message.
     */
    SendPhoto internationalize(SendPhoto sendPhoto, @Nullable String lang);

    /**
     * Internationalize outgoing video (caption).
     *
     * @param sendVideo photo being sent.
     * @param lang target language
     * @return internationalized message.
     */
    SendVideo internationalize(SendVideo sendVideo, @Nullable String lang);

    /**
     * Internationalize into all languages.
     *
     * @param text text.
     * @return set of internationalized strings.
     */
    Set<String> internationalize(String text);

    /**
     * Internationalize of some text.
     *
     * @param text text.
     * @param lang target language
     * @return internationalized string.
     */
    String internationalize(String text, @Nullable String lang);

    /**
     * Get code translation in all available languages.
     *
     * @param code code of text.
     * @return set of translations.
     */
    Set<String> getAllTranslations(String code);

    /**
     * Get all available language codes.
     *
     * @return set of available language codes.
     */
    Set<String> getAvailableLocales();

}
