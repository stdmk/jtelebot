package org.telegram.bot.providers.sber;

import org.telegram.bot.exception.SpeechSynthesizeException;

public interface SpeechSynthesizer {

    byte[] synthesize(String text) throws SpeechSynthesizeException;
}
