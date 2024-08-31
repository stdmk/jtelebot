package org.telegram.bot.enums;

import lombok.RequiredArgsConstructor;
import org.telegram.bot.config.PropertiesConfig;

import java.util.function.Function;

/**
 * Sber scopes for receiving tokens.
 */
@RequiredArgsConstructor
public enum SberScope {

    /**
     * Salute scope (speech recognition).
     */
    SALUTE_SPEECH_PERS(PropertiesConfig::getSaluteSpeechSecret),

    /**
     * Gigachat (chat bot).
     */
    GIGACHAT_API_PERS(PropertiesConfig::getGigaChatSecret),
    ;

    public final Function<PropertiesConfig, String> getSecretFunction;

}
