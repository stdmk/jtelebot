package org.telegram.bot.providers.sber;

import org.telegram.bot.exception.speech.SpeechSynthesizeException;

import javax.validation.constraints.NotNull;

public interface SpeechSynthesizer {

    byte[] synthesize(String text, @NotNull String langCode) throws SpeechSynthesizeException;
}
