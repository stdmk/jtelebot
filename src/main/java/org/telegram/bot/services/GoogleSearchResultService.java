package org.telegram.bot.services;

import org.telegram.bot.domain.entities.GoogleSearchResult;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.GoogleSearchResult}.
 */
public interface GoogleSearchResultService {
    /**
     * Get a GoogleSearchResult.
     *
     * @param googleSearchResultId of GoogleSearchResult to get.
     * @return the persisted entity.
     */
    GoogleSearchResult get(Long googleSearchResultId);

    /**
     * Save a GoogleSearchResult.
     *
     * @param googleSearchResult the entity to save.
     * @return the persisted entity.
     */
    GoogleSearchResult save(GoogleSearchResult googleSearchResult);

    /**
     * Save a list of GoogleSearchResult.
     *
     * @param googleSearchResultList the entities to save.
     * @return the persisted entities.
     */
    List<GoogleSearchResult> save(List<GoogleSearchResult> googleSearchResultList);
}
