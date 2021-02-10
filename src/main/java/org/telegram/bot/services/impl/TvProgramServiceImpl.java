package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.TvChannel;
import org.telegram.bot.domain.entities.TvProgram;
import org.telegram.bot.repositories.TvProgramRepository;
import org.telegram.bot.services.TvProgramService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class TvProgramServiceImpl implements TvProgramService {

    private final Logger log = LoggerFactory.getLogger(TvProgramServiceImpl.class);

    private final TvProgramRepository tvProgramRepository;

    @Override
    public TvProgram get(Integer tvProgramId) {
        log.debug("Request to get TvProgram by Id: {}", tvProgramId);
        return tvProgramRepository.findById(tvProgramId).orElse(null);
    }

    @Override
    public List<TvProgram> get(String tvProgramTitle, LocalDateTime dateTime) {
        log.debug("Request to get TvPrograms by it title: {}", tvProgramTitle);
        return tvProgramRepository.findByStartBetweenAndTitleContainsIgnoreCase(dateTime, dateTime.plusHours(6), tvProgramTitle);
    }

    @Override
    public List<TvProgram> get(TvChannel tvChannel, LocalDateTime dateTime) {
        log.debug("Request to get TvPrograms by channel: {}", tvChannel);
        return tvProgramRepository.findByChannelAndStopBetween(tvChannel, dateTime, dateTime.plusHours(6));
    }

    @Override
    public TvProgram save(TvProgram tvProgram) {
        log.debug("Request to save TvProgram: {}", tvProgram);
        return tvProgramRepository.save(tvProgram);
    }

    @Override
    public List<TvProgram> save(List<TvProgram> tvProgramList) {
        log.debug("Request to save TvProgramList: {}", tvProgramList);
        return tvProgramRepository.saveAll(tvProgramList);
    }

    @Override
    public void clearTable() {
        log.debug("Request to delete all TvPrograms: ");
        tvProgramRepository.deleteAll();
    }
}
