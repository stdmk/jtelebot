package org.telegram.bot.services;

import org.telegram.bot.domain.entities.NewsSource;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.NewsSource}.
 */
public interface NewsSourceService {
    /**
     * Get a NewsSource.
     *
     * @param newsSourceId of NewsSource to get.
     * @return the persisted entity.
     */
    NewsSource get(Long newsSourceId);

    /**
     * Get a NewsSources by Name.
     *
     * @param newsSourceName name of NewsSource to get.
     * @return the persisted entity.
     */
    NewsSource get(String newsSourceName);

    /**
     * Get a NewsSources by Name or Url.
     *
     * @param newsSourceName name of NewsSource to get.
     * @param newsSourceUrl url of NewsSource to get.
     * @return the persisted entity.
     */
    NewsSource get(String newsSourceName, String newsSourceUrl);

    /**
     * Get all NewsSources.
     *
     * @return the persisted entities.
     */
    List<NewsSource> getAll();

    /**
     * Save a NewsSource.
     *
     * @param newsSource the entity to save.
     * @return the persisted entity.
     */
    NewsSource save(NewsSource newsSource);

    /**
     * Remove a NewsSource.
     *
     * @param newsSourceId of NewsSource to remove.
     * @return true if remove.
     */
    Boolean remove(Long newsSourceId);

    /**
     * Remove a NewsSource.
     *
     * @param newsSourceName of NewsSource to remove.
     * @return true if remove.
     */
    Boolean remove(String newsSourceName);
}
