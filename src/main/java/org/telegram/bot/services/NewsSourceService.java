package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.NewsSource;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.NewsSource}.
 */
public interface NewsSourceService {
    /**
     * Get a NewsSource.
     *
     * @param chat entity for which gets NewsSource
     * @param newsSourceId of NewsSource to get.
     * @return the persisted entity.
     */
    NewsSource get(Chat chat, Long newsSourceId);

    /**
     * Get a NewsSources by Name.
     *
     * @param chat entity for which gets NewsSource
     * @param newsSourceName name of NewsSource to get.
     * @return the persisted entity.
     */
    NewsSource get(Chat chat, String newsSourceName);

    /**
     * Get a NewsSources by Name or Url.
     *
     * @param chat entity for which gets NewsSource
     * @param newsSourceName name of NewsSource to get.
     * @param newsSourceUrl url of NewsSource to get.
     * @return the persisted entity.
     */
    NewsSource get(Chat chat, String newsSourceName, String newsSourceUrl);

    /**
     * Get all NewsSources for Chat.
     *
     * @param chat entity for which gets NewsSources
     * @return the persisted entities.
     */
    List<NewsSource> getAll(Chat chat);

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
     * @param chat entity for which remove NewsSource
     * @param newsSourceId of NewsSource to remove.
     * @return true if remove.
     */
    Boolean remove(Chat chat, Long newsSourceId);

    /**
     * Remove a NewsSource.
     *
     * @param chat entity for which remove NewsSource
     * @param newsSourceName of NewsSource to remove.
     * @return true if remove.
     */
    Boolean remove(Chat chat, String newsSourceName);
}
