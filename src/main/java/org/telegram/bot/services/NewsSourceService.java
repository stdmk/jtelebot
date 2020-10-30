package org.telegram.bot.services;

import org.telegram.bot.domain.entities.NewsSource;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.NewsSource}.
 */
public interface NewsSourceService {
    /**
     * Get NewsSource by irs url.
     * @param url - url of NewsSource.
     * @return the persisted entity.
     */
    NewsSource get(String url);

    /**
     * Get all NewsSources.
     * @return the persisted entities.
     */
    List<NewsSource> getAll();

    /**
     * Save the NewsSource.
     * @param newsSource - entity to save.
     * @return the persisted entity.
     */
    NewsSource save(NewsSource newsSource);
}
