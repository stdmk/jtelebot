package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.domain.entities.Parcel;
import org.telegram.bot.domain.entities.TrackCode;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.ParcelRepository;
import org.telegram.bot.repositories.TrackCodeEventRepository;
import org.telegram.bot.repositories.TrackCodeRepository;
import org.telegram.bot.services.ParcelService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParcelServiceImpl implements ParcelService {

    private final ParcelRepository parcelRepository;
    private final TrackCodeRepository trackCodeRepository;
    private final TrackCodeEventRepository trackCodeEventRepository;
    
    @Override
    public void save(Parcel parcel) {
        log.debug("Request to save Parcel: {}", parcel);
        parcelRepository.save(parcel);
    }

    @Override
    public Parcel get(Long parcelId) {
        log.debug("Request to get Parcel by id {}", parcelId);
        return parcelRepository.findById(parcelId).orElse(null);
    }

    @Override
    public Parcel getByName(User user, String parcelName) {
        log.debug("Request to get Parcel by User {} and name {}", user, parcelName);
        return parcelRepository.findByUserAndNameIgnoreCase(user, parcelName);
    }

    @Override
    public Parcel getByBarcode(User user, String barcode) {
        log.debug("Request to get Parcel by barcode {}", barcode);
        return parcelRepository.findByUserAndTrackCodeBarcodeIgnoreCase(user, barcode);
    }

    @Override
    public Parcel getByBarcodeOrName(User user, String text) {
        log.debug("Request to get Parcel by barcode or name {}", text);
        return parcelRepository.findByUserAndTrackCodeBarcodeIgnoreCaseOrNameIgnoreCase(user, text, text);
    }

    @Override
    public Parcel get(User user, TrackCode trackCode, String parcelName) {
        log.debug("Request to get Parcel by User {}, TrackCode {} and name {}", user, trackCode, parcelName);
        return parcelRepository.findByUserAndTrackCodeAndNameIgnoreCase(user, trackCode, parcelName);
    }

    @Override
    public List<Parcel> get(User user) {
        log.debug("Request to get Parcel list of User {}", user);
        return parcelRepository.findAllByUser(user);
    }

    @Override
    public List<Parcel> getAll() {
        log.debug("Request to get all Parcel entities");
        return parcelRepository.findAll();
    }

    @Override
    public List<Parcel> getAll(TrackCode trackCode) {
        log.debug("Request to get all Parcels by TrackCode {}", trackCode);
        return parcelRepository.findAllByTrackCode(trackCode);
    }

    @Override
    @Transactional
    public void remove(Parcel parcel) {
        log.debug("Request to remove Parcel {}", parcel);
        parcelRepository.delete(parcel);

        TrackCode trackCode = parcel.getTrackCode();
        List<org.telegram.bot.domain.entities.Parcel> parcelWithTheTrackCodeList = getAll(trackCode);
        if (parcelWithTheTrackCodeList.isEmpty()) {
            trackCodeEventRepository.deleteAllByTrackCode(trackCode);
            trackCodeRepository.delete(trackCode);
        }
    }
}
