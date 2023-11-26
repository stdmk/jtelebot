package org.telegram.bot.providers.sber;

import org.telegram.bot.enums.SaluteSpeechVoice;
import org.telegram.bot.exception.SpeechSynthesizeException;

public interface SaluteSpeechSynthesizer {
    byte[] synthesize(String text, String langCode, SaluteSpeechVoice saluteSpeechVoice) throws SpeechSynthesizeException;
}
