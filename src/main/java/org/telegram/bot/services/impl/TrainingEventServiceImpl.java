package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.TrainingEvent;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.TrainingEventRepository;
import org.telegram.bot.services.TrainingEventService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingEventServiceImpl implements TrainingEventService {

    private final TrainingEventRepository trainingEventRepository;

    @Override
    public TrainingEvent get(User user, Long eventId) {
        return trainingEventRepository.findByUserAndId(user, eventId);
    }

    @Override
    public List<TrainingEvent> getAll(LocalDate date) {
        return trainingEventRepository.findByDateTimeBetween(
                date.atStartOfDay(),
                date.atTime(LocalTime.MAX));
    }

    @Override
    public TrainingEvent save(TrainingEvent trainingEvent) {
        return trainingEventRepository.save(trainingEvent);
    }
}
