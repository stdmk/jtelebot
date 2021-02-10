package org.telegram.bot.services;

import org.telegram.bot.domain.entities.ImageUrl;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.ImageUrl}.
 */
public interface ImageUrlService {
    /**
     * Get a ImageUrl.
     *
     * @param imageUrlId of ImageUrl to get.
     * @return the persisted entity.
     */
    ImageUrl get(Long imageUrlId);

    /**
     * Get a ImageUrl.
     *
     * @param url url of ImageUrl.
     * @return the persisted entity.
     */
    ImageUrl get(String url);

    /**
     * Get random ImageUrl.
     *
     * @return the persisted entity.
     */
    ImageUrl getRandom();

    /**
     * Save a ImageUrl.
     *
     * @param imageUrl the entity to save.
     * @return the persisted entity.
     */
    ImageUrl save(ImageUrl imageUrl);

    /**
     * Save a list of ImageUrl.
     *
     * @param imageUrlList the entities to save.
     * @return the persisted entities.
     */
    List<ImageUrl> save(List<ImageUrl> imageUrlList);
}
