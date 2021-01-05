package org.telegram.bot.services;

import org.telegram.bot.domain.enums.BotSpeechTag;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.Speech}.
 */

public interface SpeechService {

    /**
     * Get a random message by tag.
     *
     * @param tag of message to get.
     * @return message.
     */

    String getRandomMessageByTag(BotSpeechTag tag);
}
