package org.telegram.bot.services;

import org.telegram.bot.domain.entities.TrainSubscription;
import org.telegram.bot.domain.entities.User;

import java.util.List;

public interface TrainSubscriptionService {
    TrainSubscription get(Long trainSubscriptionId, User user);
    List<TrainSubscription> getActive(User user);
    TrainSubscription getFirstActive(User user);
    void save(TrainSubscription trainSubscription);

}
