package org.telegram.bot.services;

import org.telegram.bot.domain.entities.TvChannel;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.TvChannel}.
 */
public interface TvChannelService {
    /**
     * Get a TvChannel.
     *
     * @param tvChannelId of TvChannel to get.
     * @return the persisted entity.
     */
    TvChannel get(Integer tvChannelId);

    /**
     * Get a TvChannel.
     *
     * @param tvChannelName of TvChannel to get.
     * @return the persisted entity.
     */
    List<TvChannel> get(String tvChannelName);

    /**
     * Save a TvChannel.
     *
     * @param tvChannel the entity to save.
     * @return the persisted entity.
     */
    TvChannel save(TvChannel tvChannel);

    /**
     * Save a list of TvChannel.
     *
     * @param tvChannelList entities to save.
     * @return list of persisted entities.
     */
    List<TvChannel> save(List<TvChannel> tvChannelList);

    /**
     * Clear a TvChannel table.
     */
    void clearTable();
}
