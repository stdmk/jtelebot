package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.News;
import org.telegram.bot.domain.entities.NewsSource;

import java.util.List;

/**
 * Service Interface for managing {@link News}.
 */
public interface NewsService {

    /**
     * Get a News.
     *
     * @param chat entity for which gets News
     * @param newsId of News to get.
     * @return the persisted entity.
     */
    News get(Chat chat, Long newsId);

    /**
     * Get a News by Name.
     *
     * @param chat entity for which gets News
     * @param newsName name of News to get.
     * @return the persisted entity.
     */
    News get(Chat chat, String newsName);

    /**
     * Get a News by Chat and NewsSource.
     *
     * @param chat entity for which gets News
     * @param newsSource NewsSource of News to get.
     * @return the persisted entity.
     */
    News get(Chat chat, NewsSource newsSource);

    /**
     * Get a News by Name or Url.
     *
     * @param chat entity for which gets News
     * @param newsName name of News to get.
     * @param newsSource NewsSource of News to get.
     * @return the persisted entity.
     */
    News get(Chat chat, String newsName, NewsSource newsSource);

    /**
     * Get all News.
     *
     * @return the persisted entities.
     */
    List<News> getAll();

    /**
     * Get all News for Chat.
     *
     * @param chat entity for which gets News
     * @return the persisted entities.
     */
    List<News> getAll(Chat chat);

    /**
     * Get a News by NewsSource.
     *
     * @param newsSource entity for which gets News
     * @return the persisted entity.
     */
    List<News> getAll(NewsSource newsSource);

    /**
     * Save a News.
     *
     * @param news the entity to save.
     */
    void save(News news);

    /**
     * Remove a News.
     *
     * @param chat entity for which remove News
     * @param newsId of News to remove.
     * @return true if remove.
     */
    boolean remove(Chat chat, Long newsId);

    /**
     * Remove a News.
     *
     * @param chat entity for which remove News
     * @param newsName of News to remove.
     * @return true if remove.
     */
    boolean remove(Chat chat, String newsName);

    /**
     * Remove a News.
     *
     * @param newsId id of News entity
     */
    void remove(Long newsId);
}
