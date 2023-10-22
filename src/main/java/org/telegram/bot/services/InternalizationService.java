package org.telegram.bot.services;

import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import javax.annotation.Nullable;
import java.util.Set;

public interface InternalizationService {
    //TODO javadoc
    SendMessage internalize(SendMessage sendMessage, @Nullable String lang);
    EditMessageText internalize(EditMessageText editMessageText, @Nullable String lang);
    SendDocument internalize(SendDocument sendDocument, @Nullable String lang);
    SendPhoto internalize(SendPhoto sendPhoto, @Nullable String lang);
    SendVideo internalize(SendVideo sendVideo, @Nullable String lang);
    String internalize(String text, @Nullable String lang);
    Set<String> getAllTranslations(String code);
    Set<String> getAvailableLocales();
}
