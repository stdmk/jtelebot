package org.telegram.bot.exception.speech;

public class TooLongSpeechException extends SpeechParseException {
    public TooLongSpeechException(String message) {
        super(message);
    }
}
