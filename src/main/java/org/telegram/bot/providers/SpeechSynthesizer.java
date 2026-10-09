package org.telegram.bot.providers;

import jakarta.validation.constraints.NotNull;
import org.telegram.bot.exception.speech.SpeechSynthesizeException;

public interface SpeechSynthesizer {

    byte[] synthesize(String text, @NotNull String langCode) throws SpeechSynthesizeException;
}
