package org.telegram.bot.services;

import org.telegram.bot.domain.entities.TalkerPhrase;

import java.util.List;
import java.util.Set;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.TalkerPhrase}.
 */
public interface TalkerPhraseService {

    /**
     * Save a TalkerPhrase.
     *
     * @param talkerPhraseSet the set of entities to save.
     */
    List<TalkerPhrase> save(Set<TalkerPhrase> talkerPhraseSet);
}
