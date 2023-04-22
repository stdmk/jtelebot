package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Parcel;
import org.telegram.bot.domain.entities.TrackCode;
import org.telegram.bot.domain.entities.User;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.Parcel}.
 */

public interface ParcelService {
    /**
     * Save Parcel.
     *
     * @param parcel entity to save.
     */
    void save(Parcel parcel);

    /**
     * Get Parcel by User and parcel id.
     *
     * @param user User entity.
     * @param parcelId id of Parcel.
     * @return the persisted entity.
     */
    Parcel get(User user, Long parcelId);

    /**
     * Get Parcel by User and name.
     *
     * @param user User entity.
     * @param parcelName name of Parcel.
     * @return the persisted entity.
     */
    Parcel getByName(User user, String parcelName);

    /**
     * Get Parcel by User and barcode.
     *
     * @param user User entity.
     * @param barcode barcode of Parcel.
     * @return the persisted entity.
     */
    Parcel getByBarcode(User user, String barcode);

    /**
     * Get Parcel by User and barcode or name.
     *
     * @param user User entity.
     * @param text barcode or name of Parcel.
     * @return the persisted entity.
     */
    Parcel getByBarcodeOrName(User user, String text);

    /**
     * Get Parcel by User and barcode or name.
     *
     * @param user User entity.
     * @param barcode barcode of Parcel.
     * @param name name of Parcel.
     * @return the persisted entity.
     */
    Parcel getByBarcodeOrName(User user, String barcode, String name);

    /**
     * Get Parcel by User, TrackCode and name.
     *
     * @param user User entity.
     * @param trackCode TrackCode entity.
     * @param parcelName name of Parcel.
     * @return the persisted entity.
     */
    Parcel get(User user, TrackCode trackCode, String parcelName);

    /**
     * Get list of Parcel by User
     *
     * @param user User entity
     * @return the persisted entities.
     */
    List<Parcel> get(User user);

    /**
     * Get list of all Parcel
     *
     * @return the persisted entities.
     */
    List<Parcel> getAll();

    /**
     * Get list of all Parcel by TrackCode
     *
     * @param trackCode TrackCode entity
     * @return the persisted entities.
     */
    List<Parcel> getAll(TrackCode trackCode);

    /**
     * Remove Parcel
     *
     * @param parcel removing entity.
     */
    void remove(Parcel parcel);
}
