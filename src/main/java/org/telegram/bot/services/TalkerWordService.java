package org.telegram.bot.services;

import org.telegram.bot.domain.entities.TalkerWord;

import java.util.List;
import java.util.Set;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.TalkerWord}.
 */
public interface TalkerWordService {

    /**
     * Save a TalkerWords.
     *
     * @param talkerWordSet the set of entities to save.
     */
    List<TalkerWord> save(Set<TalkerWord> talkerWordSet);

    //TODO
    Set<TalkerWord> get(List<String> words);
}
