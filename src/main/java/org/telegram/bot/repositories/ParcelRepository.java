package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.telegram.bot.domain.entities.Parcel;
import org.telegram.bot.domain.entities.TrackCode;
import org.telegram.bot.domain.entities.User;

import java.util.List;

/**
 * Spring Data repository for the Parcel entity.
 */
public interface ParcelRepository extends JpaRepository<Parcel, Long> {
    Parcel findByUserAndId(User user, Long id);
    Parcel findByUserAndNameIgnoreCase(User user, String name);

    Parcel findByUserAndTrackCodeBarcodeIgnoreCase(User user, String barcode);

    @Query("select p from Parcel p where (p.user = :user) and (lower(p.trackCode.barcode) = lower(:barcode) or lower(p.name) = lower(:name)) ")
    Parcel findByUserAndTrackCodeBarcodeIgnoreCaseOrNameIgnoreCase(@Param("user") User user,
                                                                   @Param("barcode") String barcode,
                                                                   @Param("name") String name);

    Parcel findByUserAndTrackCodeAndNameIgnoreCase(User user, TrackCode trackCode, String name);

    List<Parcel> findAllByUser(User user);

    List<Parcel> findAllByTrackCode(TrackCode trackCode);
}
