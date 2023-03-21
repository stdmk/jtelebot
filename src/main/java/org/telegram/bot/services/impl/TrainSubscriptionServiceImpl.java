package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.TrainSubscription;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.TrainSubscriptionRepository;
import org.telegram.bot.services.TrainSubscriptionService;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainSubscriptionServiceImpl implements TrainSubscriptionService {

    private final TrainSubscriptionRepository trainSubscriptionRepository;

    @Override
    public TrainSubscription get(Long trainSubscriptionId, User user) {
        return trainSubscriptionRepository.findByIdAndUser(trainSubscriptionId, user);
    }

    @Override
    public List<TrainSubscription> getActive(User user) {
        return trainSubscriptionRepository.findByUserAndActive(user, true);
    }

    @Override
    public TrainSubscription getFirstActive(User user) {
        return trainSubscriptionRepository.findByUserAndActive(user, true)
                .stream()
                .min(Comparator.comparing(TrainSubscription::getStartDate))
                .orElse(null);
    }

    @Override
    public void save(TrainSubscription trainSubscription) {
        trainSubscriptionRepository.save(trainSubscription);
    }

}
