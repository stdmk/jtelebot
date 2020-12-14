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
     * Save a ImageUrl.
     *
     * @param imageUrl the entity to save.
     * @return the persisted entity.
     */
    ImageUrl save(ImageUrl imageUrl);
}
