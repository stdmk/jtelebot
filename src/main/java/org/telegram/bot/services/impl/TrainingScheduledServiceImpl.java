package org.telegram.bot.services.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.domain.entities.Training;
import org.telegram.bot.domain.entities.TrainingScheduled;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.TrainingScheduledRepository;
import org.telegram.bot.services.TrainingScheduledService;

import java.time.DayOfWeek;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingScheduledServiceImpl implements TrainingScheduledService {

    private final TrainingScheduledRepository trainingScheduledRepository;

    @Override
    public TrainingScheduled get(User user, DayOfWeek dayOfWeek, Training training) {
        return trainingScheduledRepository.findByUserAndDayOfWeekAndTrainingOrderByTrainingTimeStart(user, dayOfWeek, training);
    }

    @Override
    public List<TrainingScheduled> get(User user, DayOfWeek dayOfWeek) {
        return trainingScheduledRepository.findByUserAndDayOfWeekOrderByTrainingTimeStart(user, dayOfWeek);
    }

    @Override
    public List<TrainingScheduled> get(User user) {
        return trainingScheduledRepository.findByUserOrderByTrainingTimeStart(user);
    }

    @Override
    public List<TrainingScheduled> getAll(DayOfWeek dayOfWeek) {
        return trainingScheduledRepository.findAllByDayOfWeekOrderByTrainingTimeStart(dayOfWeek);
    }

    @Override
    public void save(TrainingScheduled trainingScheduled) {
        trainingScheduledRepository.save(trainingScheduled);
    }

    @Override
    public void remove(TrainingScheduled trainingScheduled) {
        trainingScheduledRepository.delete(trainingScheduled);
    }

    @Override
    @Transactional
    public void removeAllByTraining(Training training) {
        trainingScheduledRepository.deleteByTraining(training);
    }
}
