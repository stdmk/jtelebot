package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Training;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.TrainingRepository;
import org.telegram.bot.services.TrainingService;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingServiceImpl implements TrainingService {

    private final TrainingRepository trainingRepository;

    @Override
    public List<Training> get(User user) {
        return trainingRepository.findByUserAndDeletedOrderByTimeStart(user, false);
    }

    @Override
    public Training get(User user, Long trainingId) {
        return trainingRepository.findByUserAndIdAndDeletedOrderByTimeStart(user, trainingId, false);
    }

    @Override
    public Training get(User user, LocalTime time, String name) {
        return trainingRepository.findByUserAndTimeStartAndNameIgnoreCaseAndDeletedOrderByTimeStart(user, time, name, false);
    }

    @Override
    public void save(Training training) {
        trainingRepository.save(training.setDeleted(false));
    }

    @Override
    public void remove(Training training) {
        trainingRepository.save(training.setDeleted(true));
    }
}
