package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Wiki;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.Wiki}.
 */
public interface WikiService {
    /**
     * Get a Wiki.
     *
     * @param wikiPageId of Wiki to get.
     * @return the persisted entity.
     */
    Wiki get(Integer wikiPageId);

    /**
     * Save a Wiki.
     *
     * @param wiki the entity to save.
     * @return the persisted entity.
     */
    Wiki save(Wiki wiki);

    /**
     * Save a list of Wiki.
     *
     * @param wikiList the entities to save.
     * @return the persisted entities.
     */
    List<Wiki> save(List<Wiki> wikiList);
}
