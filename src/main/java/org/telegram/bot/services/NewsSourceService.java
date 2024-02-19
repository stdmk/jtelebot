package org.telegram.bot.services;

import org.springframework.data.domain.Page;
import org.telegram.bot.domain.entities.NewsSource;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.NewsSource}.
 */
public interface NewsSourceService {

    /**
     * Get NewsSource by its id.
     * @param newsSourceId - id of NewsSource.
     * @return the persisted entity.
     */
    NewsSource get(Long newsSourceId);

    /**
     * Get NewsSource by its url.
     * @param url - url of NewsSource.
     * @return the persisted entity.
     */
    NewsSource get(String url);

    /**
     * Get pageable NewsSource list.
     *
     * @param page page of list.
     * @return list of persisted entities.
     */
    Page<NewsSource> getAll(int page);

    /**
     * Get all NewsSource list.
     *
     * @return list of persisted entities.
     */
    List<NewsSource> getAll();

    /**
     * Save the NewsSource.
     * @param newsSource - entity to save.
     * @return the persisted entity.
     */
    NewsSource save(NewsSource newsSource);

    /**
     * NewsSource removing.
     *
     * @param newsSource removing entity.
     */
    void remove(NewsSource newsSource);
}
