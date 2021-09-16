package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.TvChannel;
import org.telegram.bot.domain.entities.TvProgram;
import org.telegram.bot.repositories.TvProgramRepository;
import org.telegram.bot.services.TvProgramService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TvProgramServiceImpl implements TvProgramService {

    private final TvProgramRepository tvProgramRepository;

    @Override
    public TvProgram get(Integer tvProgramId) {
        log.debug("Request to get TvProgram by Id: {}", tvProgramId);
        return tvProgramRepository.findById(tvProgramId).orElse(null);
    }

    @Override
    public List<TvProgram> get(String tvProgramTitle, LocalDateTime dateTime, int hours) {
        log.debug("Request to get TvPrograms by it title: {}", tvProgramTitle);
        return tvProgramRepository.findByStopBetweenAndTitleContainsIgnoreCase(dateTime, dateTime.plusHours(hours), tvProgramTitle);
    }

    @Override
    public List<TvProgram> get(TvChannel tvChannel, LocalDateTime dateTime, int hours) {
        log.debug("Request to get TvPrograms by channel: {}", tvChannel);
        return tvProgramRepository.findByChannelAndStopBetween(tvChannel, dateTime, dateTime.plusHours(hours));
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
