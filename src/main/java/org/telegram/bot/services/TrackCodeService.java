package org.telegram.bot.services;

import org.telegram.bot.domain.entities.TrackCode;
import org.telegram.bot.exception.BotException;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.TrackCode}.
 */
public interface TrackCodeService {

    /**
     * Get TrackCode by id.
     *
     * @param id id of entity.
     * @return the persisted entity.
     */
    TrackCode get(Long id);

    /**
     * Get TrackCode by barcode.
     *
     * @param barcode barcode
     * @return the persisted entity.
     */
    TrackCode get(String barcode);

    /**
     * Get all TrackCodes.
     *
     * @return the persisted entities.
     */
    List<TrackCode> getAll();

    /**
     * Save the TrackCode.
     *
     * @param trackCode TrackCode entity.
     * @return the persisted entities.
     */
    TrackCode save(TrackCode trackCode);

    /**
     * Remove the TrackCode.
     *
     * @param trackCode TrackCode entity.
     */
    void remove(TrackCode trackCode);

    /**
     * Get current TrackCode entities count.
     *
     * @return count of persisted entities.
     */
    long getTrackCodesCount();

    /**
     * Update all tracks data.
     *
     */
    void updateFromApi();

    /**
     * Update the track data.
     *
     */
    void updateFromApi(TrackCode trackCode) throws BotException;
}
