package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.TrackCode;

/**
 * Spring Data repository for the TrackCode entity.
 */
public interface TrackCodeRepository extends JpaRepository<TrackCode, Long> {
    TrackCode findByBarcode(String barcode);
}
