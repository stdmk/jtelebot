package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.TrackCode;
import org.telegram.bot.domain.entities.TrackCodeEvent;

/**
 * Spring Data repository for the TrackCodeEvent entity.
 */
public interface TrackCodeEventRepository extends JpaRepository<TrackCodeEvent, Long> {
    void deleteAllByTrackCode(TrackCode trackCode);
}
