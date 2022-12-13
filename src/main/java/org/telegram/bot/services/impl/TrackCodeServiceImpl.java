package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.domain.entities.TrackCode;
import org.telegram.bot.domain.entities.TrackCodeEvent;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.repositories.TrackCodeEventRepository;
import org.telegram.bot.repositories.TrackCodeRepository;
import org.telegram.bot.services.PostTrackingService;
import org.telegram.bot.services.TrackCodeService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackCodeServiceImpl implements TrackCodeService {

    private final PostTrackingService postTrackingService;
    private final TrackCodeRepository trackCodeRepository;
    private final TrackCodeEventRepository trackCodeEventRepository;

    @Override
    public TrackCode get(Long id) {
        log.debug("Request to get TrackCode by id {}", id);
        return trackCodeRepository.findById(id).orElse(null);
    }

    @Override
    public TrackCode get(String barcode) {
        log.debug("Request to get TrackCode by barcode {}", barcode);
        return trackCodeRepository.findByBarcode(barcode);
    }

    @Override
    public List<TrackCode> getAll() {
        log.debug("Request to get all TrackCode entities");
        return trackCodeRepository.findAll();
    }

    @Override
    public TrackCode save(TrackCode trackCode) {
        log.debug("Request to save TrackCode {}", trackCode);
        return trackCodeRepository.save(trackCode);
    }

    @Override
    public void remove(TrackCode trackCode) {
        log.debug("Request to remove TrackCode {}", trackCode);
        trackCodeRepository.delete(trackCode);
    }

    @Override
    public long getTrackCodesCount() {
        log.debug("Request to get current TrackCode entities count");
        return trackCodeRepository.count();
    }

    @Override
    @Transactional
    public boolean updateFromApi() {
        log.debug("Request to update track events data");

        List<TrackCode> trackCodeList = trackCodeRepository.findAll();
        Set<TrackCodeEvent> trackCodeEventSet = new HashSet<>();
        try {
            trackCodeList.forEach(trackCode -> {
                List<TrackCodeEvent> trackCodeEventList = postTrackingService.getData(trackCode.getBarcode());

                trackCodeEventRepository.deleteAllByTrackCode(trackCode);
                trackCodeEventList.forEach(event -> event.setTrackCode(trackCode));
                trackCode.setEvents(new HashSet<>(trackCodeEventList));

                trackCodeEventSet.addAll(trackCodeEventList);
            });
        } catch (BotException e) {
            log.error("Failed to update track events data: ", e);
            return false;
        }

        trackCodeEventRepository.saveAll(trackCodeEventSet);
        trackCodeRepository.saveAll(trackCodeList);

        return true;
    }

    @Override
    public void updateFromApi(TrackCode trackCode) throws BotException {
        List<TrackCodeEvent> trackCodeEventList = postTrackingService.getData(trackCode.getBarcode());

        trackCodeEventRepository.deleteAllByTrackCode(trackCode);
        trackCodeEventList.forEach(event -> event.setTrackCode(trackCode));
        trackCode.setEvents(new HashSet<>(trackCodeEventList));

        trackCodeEventRepository.saveAll(trackCodeEventList);
        trackCodeRepository.save(trackCode);
    }
}
