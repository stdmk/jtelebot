package org.telegram.bot.services;

import org.telegram.bot.domain.entities.TrackCodeEvent;

import java.util.List;

public interface PostTrackingService {
    /**
     * Get TrackCodeEvent data from post tracking service api.
     *
     * @param barcode barcode of parcel.
     * @return list of TrackCodeEvents.
     */
    List<TrackCodeEvent> getData(String barcode);
}
