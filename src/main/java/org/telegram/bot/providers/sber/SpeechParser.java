package org.telegram.bot.providers.sber;

import org.telegram.bot.exception.speech.SpeechParseException;

public interface SpeechParser {

    /**
     * Recognize text in speech.
     *
     * @param file file of speech.
     * @param duration duration of speech.
     * @return text of speech.
     * @throws SpeechParseException errors of speech recognition.
     */
    String parse(byte[] file, Integer duration) throws SpeechParseException;
}
