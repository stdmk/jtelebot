package org.telegram.bot.services;

import com.rometools.rome.feed.synd.SyndEntry;
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
     * Save a NewsMessage.
     *
     * @param newsMessage the entity to save.
     * @return the persisted entity.
     */
    NewsMessage save(NewsMessage newsMessage);

    /**
     * Save a list of NewsMessage.
     *
     * @param newsMessageList a list the entities to save.
     * @return the persisted entities.
     */
    List<NewsMessage> save(List<NewsMessage> newsMessageList);

    /**
     * Build short text message from NewsMessage with News.
     *
     * @param newsMessage News entity.
     * @param sourceName NewsSource entity
     * @return short text of news message.
     */
    String buildShortNewsMessageText(NewsMessage newsMessage, String sourceName);

    /**
     * Build short text message from NewsMessage.
     *
     * @param newsMessage NewsMessage entity.
     * @return short text of news message.
     */
    String buildShortNewsMessageText(NewsMessage newsMessage);

    /**
     * Build short text message from NewsMessage.
     *
     * @param syndEntry - rss entry
     * @return the persisted entities.
     */
    NewsMessage buildNewsMessageFromSyndEntry(SyndEntry syndEntry);

    /**
     * Build full text message from NewsMessage.
     *
     * @param newsMessage NewsMessage entity.
     * @return the persisted entities.
     */
    String buildFullNewsMessageText(NewsMessage newsMessage);
}
