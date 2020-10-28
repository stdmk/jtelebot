package org.telegram.bot.services;

import org.telegram.bot.domain.entities.News;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.News}.
 */
public interface NewsService {
    /**
     * Get a News.
     *
     * @param newsId of News to get.
     * @return the persisted entity.
     */
    News get(Long newsId);

    /**
     * Save a News.
     *
     * @param news the entity to save.
     * @return the persisted entity.
     */
    News save(News news);
}
