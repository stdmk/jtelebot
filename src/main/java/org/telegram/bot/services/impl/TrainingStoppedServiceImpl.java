package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.domain.entities.TrainingStopped;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.TrainingStoppedRepository;
import org.telegram.bot.services.TrainingStoppedService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingStoppedServiceImpl implements TrainingStoppedService {

    private final TrainingStoppedRepository trainingStoppedRepository;

    @Override
    public boolean isStopped(User user) {
        return trainingStoppedRepository.existsByUser(user);
    }

    @Override
    public void stop(User user) {
        trainingStoppedRepository.save(new TrainingStopped().setUser(user).setStopped(true));
    }

    @Override
    @Transactional
    public void start(User user) {
        trainingStoppedRepository.deleteByUser(user);
    }

    @Override
    public List<TrainingStopped> getAll() {
        return trainingStoppedRepository.findAll();
    }
}
