package org.telegram.bot.services;

import org.telegram.bot.domain.entities.NewsMessage;

import java.util.List;

/**
 * Service Interface for managing {@link NewsMessage}.
 */
public interface NewsMessageService {
    /**
     * Get a NewsMessage.
     *
     * @param newsId of News to get.
     * @return the persisted entity.
     */
    NewsMessage get(Long newsId);

    /**
     * Save a list of NewsMessage.
     *
     * @param newsMessageList a list the entities to save.
     * @return the persisted entities.
     */
    List<NewsMessage> save(List<NewsMessage> newsMessageList);

    /**
     * Save a NewsMessage.
     *
     * @param newsMessage the entity to save.
     * @return the persisted entity.
     */
    NewsMessage save(NewsMessage newsMessage);

}
