package org.telegram.bot.services;

import org.telegram.bot.domain.entities.NewsSource;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.NewsSource}.
 */
public interface NewsSourceService {
    /**
     * Get NewsSource by its url.
     * @param url - url of NewsSource.
     * @return the persisted entity.
     */
    NewsSource get(String url);

    /**
     * Save the NewsSource.
     * @param newsSource - entity to save.
     * @return the persisted entity.
     */
    NewsSource save(NewsSource newsSource);
}
